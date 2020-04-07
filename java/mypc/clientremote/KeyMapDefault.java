package mypc.clientremote;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.event.KeyEvent;

public class KeyMapDefault extends KeyMap {
    Map<Integer,KeyCombination> shortKeys;
    Map<Integer,KeyCombination> longKeys;

    public KeyMapDefault() {
        shortKeys = new HashMap<>();
        longKeys = new HashMap<>();
        // Builds the default keymap
        shortKeys.put(19, new KeyCombination(KeyEvent.VK_UP));
        shortKeys.put(20, new KeyCombination(KeyEvent.VK_DOWN));
        shortKeys.put(21, new KeyCombination(KeyEvent.VK_LEFT));
        shortKeys.put(22, new KeyCombination(KeyEvent.VK_RIGHT));
        shortKeys.put(23, new KeyCombination(KeyEvent.VK_ENTER));
        //case 4:   hitKey(KeyEvent.VK_BACK_SPACE); break;  // Back key
        shortKeys.put(4, new KeyCombination(KeyEvent.VK_ESCAPE));

        // Extended navigation keys
        shortKeys.put(166, new KeyCombination(KeyEvent.VK_TAB)); // program up
        shortKeys.put(167, new KeyCombination(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB)); // program down
        //case 111: hitKey(KeyEvent.VK_ESCAPE); break;  // exit
        shortKeys.put(111, new KeyCombination(KeyEvent.VK_ALT, KeyEvent.VK_F4));  // exit
        shortKeys.put(233, new KeyCombination(KeyEvent.VK_CONTEXT_MENU));  // teletext
        shortKeys.put(165, new KeyCombination(KeyEvent.VK_WINDOWS));  // teletext
        shortKeys.put(229, new KeyCombination(KeyEvent.VK_ALT, KeyEvent.VK_ESCAPE)); // back-forth key between prog/volume

        //case 62: hitKey(KeyEvent.VK_SPACE); break;
        //case 66: hitKey(KeyEvent.VK_ENTER); break;
        //case 67: hitkey(KeyEvent.VK_BACK_SPACE); break;

        // Quick launch icons (in order)
        shortKeys.put(8, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_1));  // numpad 1
        shortKeys.put(9, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_2));  // numpad 2
        shortKeys.put(10, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_3));  // numpad 3
        shortKeys.put(11, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_4));  // numpad 4
        shortKeys.put(12, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_5));  // numpad 5
        shortKeys.put(13, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_6));  // numpad 6
        shortKeys.put(14, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_7));  // numpad 7
        shortKeys.put(15, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_8));  // numpad 8
        shortKeys.put(16, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_9));  // numpad 9
        shortKeys.put(7, new KeyCombination(KeyEvent.VK_WINDOWS, KeyEvent.VK_0));  // numpad 0 (quick launch 10)
        
        // Playback keys
        shortKeys.put(126, new KeyCombination(KeyEvent.VK_SPACE));  // Play & Pause
        shortKeys.put(85, new KeyCombination(KeyEvent.VK_SPACE));  // Play & Pause
        shortKeys.put(89, new KeyCombination(KeyEvent.VK_CONTROL, KeyEvent.VK_LEFT));  // Rewind
        shortKeys.put(90, new KeyCombination(KeyEvent.VK_CONTROL, KeyEvent.VK_RIGHT));  // Forewind
        shortKeys.put(88, new KeyCombination(KeyEvent.VK_N));  // Next
        shortKeys.put(87, new KeyCombination(KeyEvent.VK_P));  // Previous
        shortKeys.put(86, new KeyCombination(KeyEvent.VK_S));  // Stop
        shortKeys.put(175, new KeyCombination(KeyEvent.VK_F));  // Full Screen
    }

    // Returns null if not found
    @Override
    public KeyCombination getKey(KeyType type, int num) {
        if (type == KeyType.SHORT) {
            return shortKeys.get(num);
        } else {
            return longKeys.get(num);
        }
    }

    @Override
    public int size() {
        return shortKeys.size() + longKeys.size();
    }

    @Override
    public JPanel getInstructions() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel panel1 = new JPanel(new FlowLayout());
        final JPanel panel2 = new JPanel(new FlowLayout());
        final JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mainPanel.add(panel1);
        mainPanel.add(panel2);
        mainPanel.add(panel3);

        // Panel 1
        String html1 = "<html><body>" +
                "The Default Keymap defines a minimum mapping that should work for most TV's and PC's. <br><br> " +
                "It is defined like this:<br></body></html>";

        String html2 = "<html><body>" +
                "<h3>Quick Launch</h3>" +
                "These buttons in the TV remote trigger applications<br>" +
                "in the quick-launch menu. For instance: " +
                "<ul><li>Channel 1 opens the first application " +
                "<li>Channel 2 opens the second application " +
                "<li>and so on...</ul><br>" +
                "<h3>Navigation</h3>" +
                "Several TV remote buttons help moving around:<br>" +
                "<ul><li>Directional pad corresponds to the arrows" +
                "<li>Channel up/down moves between elements in the<br>window, as the Tab key" +
                "<li>Central button corresponds to the Enter key " +
                "<li>The back key is the Escape" +
                "<li>Teletext, Sync and Exit keys have extra functions<br>to be experimented with</ul><br>" +
                "<h3>Playback</h3>" +
                "If available, playback keys are shortcuts to VLC Player <br>control keys";
        panel1.add(new JLabel(html1));

        Image remoteControl = new ImageIcon(this.getClass().getResource("/remote-control.png"))
                .getImage().getScaledInstance(139, 446, Image.SCALE_DEFAULT);
        JLabel imageLabel = new JLabel(new ImageIcon(remoteControl));
        imageLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        panel2.add(imageLabel);
        panel2.add(new JLabel((html2)));

        panel3.add(new JLabel("Currently, this KeyMap contains "+size()+" valid keys mapped. "));

        return mainPanel;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        shortKeys.forEach((k, v) -> System.out.println("S " + k + " : " + v.toString()));
        longKeys.forEach((k, v) -> System.out.println("L " + k + " : " + v.toString()));
        return output.toString();
    }
    

}

/* Other keys: 
// TOP small buttons
241 - Digital/analog
232 - TV/Radio
111 - Exit
NO  - Ext. box menu

// Numeric pad
[8-16] - [1-9] 
7   - 0
165 - i+
233 - Teletext 

// Teletext colors
183 - txt red
184 - txt green
185 - txt yellow
186 - txt blue

// Arrows and surrounding
[19-22] - up/down/left/right
23  - Center key
4   - Back key
NO  - volume up/down
229 - back-forth arrows between volume and programs
NO  - mute
166 - program up
167 - program down

222 - audio
89  - rewind
126 - play
90  - forewind

175 - options...
88  - -1 track
85  - pause
87  - +1 track

NO  - Help
130 - Rec
86  - Stop
NO  - Title list

*/