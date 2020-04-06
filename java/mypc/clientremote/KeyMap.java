package mypc.clientremote;

import javax.swing.JPanel;

public abstract class KeyMap {

    public static enum KeyType { SHORT, LONG };

    abstract KeyCombination getKey(KeyType type, int num);

    abstract int size();

    abstract JPanel getInstructions();

}