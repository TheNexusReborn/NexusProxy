package com.thenexusreborn.proxy;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.punishment.*;
import com.thenexusreborn.api.server.ServerInfo;
import com.thenexusreborn.api.tags.Tag;
import com.thenexusreborn.proxy.api.ProxyPlayerManager;
import com.thenexusreborn.proxy.cmds.*;
import com.thenexusreborn.proxy.listener.ServerPingListener;
import com.thenexusreborn.proxy.settings.MOTD;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NexusProxy extends Plugin {
    private Configuration config;
    
    private MOTD motd;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().severe("Could not load config file.");
            return;
        }
        
        NexusAPI.setApi(new BungeeNexusAPI(this));
        try {
            NexusAPI.getApi().init();
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Could not load the Nexus API");
            return;
        }
        
        if (config.contains("motd")) {
            String line1 = config.getString("motd.line1");
            String line2 = config.getString("motd.line2");
            this.motd = new MOTD(line1, line2);
        } else {
            this.motd = new MOTD("&d&lThe Nexus Reborn", "");
        }
        
        getProxy().getPluginManager().registerListener(this, (ProxyPlayerManager) NexusAPI.getApi().getPlayerManager());
        getProxy().getPluginManager().registerListener(this, new ServerPingListener(this));
        
        getProxy().registerChannel("nexus");
        
        getProxy().getPluginManager().registerCommand(this, new NetworkCmd(this));
        getProxy().getPluginManager().registerCommand(this, new HubCommand());
        getProxy().getPluginManager().registerCommand(this, new CreatePlayerCmd());
        getProxy().getPluginManager().registerCommand(this, new UpdatePlayersCmd());
        
        getProxy().getScheduler().schedule(this, () -> {
            PlayerManager playerManager = NexusAPI.getApi().getPlayerManager();
            for (NexusPlayer player : new ArrayList<>(playerManager.getPlayers().values())) {
                try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("select ranks, unlockedTags, tag from players where uuid='" + player.getUniqueId() + "';");
                    if (resultSet.next()) {
                        String rawRanks = resultSet.getString("ranks");
                        Map<Rank, Long> ranks = NexusAPI.getApi().getDataManager().parseRanks(rawRanks);
                        player.setRanks(ranks);
                        String rawTags = resultSet.getString("unlockedTags");
                        Set<Tag> tags = NexusAPI.getApi().getDataManager().parseTags(rawTags);
                        player.setUnlockedTags(tags);
                        player.setTag(new Tag(resultSet.getString("tag")));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);
        
        getProxy().getScheduler().schedule(this, () -> {
            ServerInfo serverInfo = NexusAPI.getApi().getServerManager().getCurrentServer();
            serverInfo.setStatus("online");
            serverInfo.setPlayers(getProxy().getOnlineCount());
            NexusAPI.getApi().getDataManager().pushServerInfo(serverInfo);
        }, 1L, 1L, TimeUnit.SECONDS);
        
        NexusAPI.getApi().getNetworkManager().getCommand("punishment").setExecutor((cmd, origin, args) -> getProxy().getScheduler().runAsync(this, () -> {
            int id = Integer.parseInt(args[0]);
            Punishment punishment = NexusAPI.getApi().getDataManager().getPunishment(id);
            if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.BLACKLIST || punishment.getType() == PunishmentType.KICK) {
                NexusAPI.getApi().getPunishmentManager().addPunishment(punishment);
                UUID target = UUID.fromString(punishment.getTarget());
                ProxiedPlayer proxiedPlayer = getProxy().getPlayer(target);
                
                if (proxiedPlayer != null) {
                    String address = ((InetSocketAddress) proxiedPlayer.getSocketAddress()).getHostName();
                    NexusPlayer punishedPlayer = NexusAPI.getApi().getPlayerManager().getNexusPlayer(target);
                    
                    if (punishment.isActive() || punishment.getType() == PunishmentType.KICK) {
                        BaseComponent[] disconnectMsg = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', punishment.formatKick()));
                        if (punishedPlayer.getRank() == Rank.NEXUS) {
                            punishedPlayer.sendMessage("&6&l>> &cSomeone tried to " + punishment.getType().name().toLowerCase() + " but you are immune.");
                        } else {
                            proxiedPlayer.disconnect(disconnectMsg);
                            
                            if (punishment.getType() == PunishmentType.BLACKLIST) {
                                Set<UUID> uuids = NexusAPI.getApi().getPlayerManager().getIpHistory().get(address);
                                if (uuids != null && uuids.size() > 0) {
                                    for (UUID uuid : uuids) {
                                        ProxiedPlayer player = getProxy().getPlayer(uuid);
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
        }));
    }
    
    @Override
    public void onDisable() {
        config.set("motd.line1", this.motd.getLine1());
        config.set("motd.line2", this.motd.getLine2());
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        NexusAPI.getApi().getNetworkManager().close();
    }
    
    public void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public Configuration getConfig() {
        return config;
    }
    
    public Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + getConfig().getString("mysql.host") + "/" + getConfig().getString("mysql.database") + "?user=" + getConfig().getString("mysql.user") + "&password=" + getConfig().getString("mysql.password");
        return DriverManager.getConnection(url);
    }
    
    public MOTD getMotd() {
        return motd;
    }
}
