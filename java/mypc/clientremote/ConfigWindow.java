package mypc.clientremote;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static mypc.clientremote.ClientRemote.EMAIL;
import static mypc.clientremote.ClientRemote.URL_EMAIL;
import static mypc.clientremote.ClientRemote.URL_WEBSITE;
import static mypc.clientremote.ClientRemote.debug;

public class ConfigWindow extends JFrame {
    private final URL MYPC_ICON_BIG = getClass().getResource("/MyPC-icon_512x512.png");
    private final JLabel labelStatus; // to change status from a method

    // When isActive, currentWindow has the real object (which object changes after
    // closing)
    private static boolean isActive = false;
    private static ConfigWindow currentWindow;

    private static JButton butTest;
    private static JTextField txtIp;
    private static Config config;
    private static JButton butScan;

    private static NetworkScanner scanner;

    // Windows should go into separate threads
    public static void display() {
        if (isActive) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentWindow = new ConfigWindow(); // Let the constructor do the job
            }
        });
    }

    private ConfigWindow() {
        isActive = true;
        try {
            // Set the default OS's style
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (final Exception e) {
            // nothing
        }

        // Set window icon
        final ImageIcon icon = new ImageIcon(MYPC_ICON_BIG);
        setIconImage(icon.getImage());

        // The "main" JPanel holds all the GUI components
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setContentPane(mainPanel);
        // Add panels
        final JPanel panel1 = new JPanel(new FlowLayout()); // for scan button
        final JPanel panel2 = new JPanel(new FlowLayout()); // for IP textfield and test button
        final JPanel panel3 = new JPanel(new FlowLayout()); // for scan status txt
        final JPanel panel4 = new JPanel(new FlowLayout(FlowLayout.LEFT)); // for connectOnStart checkbox
        final JPanel panel5 = new JPanel(new FlowLayout(FlowLayout.LEFT)); // for delayKeys checkbox
        final JPanel panel6 = new JPanel(new FlowLayout()); // for mailto link
        final JPanel panel7 = new JPanel(new FlowLayout()); // for websute link
        mainPanel.add(panel1);
        mainPanel.add(panel2);
        mainPanel.add(panel3);
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(new JSeparator()); // horizontal bar
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(panel4);
        mainPanel.add(panel5);
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(new JSeparator()); // horizontal bar
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(panel6);
        mainPanel.add(panel7);

        // Network Scan button
        butScan = new JButton("Scan local network");
        panel1.add(butScan);

        // IP address setting & test button
        final JLabel labelIp = new JLabel("TV IP address: ");
        panel2.add(labelIp);
        txtIp = new JTextField(15);
        panel2.add(txtIp);
        butTest = new JButton("Set & Test");
        panel2.add(butTest);

        // Load configured IP address
        config = Config.getInstance();
        txtIp.setText(config.getIpAddress());
        // And write to config if changed
        butTest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setIpAddress(txtIp.getText());
                // Try immediately
                if (config.isSetIpAddress())
                    ConnectionService.getInstance().connect();
            }
        });

        // Scan button
        butScan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (scanner != null && scanner.isScanning()) {
                    // If scanning, stop. Set the buttons to start again
                    labelStatus.setText("Stopping search..."); // onFinish() should replace it later
                    butScan.setText("Scan local network");
                    scanner.cancel();
                } else {
                    // If not scanning, start. Set the buttons to stop.
                    labelStatus.setText("Searching, please wait");
                    butScan.setText("Stop Scan");
                    startNetworkScan();
                }
            }
        });

        // Status
        labelStatus = new JLabel(" ");
        panel3.add(labelStatus);

        // ConnectOnStart checkbox
        final JCheckBox cbAuto = new JCheckBox("Connect to TV automatically on Startup");
        panel4.add(cbAuto);
        cbAuto.setSelected(config.isConnectOnStart());
        cbAuto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setConnectOnStart(cbAuto.isSelected());
            }
        });

        // ConnectOnStart checkbox
        final JCheckBox cbDelay = new JCheckBox("Delay keystrokes (debug only)");
        panel5.add(cbDelay);
        cbDelay.setSelected(config.isDelayKeys());
        cbDelay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDelayKeys(cbDelay.isSelected());
            }
        });

        panel6.add(new JLabel("Send feedback to: "));
        panel6.add(new JHyperlink(EMAIL, URL_EMAIL));

        panel7.add(new JLabel("MyPC App website: "));
        panel7.add(new JHyperlink(URL_WEBSITE, URL_WEBSITE));

        // Allow the window to close (without closing the app)
        // setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                isActive = false;
                dispose();
            }
        });
        setTitle("MyPC configuration"); // "super" JFrame sets title
        //setSize(450, 360); // "super" JFrame sets initial size
        setVisible(true); // "super" JFrame shows
        pack();

    }

    // This is called from the ClientRemote main class, which gets signals from the
    // ConnectionService in turn.
    public static void setStatus(final ConnectionService.Status status) {
        if (!isActive)
            return;
        switch (status) {
            case READY:
                currentWindow.labelStatus.setText("Ready");
                break;
            case CONNECTING:
                currentWindow.labelStatus.setText("Connecting...");
                break;
            case CONNECTED:
                currentWindow.labelStatus.setText("Connected");
                break;
            case RECONNECTING:
                currentWindow.labelStatus.setText("Reconnecting...");
                break;
            case ERROR:
                currentWindow.labelStatus.setText("ERROR");
                break;
        }
    }

    private static void startNetworkScan() {
        debug("Scanning local network");
        scanner = new NetworkScanner();
        // scanner.addFullNetworkFromIPTypeC("192.168.43.116");
        scanner.addIpAddress("127.0.0.1");
        scanner.addAllTypeCNetworksAvailable();
        scanner.addPort(50505);
        // scanner.addPortRange(1, 200);
        scanner.setStopAfterFound(true);

        scanner.setOnFinishCallback(new NetworkScanner.onFinishCallback() {
            @Override
            public void onFinish() {
                final int count = scanner.getHitResults().size();
                // int count = 0;
                debug("Scan finished, " + count + " found");
                currentWindow.labelStatus.setText("Scan finished, " + count + " found");
                butScan.setText("Scan local network");
            }
        });

        scanner.setOnFoundCallback(new NetworkScanner.onFoundCallback() {
            @Override
            public void onFound(final String ip, final int port) {
                debug (" found "+ip+":"+port);
                currentWindow.labelStatus.setText("Found: "+ip);
                txtIp.setText(""); // We seem to have problems resetting the field, we blank if first
                txtIp.setText(ip);
                //butTest.doClick();
            }
        });

        scanner.start();
    }


}

/* use this schema to start new processes:
SwingWorker worker = new SwingWorker<Integer, Integer>() {
    @Override
    public Integer doInBackground() {
        return scanner.start();
    }
};
*/
