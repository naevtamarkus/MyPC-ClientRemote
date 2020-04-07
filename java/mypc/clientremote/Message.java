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

public class Message {
    private String origMsg;
    private String firstWord;
    private String rest;

    // Possible status
    private boolean isError = false;
    private boolean isKey = false;
    private boolean isInfo = false;
    private int code = 0;

    public Message(String msg) {
        this.origMsg = msg;
        try {
            this.firstWord = msg.split(" ")[0];
            this.rest = msg.split(" ",2)[1];
            if (firstWord.equals("S") || firstWord.equals("L")) {
                this.code = Integer.parseInt(rest); // if it fails with an exception, isKey is never set to true
                this.isKey = true;
            } else if (firstWord.equals("I")) {
                // TODO parse all types of Info messages (to say isInfo=true)
                this.isInfo = true;
            }
        } catch (Exception e) {
            this.isError = true;
        }
    }

    public boolean isKey() { return isKey; }
    public boolean isError() { return isError; }
    public boolean isInfo() { return isInfo; }

    public KeyMap.KeyType getKeyType() {
        return firstWord.equals("S") ? KeyMap.KeyType.SHORT : KeyMap.KeyType.LONG;
    }

    public int getKeyCode() {
        return code;
    }

    public String getInfo() {
        return rest;
    }

}
