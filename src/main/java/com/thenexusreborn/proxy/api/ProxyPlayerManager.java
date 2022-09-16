package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.*;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.punishment.*;
import com.thenexusreborn.api.stats.*;
import com.thenexusreborn.api.util.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.*;

public class ProxyPlayerManager extends PlayerManager implements Listener {
    @Override
    public NexusPlayer createPlayerData(UUID uniqueId, String name) {
        NexusPlayer player = new NexusPlayer(uniqueId, name);
        player.setPlayerProxy(NexusAPI.getApi().getPlayerFactory().createProxy(player));
        return player;
    }
    
    @EventHandler
    public void onPostLogin(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        
        
        
        List<Punishment> punishments = NexusAPI.getApi().getPunishmentManager().getPunishmentsByTarget(player.getUniqueId());
        if (punishments.size() > 0) {
            for (Punishment punishment : punishments) {
                if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.BLACKLIST) {
                    if (punishment.isActive()) {
                        if (!PlayerManager.NEXUS_TEAM.contains(player.getUniqueId())) {
                            player.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', punishment.formatKick())));
                            return;
                        }
                    }
                }
            }
        }
        
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
                if (nexusPlayer.getFirstJoined() == 0) {
                    nexusPlayer.setFirstJoined(System.currentTimeMillis());
                }
                nexusPlayer.setLastLogin(System.currentTimeMillis());
    
                
                
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
                try {
                    List<Stat> stats = NexusAPI.getApi().getPrimaryDatabase().get(Stat.class, "uuid", nexusPlayer.getUniqueId());
                    for (Stat stat : stats) {
                        nexusPlayer.changeStat(stat.getName(), stat.getValue(), StatOperator.SET);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
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
