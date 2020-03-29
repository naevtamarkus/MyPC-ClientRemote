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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static mypc.clientremote.ClientRemote.debug;

class NetworkScanner {
    // https://stackoverflow.com/questions/11547082/fastest-way-to-scan-ports-with-java

    //private boolean onlyPing; // Wether it's enough to do ping or we need sockets and ports
    private boolean stopAfterFound; // whether to stop scan after first found 
    //private boolean cancel; // Give the possibility that someone cancels the search
    private List<Integer> portsToScan; // List wit the ports to scan. Can be empty
    private List<String> ipsToScan; // List with IPs to scan. 
    private int timeout; // Connection/ping timeout, in miliseconds
    private List<Future<ScanResult>> results; // Pairs IP-port for all entries (open and close)
    private int threadPoolSize;
    //private ExecutorService executor;
    private ThreadPoolExecutor executor;
    private BlockingQueue<Runnable> jobQueue;

    // These callbacks are supposed to run on the caller's thread
    private onFinishCallback onFinishCallback;
    private onFoundCallback onFoundCallback;

    public interface onFoundCallback {
        public void onFound(String ip, int port);
    }
    public interface onFinishCallback {
        public void onFinish();
    }

    public NetworkScanner() {
        // Init with defaults
        //onlyPing = true; // replaced by portsToScan.size() = 0;
        stopAfterFound = false;
        //cancel = false;
        timeout = 200;
        threadPoolSize = 20;
        portsToScan = new ArrayList<>();
        ipsToScan = new ArrayList<>();
        results = new ArrayList<>();
    }

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
                ipsToScan.add(candidate);
            }
        } catch (Exception e) {

        }
    }

    public void addAllTypeCNetworks() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            //for (int networkInterfaceNumber = 0; en.hasMoreElements(); networkInterfaceNumber++) {
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
        portsToScan.add(port);
    }

    public void addPortRange(int first, int last) {
        if (last < first) return;
        for (int i = first; i<=last; i++) {
            portsToScan.add(i);
        }
    }

    public void setStopAfterFound(boolean val) {
        stopAfterFound = val;
    }

    public void setTimeout(int millis) {
        timeout = millis;
    }

    public void setThreadPoolSize(int size) {
        threadPoolSize = size;
    }

    // Returns the size of the search
    public int start() {
        results = new ArrayList<>(); // Reset results, in case they call 'start()' twice
        //executor = Executors.newFixedThreadPool(threadPoolSize);
        jobQueue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS, jobQueue);
        for (String ip: ipsToScan) {
            // Check if there are ports to scan
            if (portsToScan.size() > 0) {
                for (int port: portsToScan) {
                    results.add(portIsOpen(ip, port, timeout));
                }
            } else {
                // Otherwise just ping
                results.add(hostIsUp(ip, timeout));
            }
        }
        // After all jobs are submitted, we wait for them to finish to call the callback
        executor.shutdown(); // Don't accept more tasks, but finish what got in
        if (onFinishCallback != null) {
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        executor.awaitTermination(500L, TimeUnit.HOURS);
                    } catch (Exception e) {
                        // Nothing
                    }
                    // TODO watch out which thread we need to run on
                    onFinishCallback.onFinish();
                }
            }).run();
        }
        return results.size();
    }

    public void cancel() {
        // Gracefully stop jobs from flowing
        if (jobQueue != null) jobQueue.clear();
    }

    public void cancelForced() {
        if (executor != null) executor.shutdownNow();
    }

    public long countCompleted() {
        if (executor != null) {
            return executor.getCompletedTaskCount();            
        } else {
            return 0;
        }
    }

    public Future<ScanResult> portIsOpen(final String ip, final int port, final int timeout) {
        return executor.submit(new Callable<ScanResult>() {
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

    public Future<ScanResult> hostIsUp(final String ip, final int timeout) {
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

    public void waitForCompletion() {
        try {
            executor.awaitTermination(500L, TimeUnit.HOURS);
        } catch (Exception e){
            // nothing
        }
    }

    public List<ScanResult> getAllResults() {
        List<ScanResult> output = new ArrayList<>();
        for (Future<ScanResult> item : results) {
            try {
                output.add(item.get()); // This can throw a couple of exceptions
            } catch (Exception e) {
                // If there was an error, just dump the result
            }
        }
        return output;
    }

    public List<ScanResult> getHitResults() {
        List<ScanResult> output = new ArrayList<>();
        ScanResult tmp;
        for (Future<ScanResult> item : results) {
            try {
                tmp = item.get();
                if (tmp.isHit()) output.add(tmp); // This can throw a couple of exceptions
            } catch (Exception e) {
                // If there was an error, just dump the result
                debug ("problem");
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