package mypc.clientremote;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

public class KeyMapWindow extends JFrame {
    private final URL MYPC_ICON_BIG = getClass().getResource("/MyPC-icon_512x512.png");

    // When isActive, currentWindow has the real object (which object changes after
    // closing)
    private static boolean isActive = false;
    private static KeyMapWindow currentWindow;
    private static ConnectionService connection;

    // Windows should go into separate threads
    public static void display() {
        if (isActive) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentWindow = new KeyMapWindow(); // Let the constructor do the job
            }
        });
    }

    private KeyMapWindow() {
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
        final JPanel panel3 = new JPanel(new FlowLayout()); // for the KeyMap description
        mainPanel.add(panel1);
        mainPanel.add(panel2);
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(new JSeparator()); // horizontal bar
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(panel3);

        // Selection of KeyMap
        final JLabel labelIp = new JLabel("Select Mapping: ");
        panel1.add(labelIp);
        String[] choices = { "DEFAULT", "CUSTOM"};
        JComboBox<String> dropdown = new JComboBox<String>(choices);
        panel1.add(dropdown);
        JButton butTest = new JButton("Test");
        panel1.add(butTest);

        // Select configured mapping
        Config config = Config.getInstance();
        KeyMap keyMap;
        if (config.getKeyMap().equals("CUSTOM")) {
            dropdown.setSelectedIndex(1); // custom
            keyMap = new KeyMapCustom();
        } else {
            dropdown.setSelectedIndex(0); // default
            keyMap = new KeyMapDefault();
        }
        panel3.add(keyMap.getInstructions());

        // Add listener to the dropdown
        connection = ConnectionService.getInstance();
        dropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) dropdown.getSelectedItem();
                debug ("Scrolldown selected: "+selectedItem);
                config.setKeyMap(selectedItem);
                KeyMap keyMap;
                if (selectedItem.equals("CUSTOM")) {
                    keyMap = new KeyMapCustom();
                } else {
                    keyMap = new KeyMapDefault();
                }
                panel3.removeAll();
                panel3.add(keyMap.getInstructions());
                connection.reloadKeymap();
                pack();
            }
        });

        // Show messages from connection
        panel2.add(new JLabel("Message from TV: "));
        JLabel lastMessage = new JLabel("");
        panel2.add(lastMessage);
        connection.setMessageReceivedEvent(new ConnectionService.MessageReceivedEvent() {
            @Override
            public void onMessageReceived(String message) {
                lastMessage.setText("");
                lastMessage.setText(message);
            }
        });

        // Allow the window to close (without closing the app)
        // setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                isActive = false;
                connection.setMessageReceivedEvent(null); // reset service so that
                dispose();
            }
        });
        setTitle("MyPC Key Mapping"); // "super" JFrame sets title
        //setSize(450, 360); // "super" JFrame sets initial size
        setVisible(true); // "super" JFrame shows
        pack();
    }

}

/*
SwingWorker worker = new SwingWorker<Integer, Integer>() {
    @Override
    public Integer doInBackground() {
        return scanner.start();
    }
};
*/
