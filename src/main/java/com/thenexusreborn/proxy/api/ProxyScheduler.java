package com.thenexusreborn.proxy.api;

import com.starmediadev.starlib.scheduler.*;
import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

public class ProxyScheduler implements Scheduler {
    
    private NexusProxy plugin;
    private TaskScheduler scheduler = ProxyServer.getInstance().getScheduler();
    
    public ProxyScheduler(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Task runTask(Runnable runnable) {
        return new ProxyTask(scheduler.schedule(plugin, runnable, 0L, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public Task runTaskAsynchronously(Runnable runnable) {
        return new ProxyTask(scheduler.runAsync(plugin, runnable));
    }
    
    @Override
    public Task runTaskLater(Runnable runnable, long l) {
        return new ProxyTask(scheduler.schedule(plugin, runnable, l * 20, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public Task runTaskLaterAsynchronously(Runnable runnable, long l) {
        return runTaskLater(runnable, l);
    }
    
    @Override
    public Task runTaskTimer(Runnable runnable, long l, long l1) {
        return new ProxyTask(scheduler.schedule(plugin, runnable, l * 20, l1 * 20, TimeUnit.MILLISECONDS));
    }
    
    @Override
    public Task runTaskTimerAsynchronously(Runnable runnable, long l, long l1) {
        return runTaskTimer(runnable, l, l1);
    }
}
