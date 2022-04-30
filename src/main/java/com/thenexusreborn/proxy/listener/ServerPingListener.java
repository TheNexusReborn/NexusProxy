package com.thenexusreborn.proxy.listener;

import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ServerPingListener implements Listener {
    
    private NexusProxy plugin;
    
    public ServerPingListener(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onServerPing(ProxyPingEvent e) {
        String description = "";
        if (plugin.getMotd().getLine1() != null && !plugin.getMotd().getLine1().equals("")) {
            description += plugin.getMotd().getLine1();
        }
        
        if (plugin.getMotd().getLine2() != null && !plugin.getMotd().getLine2().equals("")) {
            description += "\n" + plugin.getMotd().getLine2();
        }
        
        if (description != null && !description.equals("")) {
            e.getResponse().setDescriptionComponent(new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', description))));
        }
    }
}