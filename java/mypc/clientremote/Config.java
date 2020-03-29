package mypc.clientremote;

import java.util.prefs.Preferences;

public class Config {
    private final int port = 50505;
    private static Config instance;
    private Preferences prefs;
    // Pref IDs
    private String IPADDRESS_ID = "IPADDRESS";

    // Config items:
    private boolean debug = false;
    private boolean delayKeys = false;

    public static Config getInstance() { 
        if (instance == null) instance = new Config(); 
        return instance; 
    } 

    private Config() {
        // Constructor
        prefs = Preferences.userRoot().node(this.getClass().getName());
    }

    public void setDebug(boolean val) { debug = val; }
    public boolean isDebug() { return debug; }

    public void setDelayKeys(boolean val) { delayKeys = val; }
    public boolean isDelayKeys() { return delayKeys; }

    // TODO we may want to cache the IP address in the object
    public void setIpAddress(String addr) { prefs.put(IPADDRESS_ID, addr); }
    public String getIpAddress() { return prefs.get(IPADDRESS_ID, ""); }
    public boolean isSetIpAddress() { return ! getIpAddress().equals(""); }

    public int getPort() { return port; }

}