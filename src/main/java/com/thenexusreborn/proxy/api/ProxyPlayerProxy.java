package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.PlayerProxy;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class ProxyPlayerProxy extends PlayerProxy {
    public ProxyPlayerProxy(UUID uuid) {
        super(uuid);
    }
    
    @Override
    public void sendMessage(String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uniqueId);
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }
    
    @Override
    public boolean isOnline() {
        return ProxyServer.getInstance().getPlayer(uniqueId) != null;
    }
    
    @Override
    public String getName() {
        return ProxyServer.getInstance().getPlayer(uniqueId).getName();
    }
}
