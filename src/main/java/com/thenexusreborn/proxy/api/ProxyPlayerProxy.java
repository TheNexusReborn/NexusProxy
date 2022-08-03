package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.PlayerProxy;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class ProxyPlayerProxy implements PlayerProxy {
    private UUID uuid;
    
    public ProxyPlayerProxy(UUID uuid) {
        this.uuid = uuid;
    }
    
    @Override
    public void sendMessage(String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }
    
    @Override
    public boolean isOnline() {
        return ProxyServer.getInstance().getPlayer(uuid) != null;
    }
}
