package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.punishment.*;
import com.thenexusreborn.api.server.Phase;
import com.thenexusreborn.api.stats.*;
import com.thenexusreborn.api.util.StaffChat;
import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.*;

public class ProxyPlayerManager extends PlayerManager implements Listener {
    
    private NexusProxy plugin;
    
    public ProxyPlayerManager(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public NexusPlayer createPlayerData(UUID uniqueId, String name) {
        NexusPlayer player = new NexusPlayer(uniqueId, name);
        player.setPlayerProxy(NexusAPI.getApi().getPlayerFactory().createProxy(player));
        return player;
    }
    
    @EventHandler
    public void onLogin(LoginEvent e) {
        //TODO Use this event for kicking for punishments and private alpha as enough player data is cached for this to be effective
        if (e.getConnection() == null) {
            return;
        }
        
        if (e.getConnection().getUniqueId() == null) {
            return;
        }
        CachedPlayer cachedPlayer = NexusAPI.getApi().getPlayerManager().getCachedPlayer(e.getConnection().getUniqueId());
        if (cachedPlayer == null) {
            return;
        }
        
        Punishment activePunishment = checkPunishments(cachedPlayer.getUniqueId());
        if (activePunishment != null) {
            e.setCancelled(true);
            e.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', activePunishment.formatKick())));
            return;
        }
        
        if (!cachedPlayer.isPrivateAlpha() && cachedPlayer.getRank().ordinal() > Rank.HELPER.ordinal()) {
            e.setCancelled(true);
            String privateAlphaMessage = "&d&lThe Nexus Reborn &e&lPRIVATE ALPHA\n" + 
                    "&aThank you for your interest in &dThe Nexus Reborn\n" + 
                    "&aHowever we are currently in &ePrivate Alpha &aand therefore it is whitelist only\n" + 
                    "&aIf you would like to participate, you must be active\n" + 
                    "And join the &ePrivate Alpha Discord &ahere&b https://discord.gg/hkRn9jQbeb\n" + 
                    "&aIf you do not wish to be a part of the &ePrivate Alpha\n" + 
                    "&aPlease join the &fPublic Discord &afor updates until &6Public Beta&a:&b https://discord.gg/bawZKSWEpT";
            e.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', privateAlphaMessage)));
        }
    }
    
    private Punishment checkPunishments(UUID uniqueId) {
        List<Punishment> punishments = NexusAPI.getApi().getPunishmentManager().getPunishmentsByTarget(uniqueId);
        if (punishments.size() > 0) {
            for (Punishment punishment : punishments) {
                if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.BLACKLIST) {
                    if (punishment.isActive()) {
                        if (!PlayerManager.NEXUS_TEAM.contains(uniqueId)) {
                            return punishment;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    @EventHandler
    public void onPostLogin(PostLoginEvent e) {
        plugin.getLogger().info("PostLoginEvent " + e.getPlayer().getUniqueId() + "/" + e.getPlayer().getName());
        ProxiedPlayer player = e.getPlayer();
        
        if (!getPlayers().containsKey(player.getUniqueId())) {
            NexusAPI.getApi().getThreadFactory().runAsync(() -> {
                NexusPlayer nexusPlayer = null;
                if (!hasData(player.getUniqueId())) {
                    nexusPlayer = createPlayerData(player.getUniqueId(), player.getName());
                } else {
                    try {
                        nexusPlayer = NexusAPI.getApi().getPrimaryDatabase().get(NexusPlayer.class, "uniqueId", player.getUniqueId()).get(0);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (ProxyServer.getInstance().getPlayer(nexusPlayer.getUniqueId()) == null) {
                    return;
                }
                
                if (nexusPlayer.getFirstJoined() == 0) {
                    nexusPlayer.setFirstJoined(System.currentTimeMillis());
                }
                nexusPlayer.setLastLogin(System.currentTimeMillis());
                
                if (!nexusPlayer.getName().equals(player.getName())) {
                    nexusPlayer.setName(player.getName());
                }
                
                if (NexusAPI.PHASE == Phase.ALPHA) {
                    if (!nexusPlayer.isAlpha()) {
                        nexusPlayer.setAlpha(true);
                    }
                } else if (NexusAPI.PHASE == Phase.BETA) {
                    if (!nexusPlayer.isBeta()) {
                        nexusPlayer.setBeta(true);
                    }
                }
                
                NexusAPI.getApi().getPrimaryDatabase().push(nexusPlayer);
                
                getPlayers().put(nexusPlayer.getUniqueId(), nexusPlayer);
                cachedPlayers.put(nexusPlayer.getUniqueId(), new CachedPlayer(nexusPlayer));
                InetSocketAddress socketAddress = (InetSocketAddress) player.getSocketAddress();
                String hostName = socketAddress.getHostString();
                NexusAPI.getApi().getPlayerManager().addIpHistory(player.getUniqueId(), hostName);
            });
        }
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        NexusPlayer nexusPlayer = getPlayers().get(e.getPlayer().getUniqueId());
        if (nexusPlayer != null) {
            NexusAPI.getApi().getThreadFactory().runAsync(() -> {
                nexusPlayer.setLastLogout(System.currentTimeMillis());
                long playTime = nexusPlayer.getLastLogout() - nexusPlayer.getLastLogin();
                nexusPlayer.changeStat("playtime", (playTime / 50), StatOperator.ADD);
                StatHelper.consolidateStats(nexusPlayer);
                saveToMySQLAsync(nexusPlayer);
            });
            this.players.remove(nexusPlayer.getUniqueId());
            if (nexusPlayer.getRank().ordinal() <= Rank.MEDIA.ordinal()) {
                StaffChat.sendDisconnect(nexusPlayer);
            }
            this.handlePlayerLeave(nexusPlayer);
        }
    }
}
