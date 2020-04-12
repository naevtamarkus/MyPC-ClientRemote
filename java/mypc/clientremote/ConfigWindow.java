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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
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

import static mypc.clientremote.ClientRemote.DEBUGFILEPATH;
import static mypc.clientremote.ClientRemote.EMAIL;
import static mypc.clientremote.ClientRemote.URL_EMAIL;
import static mypc.clientremote.ClientRemote.URL_WEBSITE;
import static mypc.clientremote.ClientRemote.debug;
import static mypc.clientremote.ClientRemote.debugException;
import static mypc.clientremote.ClientRemote.JWhitePanel;


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
        // Set window icon
        final ImageIcon icon = new ImageIcon(MYPC_ICON_BIG);
        setIconImage(icon.getImage());

        // The "main" JPanel holds all the GUI components
        final JPanel mainPanel = new JWhitePanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setContentPane(mainPanel);
        // Add panels
        final JPanel scanPanel = new JWhitePanel(new FlowLayout()); // for scan button
        final JPanel ipPanel = new JWhitePanel(new FlowLayout()); // for IP textfield and test button
        final JPanel msgPanel = new JWhitePanel(new FlowLayout()); // for scan status txt
        final JPanel autoPanel = new JWhitePanel(new FlowLayout(FlowLayout.LEFT)); // for connectOnStart checkbox
        final JPanel delayPanel = new JWhitePanel(new FlowLayout(FlowLayout.LEFT)); // for delayKeys checkbox
        final JPanel panelViewLogs = new JWhitePanel(new FlowLayout()); // Button to view debug logs
        final JPanel mailtoPanel = new JWhitePanel(new FlowLayout(FlowLayout.LEFT)); // for mailto link
        final JPanel websitePanel = new JWhitePanel(new FlowLayout(FlowLayout.LEFT)); // for website link
        mainPanel.add(scanPanel);
        mainPanel.add(ipPanel);
        mainPanel.add(msgPanel);
        mainPanel.add(autoPanel);
        mainPanel.add(websitePanel);
        mainPanel.add(new JWhitePanel()); // separator
        mainPanel.add(new JSeparator()); // horizontal bar
        mainPanel.add(new JWhitePanel()); // separator
        mainPanel.add(panelViewLogs);
        mainPanel.add(mailtoPanel);
        mainPanel.add(delayPanel);

        // Network Scan button
        butScan = new JButton("Scan local network");
        scanPanel.add(butScan);

        // IP address setting & test button
        final JLabel labelIp = new JLabel("TV IP address: ");
        ipPanel.add(labelIp);
        txtIp = new JTextField(15);
        ipPanel.add(txtIp);
        butTest = new JButton("Set & Test");
        ipPanel.add(butTest);

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
        msgPanel.add(labelStatus);

        // ConnectOnStart checkbox
        final JCheckBox cbAuto = new JCheckBox("Connect to TV automatically on Startup");
        autoPanel.add(cbAuto);
        cbAuto.setSelected(config.isConnectOnStart());
        cbAuto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setConnectOnStart(cbAuto.isSelected());
            }
        });

        // ConnectOnStart checkbox
        final JCheckBox cbDelay = new JCheckBox("Delay keystrokes (debug only)");
        delayPanel.add(cbDelay);
        cbDelay.setSelected(config.isDelayKeys());
        cbDelay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDelayKeys(cbDelay.isSelected());
            }
        });

        JButton butViewLogs = new JButton("View debug log");
        panelViewLogs.add(butViewLogs);
        butViewLogs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                FileEditWindow.display(DEBUGFILEPATH, "Debug log",false, true);
            }
        });



        mailtoPanel.add(new JLabel("Send feedback to: "));
        mailtoPanel.add(new JHyperlink(EMAIL, URL_EMAIL));

        websitePanel.add(new JLabel("MyPC App website: "));
        websitePanel.add(new JHyperlink(URL_WEBSITE, URL_WEBSITE));

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
