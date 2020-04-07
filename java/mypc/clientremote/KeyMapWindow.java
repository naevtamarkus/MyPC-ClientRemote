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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
        final JPanel panel1 = new JPanel(new FlowLayout());
        final JPanel panel2 = new JPanel(new FlowLayout());
        final JPanel panel3 = new JPanel(new FlowLayout());
        final JPanel remotePanel = new JPanel(new FlowLayout());
        mainPanel.add(panel1);
        mainPanel.add(panel2);
        mainPanel.add(panel3);
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(new JSeparator()); // horizontal bar
        mainPanel.add(new JPanel()); // separator
        mainPanel.add(remotePanel);

        // Selection of KeyMap
        panel1.add(new JLabel("You can change how the keys from the Android TV remote convert into keys in your PC by selecting a Keymap:"));
        final JLabel labelIp = new JLabel("Select mapping: ");
        panel2.add(labelIp);
        String[] choices = { "DEFAULT", "CUSTOM"};
        JComboBox<String> dropdown = new JComboBox<String>(choices);
        panel2.add(dropdown);

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
        remotePanel.add(keyMap.getInstructions());

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
                remotePanel.removeAll();
                remotePanel.add(keyMap.getInstructions());
                connection.reloadKeymap();
                pack();
            }
        });

        // Show messages from connection
        panel3.add(new JLabel("Test current mapping (press TV key): "));
        JLabel lastMessage = new JLabel(" ");
        panel3.add(lastMessage);

        panel3.add(new JLabel(" -> "));
        JLabel lastKey = new JLabel(" ");
        panel3.add(lastKey);

        connection.setMessageReceivedEvent(new ConnectionService.MessageReceivedEvent() {
            @Override
            public void onMessageReceived(String msg) {
                lastMessage.setText("");
                lastMessage.setText(msg);

                // Parse message and find mapped key
                Message message = new Message(msg);
                if (message.isKey()) {
                    KeyCombination combo = keyMap.getKey(message.getKeyType(), message.getKeyCode());
                    if (combo != null) {
                        lastKey.setText(combo.toString());
                    } else {
                        lastKey.setText("(not mapped)");
                    }
                }

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
