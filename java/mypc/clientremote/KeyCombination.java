package mypc.clientremote;

import java.awt.Robot;
import java.awt.event.KeyEvent;

// https://github.com/justin-taylor/Remote-Desktop-Server/blob/revert_to_old_state/src/AutoBot.java
// https://stackoverflow.com/questions/14572270/how-can-i-perfectly-simulate-keyevents/14615814

public class KeyCombination {
    private int key1; // They are all KeyEvent.XXX
    private int key2;
    private int key3;
    private int numKeys; // number of keys stored in this object

    public KeyCombination(int key1, int key2, int key3) {
        this.key1 = key1;
        this.key2 = key2;
        this.key3 = key3;
        this.numKeys = 3;
    }

    public KeyCombination(int key1, int key2) {
        this.key1 = key1;
        this.key2 = key2;
        this.numKeys = 2;
    }

    public KeyCombination(int key1) {
        this.key1 = key1;
        this.numKeys = 1;
    }

    public void hitKeys(Robot robot) {
        robot.keyPress(key1);
        if (numKeys > 1) robot.keyPress(key2);
        if (numKeys == 3) robot.keyPress(key3);
        if (numKeys == 3) robot.keyRelease(key3);
        if (numKeys > 1) robot.keyRelease(key2);
        robot.keyRelease(key1);
    }

    public void hitKeysDelayed(Robot robot) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // Nothing
                }
                hitKeys(robot);
            }
        }).start();
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(KeyEvent.getKeyText(key1));
        if (numKeys > 1) output.append(" + "+KeyEvent.getKeyText(key2));
        if (numKeys == 3) output.append(" + "+KeyEvent.getKeyText(key3));
        return output.toString();
    }
}