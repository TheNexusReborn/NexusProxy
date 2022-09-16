package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.*;
import com.thenexusreborn.proxy.NexusProxy;

public class ProxyPlayerFactory extends PlayerFactory {
    
    private final NexusProxy plugin;
    
    public ProxyPlayerFactory(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public PlayerProxy createProxy(NexusPlayer player) {
        return new ProxyPlayerProxy(player.getUniqueId());
    }
}
