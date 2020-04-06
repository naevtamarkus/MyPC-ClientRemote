package mypc.clientremote;

import java.nio.charset.MalformedInputException;
import java.util.AbstractMap;
import java.util.Map;

import static mypc.clientremote.ClientRemote.debug;

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
