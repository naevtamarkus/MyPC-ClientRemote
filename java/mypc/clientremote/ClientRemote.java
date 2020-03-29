package mypc.clientremote;

// FOR System Tray Icon:
//import java.awt.AWTException;
//import java.awt.Image;
//import java.awt.MenuItem;
//import java.awt.PopupMenu;
import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Separator;
import dorkbox.util.Desktop;
//import dorkbox.util.OS;
//import java.util.Random;
import mypc.clientremote.ConnectionService.Status;

//import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
/*
import java.awt.Frame;
import java.awt.TextField;
import java.awt.Label;
import java.awt.Button;
import java.awt.FlowLayout;
*/
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

// TODO https://stackoverflow.com/questions/1276091/installer-generator-written-in-java

public class ClientRemote {
    private static ClientRemote instance;
    private final URL MYPC_ICON_NORMAL = getClass().getResource("/MyPC-icon_512x512.png");
    private static boolean exit_app = false;
    private Config config;
    private ConnectionService connectionService;
    private SystemTray systemTray;

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
        if (config.isSetIpAddress())
            connectionService.connect();
        // Change status on connection changes
        connectionService.setConnectionChangeEvent(new ConnectionService.ConnectionChangeEvent() {
            @Override
            public void onConnectionChanged() {
                Status status = connectionService.getStatus();
                debug("Connection changed: " + status.toString());
                switch (status) {
                    case READY: systemTray.setStatus("Ready"); break;
                    case CONNECTING: systemTray.setStatus("Connecting..."); break;
                    case CONNECTED: systemTray.setStatus("Connected"); break;
                    case RECONNECTING: systemTray.setStatus("Reconnecting..."); break;
                    case ERROR: systemTray.setStatus("ERROR"); break;
                    // TODO change icon also, not just the status
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

        systemTray.setImage(MYPC_ICON_NORMAL);
        systemTray.setTooltip("ToolTip");
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
        
        mainMenu.add(new Separator());

        // Send the user to a help page
        mainMenu.add(new MenuItem("About", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    Desktop.browseURL("https://mypc-app.web.app/"); 
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

    /*
    private void displayConfigWindow() {
        // Create the frame
        Frame frame = new Frame("MyPC configuration");       
        Label labelIp = new Label("IP address: "); 
        frame.add(labelIp);           
        
        // Allow it to close
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {  
                frame.dispose();  
            }  
        });  

        //Creating Text Field
        TextField txtIp = new TextField();
        txtIp.setSize(100, 20);
        frame.add(txtIp);

        // Button
        Button butTest = new Button("Test");
        frame.add(butTest);
        
        //setting frame size
        frame.setSize(500, 300);  
        
        //Setting the layout for the Frame
        frame.setLayout(new FlowLayout());
                
        frame.setVisible(true);        
    }

    */
}


    /*
    private static void displayTrayIcon() {
        TrayIcon trayIcon = null;
        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            Image image = Toolkit.getDefaultToolkit().getImage("MyPC-icon_512x512.png");

            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // execute default action of the application
                    // ...
                }
            };
            // create a popup menu
            PopupMenu popup = new PopupMenu();
            // create menu item for the default action
            MenuItem defaultItem = new MenuItem("..");
            defaultItem.addActionListener(listener);
            popup.add(defaultItem);
            /// ... add other items
            // construct a TrayIcon
            trayIcon = new TrayIcon(image, "Tray Demo", popup);
            // set the TrayIcon properties
            trayIcon.addActionListener(listener);
            // ...
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println(e);
            }
            // ...
        } else {
            // disable tray option in your application or
            // perform other actions
            debug("CONTROLLER: System tray icons not supported!");

        }
        // ...
        // some time later
        // the application state has changed - update the image
        //if (trayIcon != null) {
        //    trayIcon.setImage(updatedImage);
        //}
        // ...
    }
    */

        /*        
    // #########
    // TEST MENU
    // #########

    ActionListener callbackGray = new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            final MenuItem entry = (MenuItem) e.getSource();
            systemTray.setStatus(null);
            systemTray.setImage(MYPC_ICON_NORMAL);

            entry.setCallback(null);
//                systemTray.setStatus("Mail Empty");
            systemTray.getMenu().remove(entry);
            System.err.println("POW");
        }
    };



    MenuItem greenEntry = new MenuItem("Green Mail", new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            final MenuItem entry = (MenuItem) e.getSource();
            systemTray.setStatus("Some Mail!");
            systemTray.setImage(MYPC_ICON_NORMAL);

            entry.setCallback(callbackGray);
            entry.setImage(MYPC_ICON_NORMAL);
            entry.setText("Delete Mail");
            entry.setTooltip(null); // remove the tooltip
//                systemTray.remove(menuEntry);
        }
    });
    greenEntry.setImage(MYPC_ICON_NORMAL);
    // case does not matter
    greenEntry.setShortcut('G');
    greenEntry.setTooltip("This means you have green mail!");
    mainMenu.add(greenEntry);

    mainMenu.add(new MenuItem("Temp Directory", new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            try {
                Desktop.browseDirectory(OS.TEMP_DIR.getAbsolutePath());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }));

    Menu submenu = new Menu("Options", MYPC_ICON_NORMAL);
    submenu.setShortcut('t');

    MenuItem disableMenu = new MenuItem("Disable menu", MYPC_ICON_NORMAL, new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            MenuItem source = (MenuItem) e.getSource();
            source.getParent().setEnabled(false);
        }
    });
    submenu.add(disableMenu);

    submenu.add(new MenuItem("Hide tray", MYPC_ICON_NORMAL, new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            systemTray.setEnabled(false);
        }
    }));
    submenu.add(new MenuItem("Remove menu", MYPC_ICON_NORMAL, new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            MenuItem source = (MenuItem) e.getSource();
            source.getParent().remove();
        }
    }));

    submenu.add(new MenuItem("Add new entry to tray", new ActionListener() {
        @Override
        public
        void actionPerformed(final ActionEvent e) {
            systemTray.getMenu().add(new MenuItem("Random " + Integer.toString(new Random().nextInt(10))));
        }
    }));
    mainMenu.add(submenu);

    // TODO uncomment this and try, as getType() seems to be legit but the compiler complains. Maybe a problem with the jar?
    //MenuItem entry = new MenuItem("Type: " + systemTray.getType().toString());
    //entry.setEnabled(false);
    //systemTray.getMenu().add(entry);
    */


