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

import java.awt.Robot;
import java.awt.event.KeyEvent;

import static mypc.clientremote.ClientRemote.debugException;

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
                    debugException(e);
                }
                hitKeys(robot);
            }
        }).start();
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(KeyEvent.getKeyText(key1));
        if (numKeys > 1) output.append(" ++ "+KeyEvent.getKeyText(key2));
        if (numKeys == 3) output.append(" ++ "+KeyEvent.getKeyText(key3));
        return output.toString();
    }
}