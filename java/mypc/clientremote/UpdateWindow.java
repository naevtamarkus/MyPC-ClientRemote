package mypc.clientremote;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dorkbox.util.Desktop;
import static mypc.clientremote.ClientRemote.debug;

public class UpdateWindow extends JFrame {
    private final URL MYPC_ICON_NORMAL = getClass().getResource("/MyPC-icon_512x512.png");
    private JLabel labelStatus; // to change status from a method

    // When isActive, currentWindow has the real object (which object changes after closing)
    private static boolean isActive = false;
    private static UpdateWindow currentWindow;

    // Windows should go into separate threads
    public static void display() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentWindow = new UpdateWindow();  // Let the constructor do the job
               // TODO make it so that we can only show this window once, to avoid overwriting this. Singleton maybe?
            }
         });
    }

    private UpdateWindow() {
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
                    Desktop.browseURL("https://mypc-app.web.app/"); 
                } catch (IOException e1) {
                    debug(Arrays.toString(e1.getStackTrace()));
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