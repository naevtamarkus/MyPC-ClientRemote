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

import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.Separator;
import dorkbox.util.Desktop;
import mypc.clientremote.ConnectionService.Status;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

/* TODO LIST:
 * Improve installer: https://stackoverflow.com/questions/1276091/installer-generator-written-in-java
 *   or even get rid of it and use only .appx methods
 * Improve look & feel to resemble a modern application (check UIManager.setLookAndFeel())
 * Cache the IP address in the Config object (e.g. can also apply to other config items)
 * Handle ConnectionService ERROR state properly (e.g. unrecoverable error?)
 * Improve error-handling of the ConnectionService
 * Improve NetworkScanner to allow networks other-than-TypeC and to do a broadcast to add hosts that respond
 * Review build.gradle to make it cleaner. When I hit Run in an IDE, gradle task keeps on going :(
 */

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
    public static Path DEBUGFILEPATH = Config.getDataPath().resolve("debug.txt");

    public static void debug(String text) {
        SimpleDateFormat logTime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        Date date = new Date();
        String line = logTime.format(date) + " - " + text + "\n";
        try {
            Files.write(DEBUGFILEPATH, line.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);  //Append mode
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

        // Set up an auto-destroy mechanism (to prevent multiple instances running)
        int random = new Random().nextInt(1000000);
        Path checkFile = Config.getDataPath().resolve("instance.txt");
        try {
            Files.write(checkFile, String.valueOf(random).getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);  //truncate old stuff
        } catch (IOException e) {
            debug("Could not create pid.txt file: "+e.getMessage());
            return;
        }

        // Infinite loop to avoid leaving main()
        while (! exit_app) {
            try {
                Thread.sleep(3000);
                if (Integer.parseInt(Files.readAllLines(checkFile).get(0)) != random) {
                    System.exit(0);
                    debug("Exitting to prevent duplicate instances");
                    return;
                }
            } catch (InterruptedException e) {
                // nothing
            } catch (IOException e){
                debug("Could not read pid.txt file: "+e.getMessage());
                return;
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
        // Prep data directory
        try {
            if (!Files.exists(Config.getDataPath())) Files.createDirectory(Config.getDataPath());
        } catch (IOException e) {
            // nothing TODO fix this
        }
        // Truncate debug file to 1 MB
        try (FileChannel outChan = new FileOutputStream(DEBUGFILEPATH.toFile(), true).getChannel()) {
            outChan.truncate(1000000);
        } catch (Exception e) {
            // Nothing
        }
        config.setDebug(true);

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