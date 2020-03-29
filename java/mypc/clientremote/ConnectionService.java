//package src;
package mypc.clientremote;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mypc.clientremote.ClientRemote.debug;

public class ConnectionService {
    private static ConnectionService instance;
    private Robot robot;
    private Config config;
    private boolean disconnectNow = false; // TODO maybe we don't need this? Or maybe in Config class?
    ConnectionChangeEvent connectionChangeEvent;
    private Status status = Status.READY;
    Thread connectionThread;
    private static final int PROTOCOL_VERSION = 1;

    public enum Status { READY, CONNECTING, RECONNECTING, CONNECTED, ERROR };

    // PUBLIC METHODS

    public static ConnectionService getInstance() { 
        if (instance == null) instance = new ConnectionService(); 
        return instance; 
    } 

    public interface ConnectionChangeEvent {
        public void onConnectionChanged();
    }

    public void setConnectionChangeEvent(ConnectionChangeEvent conev) {
        connectionChangeEvent = conev;
    }

    public Status getStatus() { return status; }

    private ConnectionService() {
        status = Status.READY;
        try {
            robot = new Robot();
            config = Config.getInstance();
        }
        catch (Exception e) {
            debug("Couldn't Create robot");
            status = Status.ERROR;
            // TODO handle this properly (e.g. unrecoverable error?)
        }
    }

