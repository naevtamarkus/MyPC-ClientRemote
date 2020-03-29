package mypc.clientremote;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static mypc.clientremote.ClientRemote.debug;

public class ConfigWindow extends JFrame {
    private final URL MYPC_ICON_NORMAL = getClass().getResource("/MyPC-icon_512x512.png");
    private JLabel labelStatus; // to change status from a method

    // When isActive, currentWindow has the real object (which object changes after closing)
    private static boolean isActive = false;
    private static ConfigWindow currentWindow;

    private static JButton butTest;
    private static JTextField txtIp;
    private static Config config;

    // Windows should go into separate threads
    public static void display() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentWindow = new ConfigWindow();  // Let the constructor do the job
               // TODO make it so that we can only show this window once, to avoid overwriting this. Singleton maybe?
            }
         });
    }

    private ConfigWindow() {
        isActive = true;
        try { 
            // Set the default OS's style
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); // TODO improve this
        } catch (Exception e) {
            // nothing
        }

        // Set window icon
        ImageIcon icon = new ImageIcon(MYPC_ICON_NORMAL);
        setIconImage(icon.getImage());

        // The "main" JPanel holds all the GUI components
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        setContentPane(mainPanel);
        // Add panels
        JPanel panel1 = new JPanel(new FlowLayout());
        JPanel panel2 = new JPanel(new FlowLayout());
        JPanel panel3 = new JPanel(new FlowLayout());
        mainPanel.add(panel1);
        mainPanel.add(panel2);
        mainPanel.add(panel3);

        // Network Scan button
        JButton butScan = new JButton("Scan local network");
        panel1.add(butScan);

        // IP address setting & test button
        JLabel labelIp = new JLabel("IP address: "); 
        panel2.add(labelIp);
        txtIp = new JTextField(15);
        panel2.add(txtIp);
        butTest = new JButton("Set & Test");
        panel2.add(butTest);

        // Load configured IP address
        config = Config.getInstance();
        txtIp.setText(config.getIpAddress());
        // And write to config if changed
        butTest.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setIpAddress(txtIp.getText());
                // Try immediately
                if (config.isSetIpAddress()) ConnectionService.getInstance().connect();
            }
        });

        // Scan button
        butScan.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                labelStatus.setText("Searching, please wait");
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // Somehow running the scanner seems to interact with the drawing of the widgets
                        // Running it from a separate thread later seems to do the trick (not sure why, though)
                        scanNetwork(); 
                    }
                });
            }
        });

        // Status
        labelStatus = new JLabel(""); 
        panel3.add(labelStatus);

        // Allow it to close
        //setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {  
                isActive = false;
                dispose();  
            }  
        });  
        setTitle("MyPC configuration"); // "super" JFrame sets title
        setSize(450, 160);         // "super" JFrame sets initial size
        setVisible(true);          // "super" JFrame shows
        //pack();
  
    }

    // This is called from the ClientRemote main class, which gets signals from the ConnectionService in turn.
    public static void setStatus(ConnectionService.Status status) {
        if (! isActive) return;
        switch (status) {
            case READY: currentWindow.labelStatus.setText("Ready"); break;
            case CONNECTING: currentWindow.labelStatus.setText("Connecting..."); break;
            case CONNECTED: currentWindow.labelStatus.setText("Connected"); break;
            case RECONNECTING: currentWindow.labelStatus.setText("Reconnecting..."); break;
            case ERROR: currentWindow.labelStatus.setText("ERROR"); break;
        }
    }

    private static void scanNetwork() {
        debug("Scanning local network");
        NetworkScanner scanner = new NetworkScanner();
        //scanner.addFullNetworkFromIPTypeC("192.168.43.116");
        scanner.addAllTypeCNetworks();
        //scanner.addPort(50505);
        //scanner.addIpAddress("127.0.0.1");
        //scanner.addPortRange(1, 200);
        scanner.setStopAfterFound(true);    
        
        scanner.setOnFinishCallback(new NetworkScanner.onFinishCallback(){
            @Override
            public void onFinish() {
                int count = scanner.getHitResults().size();
                //int count = 0;
                debug("Scan finished, "+count+" found");                
                currentWindow.labelStatus.setText("Scan finished, "+count+" found");
            }
        });
        
        scanner.setOnFoundCallback(new NetworkScanner.onFoundCallback(){        
            @Override
            public void onFound(String ip, int port) {
                debug (" found "+ip+":"+port);
                // TODO enable this after doing tests
                //txtIp.setText(ip);
                //butTest.doClick();
            }
        });
        scanner.start();
    }

    /*
    private static void scanNetworkOld() {
        debug("Scanning local network");
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (Exception e) {
            currentWindow.labelStatus.setText("Error: no local network");
            return;
        }
        byte[] ip = localhost.getAddress();
        ConnectionService conn = ConnectionService.getInstance();
        //List<String> candidates = new ArrayList<>();

        // TODO we can only cover TypeC networks here, can do better!
        // TODO we should probably make 4-5 threads to scan the whole range faster
        for (int i = 1; i <= 254; i++) {
            try {
                ip[3] = (byte)i; 
                InetAddress address = InetAddress.getByAddress(ip);
                String candidate = address.toString().substring(1);

                if (address.isReachable(50)) {
                    //candidates.add(address.toString().substring(1));
                    debug(candidate + " is on the network");
                    // Check if the found host responds to the right port
                    if (conn.testHost(candidate, 300)) {
                        debug("Found MyPC: "+candidate);
                        txtIp.setText(candidate);
                        butTest.doClick();
                        return;
                    }
                        
                }
            } catch (Exception e) {
            }
        }
        currentWindow.labelStatus.setText("Not found");

        for (String candidate: candidates) {
            if (conn.testHost(candidate, 300)) {
                currentWindow.labelStatus.setText("Found "+candidate);
                txtIp.setText(candidate);
                butTest.doClick();
            }
        }

    }
    */
}