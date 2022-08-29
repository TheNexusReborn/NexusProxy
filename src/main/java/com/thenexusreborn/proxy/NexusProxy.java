package com.thenexusreborn.proxy;

import com.thenexusreborn.api.*;
import com.thenexusreborn.api.migration.Migrator;
import com.thenexusreborn.api.server.ServerInfo;
import com.thenexusreborn.api.util.Version;
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
import java.util.logging.Level;

public class NexusProxy extends Plugin {
    private Configuration config;
    
    private MOTD motd;
    
    private Migrator migrator = new DataBackendMigrator();
    
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
    
        try {
            File lastMigrationFile = new File(getDataFolder(), "lastMigration.txt");
            Version previousVersion = null;
            if (lastMigrationFile.exists()) {
                try (FileInputStream fis = new FileInputStream(lastMigrationFile); BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                    previousVersion = new Version(reader.readLine());
                    getLogger().info("Found last migration version: " + previousVersion);
                }
            } else {
                getLogger().info("Could not find a last migration version.");
            }
    
            boolean migrationSuccess = false;
    
            if (migrator != null) {
                getLogger().info("Found a Migrator");
                int compareResult = NexusAPI.getApi().getVersion().compareTo(previousVersion);
                if (compareResult > 0) {
                    getLogger().info("Current version is higher than previous version.");
                    if (migrator.getTargetVersion().equals(NexusAPI.getApi().getVersion())) {
                        getLogger().info("Migrator version is for the current version");
                        migrationSuccess = migrator.migrate();
                        getLogger().info("Migration success: " + migrationSuccess);
                
                        if (!migrationSuccess) {
                            NexusAPI.logMessage(Level.INFO, "Error while processing migration", "Migrator Class: " + migrator.getClass().getName());
                        }
                    }
                }
            }
    
            if (migrator == null || migrationSuccess) {
                if (!lastMigrationFile.exists()) {
                    lastMigrationFile.createNewFile();
                }
                String version = NexusAPI.getApi().getVersion().getMajor() + "." + NexusAPI.getApi().getVersion().getMinor();
                if (NexusAPI.getApi().getVersion().getPatch() > 0) {
                    version += "." + NexusAPI.getApi().getVersion().getPatch();
                }
                version += "-" + NexusAPI.getApi().getVersion().getStage().name();
                try (FileOutputStream fos = new FileOutputStream(lastMigrationFile); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
                    writer.write(version);
                    writer.flush();
                }
                getLogger().info("Updated last migration version to the current version.");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            NexusAPI.getApi().getPrimaryDatabase().push(serverInfo);
        }, 1L, 1L, TimeUnit.SECONDS);
    }
    
    @Override
    public void onDisable() {
        config.set("motd.line1", this.motd.getLine1());
        config.set("motd.line2", this.motd.getLine2());
        saveConfig();
        
        NexusAPI.getApi().getNetworkManager().close();
    }
    
    public void saveConfig() {
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
