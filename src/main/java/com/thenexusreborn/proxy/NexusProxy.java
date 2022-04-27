package com.thenexusreborn.proxy;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.proxy.api.ProxyPlayerManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.*;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;

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
            getLogger().severe("Could not load the Nexus API");
            return;
        }
        
        getProxy().getPluginManager().registerListener(this, (ProxyPlayerManager) NexusAPI.getApi().getPlayerManager());
        
        getProxy().registerChannel("nexus");
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
