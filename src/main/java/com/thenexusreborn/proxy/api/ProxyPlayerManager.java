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
import java.util.*;

public class ProxyPlayerManager extends PlayerManager implements Listener {
    @Override
    public NexusPlayer createPlayerData(UUID uniqueId, String name) {
        return NexusAPI.getApi().getPlayerFactory().createPlayer(uniqueId, name);
    }
    
    @EventHandler
    public void onPostLogin(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        
        InetSocketAddress socketAddress = (InetSocketAddress) player.getSocketAddress();
        String hostName = socketAddress.getHostString();
        NexusAPI.getApi().getThreadFactory().runAsync(() -> {
            long ipStart = System.currentTimeMillis();
            NexusAPI.getApi().getDataManager().addIpHistory(player.getUniqueId(), hostName);
            long ipEnd = System.currentTimeMillis();
        });
        
        long checkIpHistoryStart = System.currentTimeMillis();
        //TODO Reimplement IP History Here
//        Map<String, Set<UUID>> ipHistory = NexusAPI.getApi().getPlayerManager().getIpHistory();
//        if (ipHistory.containsKey(hostName)) {
//            ipHistory.get(hostName).add(player.getUniqueId());
//        } else {
//            ipHistory.put(hostName, new HashSet<>(Collections.singleton(player.getUniqueId())));
//        }
        long checkIpHistoryEnd = System.currentTimeMillis();
        
        long punishmentsStart = System.currentTimeMillis();
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
        long punishmentsEnd = System.currentTimeMillis();
        
        if (!getPlayers().containsKey(player.getUniqueId())) {
            NexusAPI.getApi().getThreadFactory().runAsync(() -> {
                long dataStart = System.currentTimeMillis();
                NexusPlayer nexusPlayer;
                if (!hasData(player.getUniqueId())) {
                    nexusPlayer = createPlayerData(player.getUniqueId(), player.getName());
                } else {
                    nexusPlayer = NexusAPI.getApi().getDataManager().loadPlayer(player.getUniqueId());
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
                
                getPlayers().put(nexusPlayer.getUniqueId(), nexusPlayer);
                NexusAPI.getApi().getDataManager().pushPlayer(nexusPlayer);
                long dataEnd = System.currentTimeMillis();
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
                NexusAPI.getApi().getDataManager().refreshPlayerStats(nexusPlayer);
                StatHelper.consolidateStats(nexusPlayer);
                saveToMySQLAsync(nexusPlayer);
            });
            this.players.remove(nexusPlayer.getUniqueId());
            if (nexusPlayer.getRank().ordinal() <= Rank.MEDIA.ordinal()) {
                StaffChat.sendDisconnect(nexusPlayer);
            }
        }
    }
}
