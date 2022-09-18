package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.thread.ThreadFactory;
import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.ProxyServer;

import java.util.concurrent.TimeUnit;

public class ProxyThreadFactory extends ThreadFactory {
    
    private final NexusProxy plugin;
    
    public ProxyThreadFactory(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void runAsync(Runnable runnable) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable);
    }
    
    @Override
    public void runSync(Runnable runnable) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0L, TimeUnit.SECONDS);   
    }
}
