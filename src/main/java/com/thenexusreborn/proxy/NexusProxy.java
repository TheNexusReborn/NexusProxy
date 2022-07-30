package com.thenexusreborn.proxy;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.server.ServerInfo;
import com.thenexusreborn.proxy.api.ProxyPlayerManager;
import com.thenexusreborn.proxy.cmds.*;
import com.thenexusreborn.proxy.listener.ServerPingListener;
import com.thenexusreborn.proxy.settings.MOTD;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.*;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
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
        
        getProxy().getScheduler().schedule(this, () -> {
            ServerInfo serverInfo = NexusAPI.getApi().getServerManager().getCurrentServer();
            serverInfo.setStatus("online");
            serverInfo.setPlayers(getProxy().getOnlineCount());
            NexusAPI.getApi().getDataManager().pushServerInfo(serverInfo);
        }, 1L, 1L, TimeUnit.SECONDS);
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
