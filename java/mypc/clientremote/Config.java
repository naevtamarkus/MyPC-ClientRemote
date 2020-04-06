package mypc.clientremote;

import java.util.prefs.Preferences;

public class Config {
    private static Config instance;
    private Preferences prefs;

    // Config items:
    private final int port = 50505;
    private boolean debug = false;

    public static Config getInstance() { 
        if (instance == null) instance = new Config(); 
        return instance; 
    } 

    private Config() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
    }

    public void setDebug(boolean val) { debug = val; }
    public boolean isDebug() { return debug; }

    public void setIpAddress(String val) { prefs.put("IPADDRESS", val); }
    public String getIpAddress() { return prefs.get("IPADDRESS", ""); }
    public boolean isSetIpAddress() { return ! getIpAddress().equals(""); }

    public void setConnectOnStart(boolean val) { prefs.putBoolean("CONNECTONSTART", val); }
    public boolean isConnectOnStart() { return prefs.getBoolean("CONNECTONSTART", true); }

    public void setDelayKeys(boolean val) { prefs.putBoolean("DELAYKEYS", val); }
    public boolean isDelayKeys() { return prefs.getBoolean("DELAYKEYS", false); }

    public void setKeyMap(String val) { prefs.put("KEYMAP", val); }
    public String getKeyMap() { return prefs.get("KEYMAP", "DEFAULT"); }

    public int getPort() { return port; }

}