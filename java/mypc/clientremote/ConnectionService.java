package mypc.clientremote;

/*
    This is a Companion App that allows users to control their personal computer
    from their Android TV when they are using the MyPC App:
    https://play.google.com/store/apps/details?id=org.trecet.mypc
    Copyright (C) 2020    Pablo Fernandez    naevtamarkus@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import java.awt.Robot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mypc.clientremote.ClientRemote.debug;
import static mypc.clientremote.ClientRemote.debugException;

public class ConnectionService {
    private static ConnectionService instance;
    private Robot robot;
    private Config config;
    private boolean disconnectNow = false; // TODO maybe we don't need this? Or maybe in Config class?
    ConnectionChangeEvent connectionChangeEvent;
    MessageReceivedEvent messageReceivedEvent;
    private Status status = Status.READY;
    Thread connectionThread;
    private static final int PROTOCOL_VERSION = 1;
    private KeyMap keyMap; 

    public enum Status { READY, CONNECTING, RECONNECTING, CONNECTED, ERROR };

    // PUBLIC METHODS

    public static ConnectionService getInstance() { 
        if (instance == null) instance = new ConnectionService(); 
        return instance; 
    } 

    public interface ConnectionChangeEvent {
        public void onConnectionChanged();
    }

    public interface MessageReceivedEvent {
        public void onMessageReceived(String message);
    }

    public void setConnectionChangeEvent(ConnectionChangeEvent event) {
        connectionChangeEvent = event;
    }

    public void setMessageReceivedEvent(MessageReceivedEvent event) {
        messageReceivedEvent = event;
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
        }
        // Select keymap
        reloadKeymap();
    }

    public void reloadKeymap() {
        if (config.getKeyMap().equals("CUSTOM")) {
            keyMap = new KeyMapCustom();
            debug("Loaded Custom KeyMap with "+keyMap.size()+" entries");
        } else {
            keyMap = new KeyMapDefault();
            debug("Loaded Default KeyMap with "+keyMap.size()+" entries");
        }
    }
    /*
    // Beware you should run this inside of a thread
    public boolean testHost(String ip, int timeout) {
        Socket s = new Socket();
        try {
            s.setSoTimeout(timeout);
            s.connect(new InetSocketAddress(ip, config.getPort()), timeout);
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    */

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
                boolean retrying = false; // only used for logging purposes (to avoid too much noise)
                while (! disconnectNow && ! Thread.currentThread().isInterrupted()) {
                    // Loop again and again to connect
                    try {
                        if (!retrying) debug("ConnectionService: client connecting to server");
                        Socket s = new Socket(config.getIpAddress(), config.getPort());
                        //outgoing stream redirect to socket
                        //OutputStream out = s.getOutputStream();
                        //PrintWriter output = new PrintWriter(out);
                        //output.println("Hello Android!");
                        retrying = false; // we went through, we enable logging
                        status = Status.CONNECTED;
                        if (connectionChangeEvent != null) connectionChangeEvent.onConnectionChanged();
                        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                            
                        debug("ConnectionService: client connected and waiting for messages");
                        while (! disconnectNow && ! Thread.currentThread().isInterrupted()) {
                            //read line(s)
                            String st = input.readLine();
                            if (st == null) throw new IOException("received null value!"); // TODO we get this when disconnected, better handle in a better way?
                            debug("ConnectionService: client received msg from server: "+st);
                            // Send to listener, if any
                            if (messageReceivedEvent != null) {
                                debug ("  redirecting message to event listener");
                                messageReceivedEvent.onMessageReceived(st);
                                // Don't process message anymore
                                continue;
                            }
                            Message message = new Message(st);
                            if (message.isKey()) {
                                processKey(message.getKeyType(), message.getKeyCode());
                            } else if (message.isInfo()) {
                                processInfoMessage(message.getInfo());
                            } else {
                                debug ("  unknown message");
                            }
                        }
                        //Close connection 
                        debug("ConnectionService: client disconnected");
                        s.close();
                        

                    } catch (UnknownHostException e) {
                        if(!retrying) debugException(e);
                    // Track the whole exception stack (first the lowest in the stack)
                    } catch (ConnectException e) {
                        // This is a problem with the remote server, probably Connection Refused. Just catch and continue
                        if(!retrying) debugException(e);
                    } catch (NoRouteToHostException e) {
                        // This is a problem with the remote server, probably Connection Refused. Just catch and continue
                        if(!retrying) debugException(e);
                    } catch (SocketException e) {
                        // This is a problem creating the socket itself, bad IP address and the like
                        if(!retrying) debugException(e);
                        // In this case, we can't recover from this error
                        status = Status.ERROR;
                        if (connectionChangeEvent != null) connectionChangeEvent.onConnectionChanged();
                        disconnectNow = true;
                        continue;
                    } catch (IOException e) {
                        // This is the most generic of the three, just catch and continue
                        if(!retrying) debugException(e);
                    }
                    // We can recover, just try again
                    if (connectionChangeEvent != null && status != Status.RECONNECTING) {
                        // We only notify the first time
                        status = Status.RECONNECTING;
                        connectionChangeEvent.onConnectionChanged();
                    } else {
                        status = Status.RECONNECTING;
                    }
                    if (!retrying) debug("ConnectionService: client disconnected. Trying to reconnect");
                    retrying = true;

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

    private void processKey(KeyMap.KeyType type, int code) {
        KeyCombination keyCombo = keyMap.getKey(type, code);
        if (keyCombo == null) {
            debug("  unmapped key");
            return;
        }
        debug("  pressing key: "+code+" -> combo: "+keyCombo.toString());
        if (config.isDelayKeys()) {
            keyCombo.hitKeysDelayed(robot);
        } else {
            keyCombo.hitKeys(robot);
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
    
    

}

