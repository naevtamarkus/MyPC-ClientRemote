package mypc.clientremote;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static mypc.clientremote.ClientRemote.debug;

/*
    ##### USAGE EXAMPLE #####
    
    NetworkScanner scanner = new NetworkScanner();

    // Add hosts/networks to scan (as many as you want)
    scanner.addIpAddress("127.0.0.1"); // individual IP
    scanner.addAllTypeCNetworksAvailable(); // multiple IPs
    // Optionally add ports to scan (if no ports are added, it does ICMP Ping)
    scanner.addPort(50505); // individual port
    scanner.addPortRange(1, 200); // multiple ports
    // Optionally, tell the scanner to stop after the first success
    scanner.setStopAfterFound(true);    
        
    // Define callback when scan is finished (even if stopped/cancelled):
    scanner.setOnFinishCallback(new NetworkScanner.onFinishCallback(){
        @Override
        public void onFinish() {
            debug("Scan finished, " + scanner.getHitResults().size() + " found");                
        }
    });
    
    // Define callback when something is found:
    scanner.setOnFoundCallback(new NetworkScanner.onFoundCallback(){        
        @Override
        public void onFound(String ip, int port) {
            debug ("Found " + ip + ":" + port);
        }
    });

    // Start scan (asynchronously)
    scanner.start();

    // Check status
    if (scanner.isScanning()) debug ("is scanning!");
    // Cancel
    scanner.cancel();
    // Get scan results:
    List<ScanResult> results = getAllResults();
    // Check below for other public methods

*/

class NetworkScanner {
    // https://stackoverflow.com/questions/11547082/fastest-way-to-scan-ports-with-java

    private boolean stopAfterFound; // whether to stop scan after first found 
    private List<Integer> portsToScan; // List wit the ports to scan. Can be empty
    private List<String> ipsToScan; // List with IPs to scan. 
    private int timeout; // Connection/ping timeout, in miliseconds
    private List<Future<ScanResult>> results; // Pairs IP-port for all entries (open and close)
    private int threadPoolSize;
    private ThreadPoolExecutor executor;
    private BlockingQueue<Runnable> jobQueue;

    // These callbacks are supposed to run on the caller's thread
    private onFinishCallback onFinishCallback;
    private onFoundCallback onFoundCallback;

    // Callback interfaces
    public interface onFoundCallback {
        public void onFound(String ip, int port);
    }
    public interface onFinishCallback {
        public void onFinish();
    }

