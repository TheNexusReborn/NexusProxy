package com.thenexusreborn.proxy.api.scheduler;

import com.starmediadev.starlib.scheduler.*;
import com.thenexusreborn.proxy.api.scheduler.ProxyTask;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class ProxyScheduler implements Scheduler {
    
    private Plugin plugin;
    
    public ProxyScheduler(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Task runTask(Runnable runnable) {
        return new ProxyTask(ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0L, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public Task runTaskAsynchronously(Runnable runnable) {
        return new ProxyTask(ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable));
    }
    
    @Override
    public Task runTaskLater(Runnable runnable, long l) {
        return new ProxyTask(ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0L, l * 50L, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public Task runTaskLaterAsynchronously(Runnable runnable, long l) {
        return runTaskLater(runnable, l * 50);
    }
    
    @Override
    public Task runTaskTimer(Runnable runnable, long l, long l1) {
        return new ProxyTask(ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, l * 50, l1 * 50, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public Task runTaskTimerAsynchronously(Runnable runnable, long l, long l1) {
        return runTaskTimer(runnable, l, l1);
    }
}
