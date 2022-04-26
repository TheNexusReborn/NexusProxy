package com.thenexusreborn.proxy;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.data.DataManager;
import com.thenexusreborn.api.tags.TagManager;
import com.thenexusreborn.proxy.api.*;

import java.sql.*;

public class BungeeNexusAPI extends NexusAPI {
    
    private NexusProxy plugin;
    
    public BungeeNexusAPI(NexusProxy plugin) {
        super(plugin.getLogger(), new DataManager(), new TagManager(), new ProxyPlayerManager(), new ProxyThreadFactory(plugin), new ProxyPlayerFactory(plugin));
        this.plugin = plugin;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return plugin.getConnection();
    }
}
