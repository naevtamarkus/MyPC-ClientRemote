package mypc.clientremote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static mypc.clientremote.ClientRemote.debug;

public class KeyMapCustom extends KeyMap {
    Map<Integer,KeyCombination> shortKeys;
    Map<Integer,KeyCombination> longKeys;
    Map<String, Integer> fullKeyboard;

    public KeyMapCustom() {
        shortKeys = new HashMap<>();
        longKeys = new HashMap<>();
        generateFullKeyboard();
        // Read input config file
        List<String> allLines;
        Path path = Paths.get("customkeymap.cfg");
        debug("Parsing customkeymap.cfg");
        try {
            allLines = Files.readAllLines(path);
        } catch (IOException e) {
            debug(e.getMessage());
            return;
        }
        // Builds the default keymap
        for (String line : allLines) {
            try {
                Matcher m = Pattern.compile("^([SL]) ([0-9]+) : (.*)$").matcher(line);
                if (m.find()) {
                    // Found a valid line. Extact valid info
                    debug("  processing line: t="+m.group(1)+" n="+m.group(2)+" k="+m.group(3));
                    // Parse input
                    KeyType type;
                    if (m.group(1).equals("S")) {
                        type = KeyType.SHORT;
                    } else {
                        type = KeyType.LONG;
                    }
                    int num = Integer.parseInt(m.group(2));
                    // Find the keys in the fullKeyboard dictionary
                    KeyCombination keyCombo;
                    String[] split = m.group(3).split(" \\+ ");
                    if (split.length == 3) {
                        keyCombo = new KeyCombination(fullKeyboard.get(split[0]), 
                            fullKeyboard.get(split[1]), fullKeyboard.get(split[2]));
                    } else if (split.length == 2) {
                        keyCombo = new KeyCombination(fullKeyboard.get(split[0]), 
                            fullKeyboard.get(split[1]));
                    } else {
                        keyCombo = new KeyCombination(fullKeyboard.get(split[0]));
                    }
                    // And finally add the key to the KeyMap
                    if (type == KeyType.SHORT) {
                        shortKeys.put(num, keyCombo);
                    } else {
                        longKeys.put(num, keyCombo);
                    }
                } else {
                    debug("  ignoring line: "+line);
                }
    
            } catch (Exception e) {
                // Just ignore the line if something is wrong
                debug ("  exception handling line: "+line);
                debug(e.getMessage());
            }
        }
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
    public JPanel getInstructions() {
        JLabel text = new JLabel("this is an example");
        JPanel panel = new JPanel();
        panel.add(text);
        return panel;
    }

    @Override
    public int size() {
        return shortKeys.size() + longKeys.size();
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        shortKeys.forEach((k, v) -> System.out.println("S " + k + " : " + v.toString()));
        longKeys.forEach((k, v) -> System.out.println("L " + k + " : " + v.toString()));
        return output.toString();
    }

    private void generateFullKeyboard() {
        fullKeyboard = new HashMap<>();
        // Loop over all possible KeyEvent constants to get their names
        int i = 0;
        while (i < 65536) {
            String description = KeyEvent.getKeyText(i);
            if (! description.contains("Unknown keyCode")) {
                fullKeyboard.put(description, i);
                //debug("  adding keyboard "+i+ " " +description);
            }
            i++;
            // Speed things up a bit by focusing only on certain groups
            // Check https://docs.oracle.com/javase/7/docs/api/constant-values.html#java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA
            if (i == 1000) i=61000;
            if (i == 62000) i=65000;
        }

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