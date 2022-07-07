package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.tags.Tag;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

public class ProxyNexusPlayer extends NexusPlayer {
    public ProxyNexusPlayer(CachedPlayer cachedPlayer) {
        super(cachedPlayer);
    }
    
    public ProxyNexusPlayer(UUID uniqueId) {
        super(uniqueId);
    }
    
    public ProxyNexusPlayer(UUID uniqueId, String name) {
        super(uniqueId, name);
    }
    
    public ProxyNexusPlayer(UUID uniqueId, Map<Rank, Long> ranks, long firstJoined, long lastLogin, long lastLogout, String lastKnownName, Tag tag, Set<String> unlockedTags, boolean prealpha, boolean alpha, boolean beta) {
        super(uniqueId);
        this.ranks = ranks;
        setFirstJoined(firstJoined);
        setLastLogin(lastLogin);
        setLastLogout(lastLogout);
        this.name = lastKnownName;
        setTag(tag);
        this.unlockedTags = unlockedTags;
        setPrealpha(prealpha);
        setAlpha(alpha);
        setBeta(beta);
    }
    
    public ProxiedPlayer getPlayer() {
        return ProxyServer.getInstance().getPlayer(this.uniqueId);
    }
    
    @Override
    public String getName() {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(this.uniqueId);
        if (player != null) {
            return player.getName();
        }
        
        return super.getName();
    }
    
    @Override
    public void sendMessage(String message) {
        ProxiedPlayer player = getPlayer();
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
        }
    }
    
    @Override
    public boolean isOnline() {
        return ProxyServer.getInstance().getPlayer(this.uniqueId) != null;
    }
}
