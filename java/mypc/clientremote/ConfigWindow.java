package mypc.clientremote;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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
    private static JButton butScan;

    private static NetworkScanner scanner; 

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
        JPanel panel1 = new JPanel(new FlowLayout());  // for scan button
        JPanel panel2 = new JPanel(new FlowLayout());  // for IP textfield and test button
        JPanel panel3 = new JPanel(new FlowLayout());  // for scan status txt
        JPanel panel4 = new JPanel(new FlowLayout());  // for connectOnStart checkbox
        JPanel panel5 = new JPanel(new FlowLayout());  // for delayKeys checkbox
        mainPanel.add(panel1);
        mainPanel.add(panel2);
        mainPanel.add(panel3);
        mainPanel.add(panel4);
        mainPanel.add(panel5);

        // Network Scan button
        butScan = new JButton("Scan local network");
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
                if (scanner != null && scanner.isScanning()) {
                    // If scanning, stop. Set the buttons to start again
                    labelStatus.setText("Stopping search..."); // onFinish() should replace it later
                    butScan.setText("Scan local network");
                    scanner.cancel();
                    //SwingUtilities.invokeLater(new Runnable() {
                    //    @Override
                    //    public void run() {
                    //        // Somehow running the scanner seems to interact with the drawing of the widgets
                    //        // Running it from a separate thread later seems to do the trick (not sure why, though)
                    //        scanner.cancel(); 
                    //    }
                    //});
                } else {
                    // If not scanning, start. Set the buttons to stop.
                    labelStatus.setText("Searching, please wait");
                    butScan.setText("Stop Scan");
                    startNetworkScan();
                    //SwingUtilities.invokeLater(new Runnable() {
                    //    @Override
                    //    public void run() {
                    //        // Somehow running the scanner seems to interact with the drawing of the widgets
                    //        // Running it from a separate thread later seems to do the trick (not sure why, though)
                    //        startNetworkScan(); 
                    //    }
                    //});
                }
            }
        });

        // Status
        labelStatus = new JLabel(""); 
        panel3.add(labelStatus);

        // ConnectOnStart checkbox
        JCheckBox cbCOS = new JCheckBox("Connect on Start");
        panel4.add(cbCOS);
        cbCOS.addItemListener(new ItemListener() {    
            public void itemStateChanged(ItemEvent e) {     
                config.setConnectOnStart(e.getStateChange() == 1);
                debug("Setting: "+config.getConnectOnStart());
            }    
        });    

        JCheckBox cbCOS2 = new JCheckBox("Connect on Start");
        panel5.add(cbCOS);
        cbCOS2.addItemListener(new ItemListener() {    
            public void itemStateChanged(ItemEvent e) {     
                config.setConnectOnStart(e.getStateChange() == 1);
                debug("Setting: "+config.getConnectOnStart());
            }    
        });    

        // Allow the window to close (without closing the app)
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

    private static void startNetworkScan() {
        debug("Scanning local network");
        scanner = new NetworkScanner();
        //scanner.addFullNetworkFromIPTypeC("192.168.43.116");
        scanner.addIpAddress("127.0.0.1");
        scanner.addAllTypeCNetworksAvailable();
        scanner.addPort(50505);
        //scanner.addPortRange(1, 200);
        scanner.setStopAfterFound(true);    
        
        scanner.setOnFinishCallback(new NetworkScanner.onFinishCallback(){
            @Override
            public void onFinish() {
                int count = scanner.getHitResults().size();
                //int count = 0;
                debug("Scan finished, "+count+" found");                
                currentWindow.labelStatus.setText("Scan finished, "+count+" found");
                butScan.setText("Scan local network");
            }
        });
        
        scanner.setOnFoundCallback(new NetworkScanner.onFoundCallback(){        
            @Override
            public void onFound(String ip, int port) {
                debug (" found "+ip+":"+port);
                // TODO enable this after doing tests
                currentWindow.labelStatus.setText("Found: "+ip);
                txtIp.setText(""); // We seem to have problems resetting the field, we blank if first
                txtIp.setText(ip);
                //butTest.doClick();
            }
        });

        scanner.start();
        /*
        SwingWorker worker = new SwingWorker<Integer, Integer>() {
            @Override
            public Integer doInBackground() {
                return scanner.start();
            }
        };
        */
    }


}