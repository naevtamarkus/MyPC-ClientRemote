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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static mypc.clientremote.ClientRemote.debug;
import static mypc.clientremote.ClientRemote.debugException;

public class FileEditWindow extends JFrame {
    private final URL MYPC_ICON_BIG = getClass().getResource("/MyPC-icon_512x512.png");
    private static boolean isActive = false;
    private static FileEditWindow currentWindow;

    // Windows should go into separate threads
    public static void display(Path filePath, String windowName, boolean canSave, boolean scrollDown) {
        if (isActive) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentWindow = new FileEditWindow(filePath, windowName, canSave, scrollDown); // Let the constructor do the job
            }
        });
    }

    private FileEditWindow(Path filePath, String windowName, boolean canSave, boolean scrollDown) {
        isActive = true;
        debug ("Creating FileEdit Window: "+filePath.toAbsolutePath());
        // Set window icon
        final ImageIcon icon = new ImageIcon(MYPC_ICON_BIG);
        setIconImage(icon.getImage());

        // The "main" JPanel holds all the GUI components
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        setContentPane(mainPanel);
        // Add panels
        final JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JPanel panel2 = new JPanel(new FlowLayout());
        mainPanel.add(panel1);
        mainPanel.add(panel2);

        // Add first the text area
        JTextArea textArea = new JTextArea(30,80);
        textArea.setLineWrap(true);
        textArea.setEditable ( canSave );
        JScrollPane scroll = new JScrollPane ( textArea );
        scroll.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        if (scrollDown) {
            scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    e.getAdjustable().setValue(e.getAdjustable().getMaximum());
                }
            });
        }

        panel2.add(scroll);
        //panel2.add(textArea, BorderLayout.CENTER);
        // Read the contents
        FileReader reader = null;
        try {
            reader = new FileReader(filePath.toFile());
            textArea.read(reader, null);
        } catch (IOException ex) {
            textArea.setText("ERROR READING FILE: "+ ex.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    debugException(e);
                }
            }
        }


        // And then the save button, if necessary
        if (canSave) {
            JButton saveBut = new JButton("Save");
            JLabel msgLabel = new JLabel("");
            panel1.add(saveBut);
            panel1.add(msgLabel);
            saveBut.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(filePath.toFile());
                        textArea.write(writer);
                    } catch (IOException ex) {
                        msgLabel.setText("Error writing to file");
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e2) {
                                debugException(e2);
                            }
                        }
                    }

                }
            });

        }

        // Allow the window to close (without closing the app)
        // setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                isActive = false;
                dispose();
            }
        });
        setTitle(windowName); // "super" JFrame sets title
        //setSize(450, 360); // "super" JFrame sets initial size
        setVisible(true); // "super" JFrame shows
        pack();
    }

}
