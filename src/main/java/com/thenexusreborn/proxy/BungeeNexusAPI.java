package com.thenexusreborn.proxy;

import com.thenexusreborn.api.*;
import com.thenexusreborn.api.data.DataManager;
import com.thenexusreborn.api.networking.SocketContext;
import com.thenexusreborn.proxy.api.*;

import java.sql.*;

public class BungeeNexusAPI extends NexusAPI {
    
    private NexusProxy plugin;
    
    public BungeeNexusAPI(NexusProxy plugin) {
        super(Environment.valueOf(plugin.getConfig().getString("environment")), plugin.getLogger(), new DataManager(), new ProxyPlayerManager(), new ProxyThreadFactory(plugin), new ProxyPlayerFactory(plugin), new ProxyServerManager(plugin), SocketContext.SERVER);
        this.plugin = plugin;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return plugin.getConnection();
    }
}
