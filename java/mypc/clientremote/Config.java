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