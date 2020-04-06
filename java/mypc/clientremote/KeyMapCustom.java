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

    private static String configFileName = "customkeymap.cfg";

    public KeyMapCustom() {
        shortKeys = new HashMap<>();
        longKeys = new HashMap<>();
        generateFullKeyboard();
        // Read input config file
        List<String> allLines;
        Path path = Paths.get(configFileName);
        debug("Parsing "+configFileName);
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
        JLabel text = new JLabel("This is the Custom KeyMap");
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

/*
3 Cancel
8 Backspace
9 Tab
10 Enter
12 Clear
16 Shift
17 Ctrl
18 Alt
19 Pause
20 Caps Lock
21 Kana
24 Final
25 Kanji
27 Escape
28 Convert
29 No Convert
30 Accept
31 Mode Change
32 Space
33 Page Up
34 Page Down
35 End
36 Home
37 Left
38 Up
39 Right
40 Down
44 Comma
45 Minus
46 Period
47 Slash
48 0
49 1
50 2
51 3
52 4
53 5
54 6
55 7
56 8
57 9
59 Semicolon
61 Equals
65 A
66 B
67 C
68 D
69 E
70 F
71 G
72 H
73 I
74 J
75 K
76 L
77 M
78 N
79 O
80 P
81 Q
82 R
83 S
84 T
85 U
86 V
87 W
88 X
89 Y
90 Z
91 Open Bracket
92 Back Slash
93 Close Bracket
96 NumPad-0
97 NumPad-1
98 NumPad-2
99 NumPad-3
100 NumPad-4
101 NumPad-5
102 NumPad-6
103 NumPad-7
104 NumPad-8
105 NumPad-9
106 NumPad *
107 NumPad +
108 NumPad ,
109 NumPad -
110 NumPad .
111 NumPad /
112 F1
113 F2
114 F3
115 F4
116 F5
117 F6
118 F7
119 F8
120 F9
121 F10
122 F11
123 F12
127 Delete
128 Dead Grave
129 Dead Acute
130 Dead Circumflex
131 Dead Tilde
132 Dead Macron
133 Dead Breve
134 Dead Above Dot
135 Dead Diaeresis
136 Dead Above Ring
137 Dead Double Acute
138 Dead Caron
139 Dead Cedilla
140 Dead Ogonek
141 Dead Iota
142 Dead Voiced Sound
143 Dead Semivoiced Sound
144 Num Lock
145 Scroll Lock
150 Ampersand
151 Asterisk
152 Double Quote
153 Less
154 Print Screen
155 Insert
156 Help
157 Meta
160 Greater
161 Left Brace
162 Right Brace
192 Back Quote
222 Quote
224 Up
225 Down
226 Left
227 Right
240 Alphanumeric
241 Katakana
242 Hiragana
243 Full-Width
244 Half-Width
245 Roman Characters
256 All Candidates
257 Previous Candidate
258 Code Input
259 Japanese Katakana
260 Japanese Hiragana
261 Japanese Roman
262 Kana Lock
263 Input Method On/Off
512 At
513 Colon
514 Circumflex
515 Dollar
516 Euro
517 Exclamation Mark
518 Inverted Exclamation Mark
519 Left Parenthesis
520 Number Sign
521 Plus
522 Right Parenthesis
523 Underscore
524 Windows
525 Context Menu
61440 F13
61441 F14
61442 F15
61443 F16
61444 F17
61445 F18
61446 F19
61447 F20
61448 F21
61449 F22
61450 F23
61451 F24
65312 Compose
65368 Begin
65406 Alt Graph
65480 Stop
65481 Again
65482 Props
65483 Undo
65485 Copy
65487 Paste
65488 Find
65489 Cut

 */