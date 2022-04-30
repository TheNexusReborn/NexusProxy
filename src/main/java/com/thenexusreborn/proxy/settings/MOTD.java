package com.thenexusreborn.proxy.settings;

public class MOTD {
    private String line1, line2;
    
    public MOTD(String line1, String line2) {
        this.line1 = line1;
        this.line2 = line2;
    }
    
    public MOTD() {
    }
    
    public String getLine1() {
        return line1;
    }
    
    public void setLine1(String line1) {
        this.line1 = line1;
    }
    
    public String getLine2() {
        return line2;
    }
    
    public void setLine2(String line2) {
        this.line2 = line2;
    }
}
