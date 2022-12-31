package com.thenexusreborn.proxy;

import com.starmediadev.starsql.objects.Database;
import com.thenexusreborn.api.*;
import com.thenexusreborn.api.network.NetworkContext;
import com.thenexusreborn.api.network.cmd.NetworkCommand;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.punishment.*;
import com.thenexusreborn.api.registry.*;
import com.thenexusreborn.api.server.Environment;
import com.thenexusreborn.proxy.api.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.File;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;

public class BungeeNexusAPI extends NexusAPI {
    
    private final NexusProxy plugin;
    
    public BungeeNexusAPI(NexusProxy plugin) {
        super(Environment.valueOf(plugin.getConfig().getString("environment")), NetworkContext.SERVER, plugin.getLogger(), new ProxyPlayerManager(plugin), new ProxyThreadFactory(plugin), new ProxyServerManager(plugin));
        this.plugin = plugin;
        PlayerProxy.setProxyClass(ProxyPlayerProxy.class);
    }
    
    @Override
    public void registerDatabases(DatabaseRegistry registry) {
        Configuration databasesSection = plugin.getConfig().getSection("databases");
        if (databasesSection != null) {
            for (String db : databasesSection.getKeys()) {
                String name;
                String dbName = databasesSection.getString(db + ".name");
                if (dbName != null && !dbName.equals("")) {
                    name = dbName;
                } else {
                    name = db;
                }
            
                String host = databasesSection.getString(db + ".host");
                String user = databasesSection.getString(db + ".user");
                String password = databasesSection.getString(db + ".password");
                boolean primary = false;
                if (databasesSection.contains(db + ".primary")) {
                    primary = databasesSection.getBoolean(db + ".primary");
                }
                Database database = new Database("mysql", name, host, user, password, primary);
                registry.register(database);
            }
        }
    }
    
    @Override
    public void registerStats(StatRegistry registry) {
        
    }
    
    @Override
    public void registerNetworkCommands(NetworkCommandRegistry registry) {
        registry.register(new NetworkCommand("punishment", (cmd, origin, args) -> ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            int id = Integer.parseInt(args[0]);
            Punishment punishment = null;
            try {
                punishment = NexusAPI.getApi().getPrimaryDatabase().get(Punishment.class, "id", id).get(0);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.BLACKLIST || punishment.getType() == PunishmentType.KICK) {
                NexusAPI.getApi().getPunishmentManager().addPunishment(punishment);
                UUID target = UUID.fromString(punishment.getTarget());
                ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(target);
                
                if (proxiedPlayer != null) {
                    String address = ((InetSocketAddress) proxiedPlayer.getSocketAddress()).getHostName();
                    NexusPlayer punishedPlayer = NexusAPI.getApi().getPlayerManager().getNexusPlayer(target);
                    
                    if (punishment.isActive() || punishment.getType() == PunishmentType.KICK) {
                        BaseComponent[] disconnectMsg = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', punishment.formatKick()));
                        if (punishedPlayer.getRank() == Rank.NEXUS) {
                            punishedPlayer.sendMessage("&6&l>> &cSomeone tried to " + punishment.getType().name().toLowerCase() + " you, but you are immune.");
                        } else {
                            proxiedPlayer.disconnect(disconnectMsg);
                            
                            if (punishment.getType() == PunishmentType.BLACKLIST) {
                                Set<UUID> uuids = NexusAPI.getApi().getPlayerManager().getPlayersByIp(address);
                                if (uuids != null && uuids.size() > 0) {
                                    for (UUID uuid : uuids) {
                                        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
                                        if (player != null) {
                                            player.disconnect(disconnectMsg);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })));
    
        registry.register(new NetworkCommand("removepunishment", (cmd, origin, args) -> {
            long id = Long.parseLong(args[0]);
            Punishment punishment = NexusAPI.getApi().getPunishmentManager().getPunishment(id);
            if (punishment != null) {
                try {
                    punishment = NexusAPI.getApi().getPrimaryDatabase().get(Punishment.class, "id", id).get(0);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                NexusAPI.getApi().getPunishmentManager().addPunishment(punishment);
            }
        }));
    }
    
    @Override
    public void registerToggles(ToggleRegistry registry) {
        
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return plugin.getConnection();
    }
    
    @Override
    public File getFolder() {
        return plugin.getDataFolder();
    }
}
