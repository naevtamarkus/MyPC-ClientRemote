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
import java.io.IOException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dorkbox.util.Desktop;

import static mypc.clientremote.ClientRemote.URL_WEBSITE;
import static mypc.clientremote.ClientRemote.debug;
import static mypc.clientremote.ClientRemote.debugException;
import static mypc.clientremote.ClientRemote.JWhitePanel;

public class UpdateWindow extends JFrame {
    private final URL MYPC_ICON_NORMAL = getClass().getResource("/MyPC-icon_512x512.png");
    private JLabel labelStatus; // to change status from a method

    // When isActive, currentWindow has the real object (which object changes after closing)
    private static boolean isActive = false;
    private static UpdateWindow currentWindow;

    // Windows should go into separate threads
    public static void display() {
        if (isActive) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentWindow = new UpdateWindow();  // Let the constructor do the job
            }
         });
    }

    private UpdateWindow() {
        isActive = true;
        // Set window icon
        ImageIcon icon = new ImageIcon(MYPC_ICON_NORMAL);
        setIconImage(icon.getImage());

        // The "main" JPanel holds all the GUI components
        JPanel mainPanel = new JWhitePanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        setContentPane(mainPanel);
        // Add panels
        JPanel panel1 = new JWhitePanel(new FlowLayout());
        JPanel panel2 = new JWhitePanel(new FlowLayout());
        mainPanel.add(panel1);
        mainPanel.add(panel2);

        // Message
        JLabel labelMsg1 = new JLabel("The current version of MyPC Remote Client is outdated\n"); 
        JLabel labelMsg2 = new JLabel("You must download a newer version for it to work normally\n"); 
        panel1.add(labelMsg1);
        panel1.add(labelMsg2);
        
        // Update button
        JButton butUpdate = new JButton("Update");
        panel2.add(butUpdate);

        // And write to config if changed
        butUpdate.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.browseURL(URL_WEBSITE);
                } catch (IOException e1) {
                    debugException(e1);
                }
            }
        });

        // Allow it to close
        //setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {  
                isActive = false;
                dispose();  
            }  
        });  
        setTitle("MyPC update"); // "super" JFrame sets title
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
}