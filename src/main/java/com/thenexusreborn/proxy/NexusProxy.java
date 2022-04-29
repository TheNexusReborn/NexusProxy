package com.thenexusreborn.proxy;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.tags.Tag;
import com.thenexusreborn.proxy.api.ProxyPlayerManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.*;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NexusProxy extends Plugin {
    private Configuration config;
    
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
        
        getProxy().getPluginManager().registerListener(this, (ProxyPlayerManager) NexusAPI.getApi().getPlayerManager());
        
        getProxy().registerChannel("nexus");
        
        getProxy().getScheduler().schedule(this, () -> {
            PlayerManager playerManager = NexusAPI.getApi().getPlayerManager();
            for (NexusPlayer player : playerManager.getPlayers().values()) {
                try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("select ranks, unlockedTags, tag from players where uuid='" + player.getUniqueId() + "';");
                    if (resultSet.next()) {
                        String rawRanks = resultSet.getString("ranks");
                        Map<Rank, Long> ranks = NexusAPI.getApi().getDataManager().parseRanks(rawRanks);
                        player.setRanks(ranks);
                        String rawTags = resultSet.getString("unlockedTags");
                        Set<Tag> tags = NexusAPI.getApi().getDataManager().parseTags(rawTags);
                        player.setUnlockedTags(tags);
                        player.setTag(NexusAPI.getApi().getTagManager().getTag(resultSet.getString("tag")));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);
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
}
