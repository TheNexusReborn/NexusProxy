package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.tags.Tag;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

public class ProxyNexusPlayer extends NexusPlayer {
    public ProxyNexusPlayer(UUID uniqueId, String name) {
        super(uniqueId, name);
    }
    
    public ProxyNexusPlayer(UUID uniqueId, Map<Rank, Long> ranks, long firstJoined, long lastLogin, long lastLogout, long playTime, String lastKnownName, Tag tag, Set<Tag> unlockedTags) {
        super(uniqueId, ranks, firstJoined, lastLogin, lastLogout, playTime, lastKnownName, tag, unlockedTags);
    }
    
    public ProxiedPlayer getPlayer() {
        return ProxyServer.getInstance().getPlayer(this.uniqueId);
    }
    
    @Override
    public String getNameFromServer() {
        ProxiedPlayer player = getPlayer();
        if (player != null) {
            return player.getName();
        }
        return null;
    }
    
    @Override
    public void sendMessage(String message) {
        ProxiedPlayer player = getPlayer();
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
        }
    }
}