    // Constructor
    public NetworkScanner() {
        // Init with defaults
        //onlyPing = true; // replaced by portsToScan.size() = 0;
        this.stopAfterFound = false;
        //cancel = false;
        this.timeout = 200;
        this.threadPoolSize = 20;
        this.portsToScan = new ArrayList<>();
        this.ipsToScan = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    // #######################
    //      PUBLIC METHODS
    // #######################

    public void setOnFinishCallback(onFinishCallback cb) {
        this.onFinishCallback = cb;
    }

    public void setOnFoundCallback(onFoundCallback cb) {
        this.onFoundCallback = cb;
    }

    public void addIpAddress(String ip) {
        ipsToScan.add(ip);
    }

    // TODO implement!
    //Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
    // https://commons.apache.org/proper/commons-net/javadocs/api-3.6/org/apache/commons/net/util/SubnetUtils.html
    // https://docs.oracle.com/javase/7/docs/api/java/net/InterfaceAddress.html
    public void addFullNetworkFromInterface(InterfaceAddress iAddr) {
        // Adds all IPs visible from the parameter
        // First we try to guess the network params
        //InetAddress addr = InetAddress.getByName(ip);
        //SubnetInfo
        //int prefixLength = iAddr.getNetworkPrefixLength();
    }

    public void addFullNetworkFromIPTypeC(InetAddress ipAddr) {
        try {
            //InetAddress ipAddr= InetAddress.getByName(ip);
            byte[] bytes = ipAddr.getAddress();
            for (int i = 1; i <= 254; i++) {
                bytes[3] = (byte)i; 
                InetAddress address = InetAddress.getByAddress(bytes);
                String candidate = address.toString().substring(1);
                this.ipsToScan.add(candidate);
            }
        } catch (Exception e) {
            debug(e.getMessage());
        }
    }

    public void addAllTypeCNetworksAvailable() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            for (;en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                List<InterfaceAddress> enumIfaceAddr = intf.getInterfaceAddresses();
                for (InterfaceAddress ifaceAddr: enumIfaceAddr) {
                    if (ifaceAddr.getNetworkPrefixLength() == 24){
                        debug("Adding TypeC network: "+ ifaceAddr.getAddress() + " / " + ifaceAddr.getNetworkPrefixLength());
                        addFullNetworkFromIPTypeC(ifaceAddr.getAddress());
                    }
                }
            }
        } catch (Exception e) {
            debug(e.getMessage());
        }
    }

    public void addPort(int port) {
        this.portsToScan.add(port);
    }

    public void addPortRange(int first, int last) {
        if (last < first) return;
        for (int i = first; i<=last; i++) {
            this.portsToScan.add(i);
        }
    }

    public void setStopAfterFound(boolean val) {
        this.stopAfterFound = val;
    }

    public void setTimeout(int millis) {
        this.timeout = millis;
    }

    public void setThreadPoolSize(int size) {
        this.threadPoolSize = size;
    }

    // Returns the size of the search
    // This is a non-blocking call
    public int start() {
        this.results = new ArrayList<>(); // Reset results, in case they call 'start()' twice
        //executor = Executors.newFixedThreadPool(threadPoolSize);
        this.jobQueue = new LinkedBlockingQueue<Runnable>();
        this.executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, 
                                          TimeUnit.MILLISECONDS, jobQueue);
        for (String ip: this.ipsToScan) {
            // Check if there are ports to scan
            if (this.portsToScan.size() > 0) {
                for (int port: this.portsToScan) {
                    this.results.add(portIsOpen(ip, port, this.timeout));
                }
            } else {
                // Otherwise just ping
                this.results.add(hostIsUp(ip, this.timeout));
            }
        }
        // Don't accept more tasks, but finish what got in
        this.executor.shutdown(); 
        // Launch a background task to call the callback when it's finished
        if (onFinishCallback != null) {
            new Thread(new Runnable(){
                @Override
                public void run() {
                    waitForCompletion();
                    onFinishCallback.onFinish(); // we already checked != null
                    /*
                    try {
                        executor.awaitTermination(500L, TimeUnit.HOURS);
                    } catch (Exception e) {
                        // Nothing
                    }
                    onFinishCallback.onFinish();
                    */
                }
            }).start();
        }
        return this.results.size();
    }

    public void cancel() {
        // Gracefully stop jobs from flowing
        debug("Stopping scan. Pending jobs: " + this.executor.getActiveCount());
        if (this.jobQueue != null) this.jobQueue.clear();
    }

    public void cancelForced() {
        // TODO this was never properly tested
        if (this.executor != null) this.executor.shutdownNow();
    }

    public long countCompleted() {
        if (this.executor != null) {
            return this.executor.getCompletedTaskCount();            
        } else {
            return 0;
        }
    }

    public boolean isScanning() {
        return this.executor != null && this.executor.getActiveCount() > 0;
    }

    public void waitForCompletion() {
        int deciSeconds = 10*3600*24; // wait 24 hours max
        while (deciSeconds > 0 && this.isScanning()) {
            deciSeconds--;
            try { 
                Thread.sleep(100); 
            } catch (Exception e) {
                debug(e.getMessage());
            }
        }
    }

    // #######################
    //      PRIVATE METHODS
    // #######################

    private Future<ScanResult> portIsOpen(final String ip, final int port, final int timeout) {
        return this.executor.submit(new Callable<ScanResult>() {
            @Override
            public ScanResult call() {
                try {
                    debug("Scanning "+ ip + ":" +port);
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    // TODO watch out which thread we need to run on
                    if (onFoundCallback != null) onFoundCallback.onFound(ip, port);
                    if (stopAfterFound) cancel(); 
                    return new ScanResult(ip, port, true);
                } catch (Exception ex) {
                    return new ScanResult(ip, port, false);
                }
            }
        });
    }

    private Future<ScanResult> hostIsUp(final String ip, final int timeout) {
        return executor.submit(new Callable<ScanResult>() {
            @Override
            public ScanResult call() {
                try {
                    debug("Scanning "+ ip);
                    InetAddress address = InetAddress.getByName(ip); // no DNS query if IP format is correct
                    boolean found = address.isReachable(timeout);
                    // TODO watch out which thread we need to run on
                    if (found && onFoundCallback != null) onFoundCallback.onFound(ip, 0);
                    if (found && stopAfterFound) cancel(); // TODO Can we do this inside?
                    return new ScanResult(ip, 0, address.isReachable(timeout));
                } catch (Exception ex) {
                    return new ScanResult(ip, 0, false);
                }
            }
        });
    }

    // #######################
    //      RESULTS CLASS
    // #######################

    public List<ScanResult> getAllResults() {
        List<ScanResult> output = new ArrayList<>();
        for (Future<ScanResult> item : this.results) {
            try {
                if (! item.isDone()) continue; // Ignore if the future is not done, otherwise blocks call
                output.add(item.get()); // This can throw a couple of exceptions
            } catch (Exception e) {
                // If there was an error, just dump the result
                debug(e.getMessage());
            }
        }
        return output;
    }

    public List<ScanResult> getHitResults() {
        List<ScanResult> output = new ArrayList<>();
        ScanResult tmp;
        for (Future<ScanResult> item : this.results) {
            try {
                if (! item.isDone()) continue; // Ignore if the future is not done, otherwise blocks call
                tmp = item.get();
                if (tmp.isHit()) output.add(tmp); // This can throw a couple of exceptions
            } catch (Exception e) {
                // If there was an error, just dump the result
                debug(e.getMessage());
            }
        }
        return output;
    }

    public class ScanResult {
        private String ip;
        private int port;  // 0 means no port, just ping
        private boolean isHit;
    
        public ScanResult(String ip, int port, boolean isHit) {
            this.ip = ip;
            this.port = port; // 0 means no port, just ping
            this.isHit = isHit;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port; // 0 means no port, just ping
        }
    
        public boolean isHit() {
            return isHit;
        }
    }

    // This just shows local networks, helper function
    public static void debugNetworks() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            //for (int networkInterfaceNumber = 0; en.hasMoreElements(); networkInterfaceNumber++) {
            for (;en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                debug ("Found networkInterface: "+intf.getName());
                
                List<InterfaceAddress> enumIfaceAddr = intf.getInterfaceAddresses();
                for (InterfaceAddress ifaceAddr: enumIfaceAddr) {
                    debug("  address: "+ ifaceAddr.getAddress() + " / " + ifaceAddr.getNetworkPrefixLength());
                }
            }
        } catch (Exception e) {
            debug(e.getMessage());
        }
    }
}