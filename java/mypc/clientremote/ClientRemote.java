package mypc.clientremote;

import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.Separator;
import dorkbox.util.Desktop;
import mypc.clientremote.ConnectionService.Status;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/* TODO LIST
 * Improve installer: https://stackoverflow.com/questions/1276091/installer-generator-written-in-java
 *   or even get rid of it and use only .appx methods
 * Improve look & feel to resemble a modern application (check UIManager.setLookAndFeel())
 * Reduce verbosity when TV cannot be reached
 * Cache the IP address in the config object (e.g. can also apply to other config items)
 * Handle ConnectionService ERROR state properly (e.g. unrecoverable error?)
 * consider checking if we get the right welcome message in the testHost() in ConnectionService
 * Improve error-handling of the ConnectionService
 * Replace " + " by something that can't be repeated in KeyCombination (e.g. ++)
 * Improve NetworkScanner to allow networks other-than-TypeC and to do a broadcast to add hosts that respond
 *
 */
// TODO

public class ClientRemote {
    private static ClientRemote instance;
    private final URL MYPC_ICON_TRAY_NORMAL = getClass().getResource("/MyPC-icon_128x128_normal.png");
    private final URL MYPC_ICON_TRAY_RED = getClass().getResource("/MyPC-icon_128x128_red.png");
    private final URL MYPC_ICON_TRAY_GREEN = getClass().getResource("/MyPC-icon_128x128_green.png");
    private static boolean exit_app = false;
    private Config config;
    private ConnectionService connectionService;
    private SystemTray systemTray;

    // Project-wise constants:
    public static String URL_WEBSITE = "https://mypc-app.web.app/";
    public static String EMAIL = "naevtamarkus@gmail.com";
    public static String URL_EMAIL = "mailto:"+EMAIL;

    public static void debug(String text) {
        SimpleDateFormat logTime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Date date = new Date();
        String line = logTime.format(date) + " - " + text + "\n";
        Path path = Paths.get("debug.txt");
        try {
            Files.write(path, line.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);  //Append mode
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        System.out.print(line);
    }

    public static void main(String[] args) {
        ClientRemote clientRemote = getInstance();
        // Keep the main loop active until the end
        debug("Client started, waiting to die...");
        NetworkScanner.debugNetworks();

        while (! exit_app) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // nothing
            }
        }
        debug("Bye!");
        // System.exit(failure);
    }

    public static ClientRemote getInstance() { 
        if (instance == null) instance = new ClientRemote(); 
        return instance; 
    } 

    private ClientRemote() {
        config = Config.getInstance();
        config.setDebug(true); // TODO remove this in production

        // Show the tray icon
        displayTrayIcon();

        connectionService = ConnectionService.getInstance();
        // Connect only if an IP address is configured
        if (config.isSetIpAddress() && config.isConnectOnStart()) connectionService.connect();
        // Change status on connection changes
        connectionService.setConnectionChangeEvent(new ConnectionService.ConnectionChangeEvent() {
            @Override
            public void onConnectionChanged() {
                Status status = connectionService.getStatus();
                systemTray = SystemTray.get();
                debug("Connection changed: " + status.toString());
                switch (status) {
                    case READY: systemTray.setStatus("Ready"); break;
                    case CONNECTING: 
                        systemTray.setStatus("Connecting..."); 
                        systemTray.setImage(MYPC_ICON_TRAY_RED);
                        break;
                    case CONNECTED: 
                        systemTray.setStatus("Connected"); 
                        systemTray.setImage(MYPC_ICON_TRAY_GREEN);
                        break;
                    case RECONNECTING: 
                        systemTray.setStatus("Reconnecting..."); 
                        systemTray.setImage(MYPC_ICON_TRAY_RED);
                        break;
                    case ERROR: 
                        systemTray.setStatus("ERROR"); 
                        systemTray.setImage(MYPC_ICON_TRAY_RED);
                        break;
                }
                ConfigWindow.setStatus(status);
            }
        }); 
    }

    private void displayTrayIcon() {
        // See https://github.com/dorkbox/SystemTray/blob/384cd97622cd70c93ba5b2ae709365028de1a0b4/test/dorkbox/TestTray.java for documentation
        SystemTray.DEBUG = true;
        //SystemTray.APP_NAME = "MyPC";
        systemTray = SystemTray.get();
        if (systemTray == null) {
            throw new RuntimeException("Unable to load SystemTray!");
        }

        systemTray.setImage(MYPC_ICON_TRAY_NORMAL);
        systemTray.setTooltip("MyPC Remote Client");
        systemTray.setStatus("READY");

        // Build the menu
        Menu mainMenu = systemTray.getMenu();

        mainMenu.add(new MenuItem("Config", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                //displayConfigWindow();
                debug("Opening Config window");
                ConfigWindow.display();
            }
        }));

        mainMenu.add(new MenuItem("Key Mapping", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                //displayConfigWindow();
                debug("Opening KeyMap window");
                KeyMapWindow.display();
            }
        }));

        /*
        // Some elements only go in when in debug mode
        if (config.isDebug()) {
            Checkbox delayKeysCheckbox = new Checkbox("Delay Keys", new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    boolean checked = ((Checkbox) e.getSource()).getChecked();
                    //System.err.println("Am i checked? " + checked);
                    config.setDelayKeys(checked);
                }
            });
            //delayKeysCheckbox.setShortcut('k');
            delayKeysCheckbox.setChecked(false);
            mainMenu.add(delayKeysCheckbox);
        }
        */
        mainMenu.add(new Separator());

        // Send the user to a help page
        mainMenu.add(new MenuItem("About", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    Desktop.browseURL(URL_WEBSITE);
                } catch (IOException e1) {
                    debug(Arrays.toString(e1.getStackTrace()));
                }
            }
        }));

        mainMenu.add(new MenuItem("Quit", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                exit_app = true;
                debug("User quitted app");
                systemTray.shutdown();
                System.exit(0);  // not necessary if all non-daemon threads have stopped.
            }
        })); // case does not matter
    }
}