    // Beware you should run this inside of a thread
    public boolean testHost(String ip, int timeout) {
        Socket s = new Socket();
        try {
            s.setSoTimeout(timeout);
            s.connect(new InetSocketAddress(ip, config.getPort()), timeout);
            // TODO maybe we should check if we get the right welcome message
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void connect() {
        // We assume we enter from READY or ERROR states most times... anyway, treat as connecting for the first time (no alerts)
        status = Status.CONNECTING;
        if (connectionChangeEvent != null) connectionChangeEvent.onConnectionChanged();
        // Kill previous threads
        if (connectionThread != null) connectionThread.interrupt();
        
        // Start thread 
        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (! disconnectNow && ! Thread.currentThread().isInterrupted()) {
                    // Loop again and again to connect
                    try {
                        debug("ConnectionService: client connecting to server");
                        Socket s = new Socket(config.getIpAddress(), config.getPort());
                        //outgoing stream redirect to socket
                        //OutputStream out = s.getOutputStream();
                        //PrintWriter output = new PrintWriter(out);
                        //output.println("Hello Android!");
                        status = Status.CONNECTED;
                        if (connectionChangeEvent != null) connectionChangeEvent.onConnectionChanged();
                        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                            
                        debug("ConnectionService: client connected and waiting for messages");
                        while (! disconnectNow && ! Thread.currentThread().isInterrupted()) {
                            //read line(s)
                            String st = input.readLine();
                            if (st == null) throw new IOException("received null value!"); // TODO we get this when disconnected, better handle in a better way?
                            debug("ConnectionService: client received msg from server: "+st);
                            // Hit the key in the local PC
                            String msgType = st.split(" ")[0];
                            String msgContent = st.split(" ",2)[1];
                            if (msgType.equals("S")) {
                                processSingleKey(msgContent);
                            } else if (msgType.equals("L")) {
                                processLongKey(msgContent);
                            } else if (msgType.equals("I")) {
                                processInfoMessage(msgContent);
                            }
                        }
                        //Close connection 
                        debug("ConnectionService: client disconnected");
                        s.close();
                        

                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        debug(Arrays.toString(e.getStackTrace()));

                    // Track the whole exception stack (first the lowest in the stack)
                    } catch (ConnectException e) {
                        // This is a problem with the remote server, probably Connection Refused. Just catch and continue
                        debug(Arrays.toString(e.getStackTrace()));
                    } catch (NoRouteToHostException e) {
                        // This is a problem with the remote server, probably Connection Refused. Just catch and continue
                        debug(Arrays.toString(e.getStackTrace()));
                    } catch (SocketException e) {
                        // This is a problem creating the socket itself, bad IP address and the like
                        debug(Arrays.toString(e.getStackTrace()));
                        // In this case, we can't recover from this error
                        status = Status.ERROR;
                        if (connectionChangeEvent != null) connectionChangeEvent.onConnectionChanged();
                        disconnectNow = true;
                        continue;
                    } catch (IOException e) {
                        // This is the most generic of the three, just catch and continue
                        debug(Arrays.toString(e.getStackTrace()));
                    }
                    // We can recover, just try again
                    if (connectionChangeEvent != null && status != Status.RECONNECTING) connectionChangeEvent.onConnectionChanged();
                    status = Status.RECONNECTING;
                    debug("ConnectionService: client disconnected. Trying to reconnect");

                    // Wait 5 seconds before connecting again
                    try {
                        if (! disconnectNow && ! Thread.currentThread().isInterrupted()) Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // nothing
                    }
                }
            }
        });
        connectionThread.start();
        // TODO handle more unrecoverable errors (and change connection status)
    }

    private void processSingleKey(String message) {
        int keyCodeAndroid = Integer.parseInt(message);
        switch(keyCodeAndroid){
            // Basic navigation keys
            case 19:  hitKey(KeyEvent.VK_UP); break;
            case 20:  hitKey(KeyEvent.VK_DOWN); break;
            case 21:  hitKey(KeyEvent.VK_LEFT); break;
            case 22:  hitKey(KeyEvent.VK_RIGHT); break;
            case 23:  hitKey(KeyEvent.VK_ENTER); break;  // Center key
            //case 4:   hitKey(KeyEvent.VK_BACK_SPACE); break;  // Back key
            case 4:   hitKey(KeyEvent.VK_ESCAPE); break;  // Back key

            // Extended navigation keys
            case 166: hitKey(KeyEvent.VK_TAB); break;  // program up
            case 167: hitKeys(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB); break;  // program down
            //case 111: hitKey(KeyEvent.VK_ESCAPE); break;  // exit
            case 111: hitKeys(KeyEvent.VK_ALT, KeyEvent.VK_F4); break;  // exit
            case 233: hitKey(KeyEvent.VK_CONTEXT_MENU); break;  // teletext
            case 165: hitKey(KeyEvent.VK_WINDOWS); break;  // teletext
            case 229: hitKeys(KeyEvent.VK_ALT, KeyEvent.VK_ESCAPE); break;  // back-forth key between prog/volume

            //case 62: hitKey(KeyEvent.VK_SPACE); break;
            //case 66: hitKey(KeyEvent.VK_ENTER); break;
            //case 67: hitkey(KeyEvent.VK_BACK_SPACE); break;

            // Quick launch icons (in order)
            case 8:  hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_1); break;  // 1
            case 9:  hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_2); break;  // 2
            case 10: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_3); break;  // 3
            case 11: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_4); break;  // 4
            case 12: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_5); break;  // 5
            case 13: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_6); break;  // 6
            case 14: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_7); break;  // 7
            case 15: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_8); break;  // 8
            case 16: hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_9); break;  // 9
            case 7:  hitKeys(KeyEvent.VK_WINDOWS, KeyEvent.VK_0); break;  // 0
            
            // Playback keys
            case 126: hitKey(KeyEvent.VK_SPACE); break;  // Play
            case 85:  hitKey(KeyEvent.VK_SPACE); break;  // Pause
            case 89:  hitKeys(KeyEvent.VK_CONTROL, KeyEvent.VK_LEFT); break;  // Rewind
            case 90:  hitKeys(KeyEvent.VK_CONTROL, KeyEvent.VK_RIGHT); break;  // Forewind
            case 88:  hitKey(KeyEvent.VK_N); break;  // Next
            case 87:  hitKey(KeyEvent.VK_P); break;  // Previous
            case 86:  hitKey(KeyEvent.VK_S); break;  // Stop
            case 175: hitKey(KeyEvent.VK_F); break;  // Full Screen
        }

    }

    private void processLongKey(String message) {
        int keyCodeAndroid = Integer.parseInt(message);
        switch(keyCodeAndroid){
            // Extended navigation keys
            //case 111: hitKeys(KeyEvent.VK_ALT, KeyEvent.VK_F4); break;  // exit
        } 
    }

    private void processInfoMessage(String message) {
        Matcher matcher = Pattern.compile( "proto_version=([0-9]+)" ).matcher( message );
        if (matcher.find()) {
            int version = Integer.parseInt(matcher.group(1));
            if (version > PROTOCOL_VERSION) {
                debug("Protocol version " + version+". Opening Update window");
                UpdateWindow.display();
            }
        } else {
            debug ("error: no version found");
        }
    }
    

    // https://github.com/justin-taylor/Remote-Desktop-Server/blob/revert_to_old_state/src/AutoBot.java
    // https://stackoverflow.com/questions/14572270/how-can-i-perfectly-simulate-keyevents/14615814
    private void hitKey(final int pcKey) {
        if (config.isDelayKeys()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // Nothing
                    }
                    robot.keyPress(pcKey);
                    robot.keyRelease(pcKey);
                }
            }).start();
        } else {
            robot.keyPress(pcKey);
            robot.keyRelease(pcKey);
        }
    }

    private void hitKeys(final int pcKey1, final int pcKey2) {
        if (config.isDelayKeys()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // Nothing
                    }
                    robot.keyPress(pcKey1);
                    robot.keyPress(pcKey2);
                    robot.keyRelease(pcKey2);
                    robot.keyRelease(pcKey1);
                }
            }).start();
        } else {
            robot.keyPress(pcKey1);
            robot.keyPress(pcKey2);
            robot.keyRelease(pcKey2);
            robot.keyRelease(pcKey1);
        }
    }


}

/* Other keys: 
// TOP small buttons
241 - Digital/analog
232 - TV/Radio
111 - Exit
NO  - Ext. box menu

// Numeric pad
[8-16] - [1-9] 
7   - 0
165 - i+
233 - Teletext 

// Teletext colors
183 - txt red
184 - txt green
185 - txt yellow
186 - txt blue

// Arrows and surrounding
[19-22] - up/down/left/right
23  - Center key
4   - Back key
NO  - volume up/down
229 - back-forth arrows between volume and programs
NO  - mute
166 - program up
167 - program down

222 - audio
89  - rewind
126 - play
90  - forewind

175 - options...
88  - -1 track
85  - pause
87  - +1 track

NO  - Help
130 - Rec
86  - Stop
NO  - Title list

*/