package com.thenexusreborn.proxy.api;

import com.starmediadev.starlib.TimeUnit;
import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.gamearchive.GameInfo;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.punishment.*;
import com.thenexusreborn.api.server.Phase;
import com.thenexusreborn.api.stats.*;
import com.thenexusreborn.api.storage.objects.*;
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
    
    private Map<UUID, Long> loginTimes = new HashMap<>();
    private Map<UUID, Session> sessions = new HashMap<>();
    
    public ProxyPlayerManager(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public NexusPlayer createPlayerData(UUID uniqueId, String name) {
        NexusPlayer player = new NexusPlayer(uniqueId, name);
        player.setPlayerProxy(PlayerProxy.of(uniqueId));
        return player;
    }
    
    @EventHandler
    public void onLogin(LoginEvent e) {
        if (e.getConnection() == null) {
            return;
        }
        
        if (e.getConnection().getUniqueId() == null) {
            return;
        }
        CachedPlayer cachedPlayer = NexusAPI.getApi().getPlayerManager().getCachedPlayer(e.getConnection().getUniqueId());
        if (cachedPlayer == null) {
            NexusAPI.getApi().getThreadFactory().runAsync(() -> {
                NexusPlayer nexusPlayer = new NexusPlayer(e.getConnection().getUniqueId());
                if (e.getConnection().getName() != null && !e.getConnection().getName().equalsIgnoreCase("")) {
                    nexusPlayer.setName(e.getConnection().getName());
                }
                nexusPlayer.setFirstJoined(System.currentTimeMillis());
                nexusPlayer.setLastLogin(System.currentTimeMillis());
                nexusPlayer.setLastLogout(System.currentTimeMillis());
                NexusAPI.getApi().getPrimaryDatabase().push(nexusPlayer);
                CachedPlayer player = new CachedPlayer(nexusPlayer);
                NexusAPI.getApi().getPlayerManager().getCachedPlayers().put(nexusPlayer.getUniqueId(), player);
                NexusAPI.getApi().getNetworkManager().send("playercreate", nexusPlayer.getUniqueId().toString());
            });
        }
        
        if (cachedPlayer != null) {
            Punishment activePunishment = checkPunishments(cachedPlayer.getUniqueId());
            if (activePunishment != null) {
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', activePunishment.formatKick())));
                return;
            }
        }
        
        if (NexusAPI.PHASE == Phase.PRIVATE_ALPHA) {
            if (NexusAPI.getApi().getPrivateAlphaUsers().containsKey(e.getConnection().getUniqueId())) {
                return;
            }
            
            if (cachedPlayer != null && cachedPlayer.getRank().ordinal() <= Rank.HELPER.ordinal()) {
                return;
            }
            
            e.setCancelled(true);
            String privateAlphaMessage = """
                    &d&lThe Nexus Reborn &e&lPRIVATE ALPHA
                    &aThank you for your interest in &dThe Nexus Reborn
                    &aHowever we are currently in &ePrivate Alpha &aand therefore it is whitelist only
                    &aIf you would like to participate, you must be active
                    And join the &ePrivate Alpha Discord &ahere&b https://discord.gg/hkRn9jQbeb
                    &aIf you do not wish to be a part of the &ePrivate Alpha
                    &aPlease join the &fPublic Discord &afor updates until &6Public Beta&a:&b https://discord.gg/bawZKSWEpT""";
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
        ProxiedPlayer player = e.getPlayer();
        
        Session session = new Session(player.getUniqueId());
        session.start();
        this.sessions.put(player.getUniqueId(), session);
        
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
                this.loginTimes.put(nexusPlayer.getUniqueId(), System.currentTimeMillis());
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
                long playTime = System.currentTimeMillis() - this.loginTimes.get(nexusPlayer.getUniqueId());
                this.loginTimes.remove(nexusPlayer.getUniqueId());
                Session session = this.sessions.get(nexusPlayer.getUniqueId());
                if (session == null) {
                    plugin.getLogger().severe("There was no session for player " + nexusPlayer.getName());
                } else {
                    session.end();
                    
                    if (session.getTimeOnline() >= TimeUnit.MINUTES.toMilliseconds(5)) {
                        Database database = NexusAPI.getApi().getPrimaryDatabase();
                        Table table = database.getTable(GameInfo.class);
                        String query = "select * from " + table.getName() + " where `gameStart`>='" + session.getStart() + "' and `gameEnd` <= '" + session.getEnd() + "' and `players` like '%" + nexusPlayer.getName() + "%';";
                        try {
                            List<Row> rows = database.executeQuery(query);
                            session.setGamesPlayed(rows.size());
                        } catch (SQLException ex) {
                            NexusAPI.getApi().getLogger().info(query);
                            ex.printStackTrace();
                        }
                        
                        if (session.getGamesPlayed() > 0) {
                            database.push(session);
                        }
                    }
                }
                this.sessions.remove(nexusPlayer.getUniqueId());
                nexusPlayer.changeStat("playtime", playTime, StatOperator.ADD).push();
                StatHelper.consolidateStats(nexusPlayer);
                NexusAPI.getApi().getPrimaryDatabase().push(nexusPlayer);
            });
            this.players.remove(nexusPlayer.getUniqueId());
            if (nexusPlayer.getRank().ordinal() <= Rank.MEDIA.ordinal()) {
                StaffChat.sendDisconnect(nexusPlayer);
            }
            this.handlePlayerLeave(nexusPlayer);
        }
    }
}
