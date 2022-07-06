package com.thenexusreborn.proxy;

import com.thenexusreborn.api.*;
import com.thenexusreborn.api.network.NetworkContext;
import com.thenexusreborn.api.registry.*;
import com.thenexusreborn.proxy.api.*;

import java.sql.*;

public class BungeeNexusAPI extends NexusAPI {
    
    private NexusProxy plugin;
    
    public BungeeNexusAPI(NexusProxy plugin) {
        super(Environment.valueOf(plugin.getConfig().getString("environment")), NetworkContext.SERVER, plugin.getLogger(), new ProxyPlayerManager(), new ProxyThreadFactory(plugin), new ProxyPlayerFactory(plugin), new ProxyServerManager(plugin));
        this.plugin = plugin;
    }
    
    @Override
    public void registerDatabases(DatabaseRegistry registry) {
        
    }
    
    @Override
    public void registerStats(StatRegistry registry) {
        
    }
    
    @Override
    public void registerNetworkCommands(NetworkCommandRegistry registry) {
        
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return plugin.getConnection();
    }
}
