package com.thenexusreborn.proxy.api;

import com.starmediadev.starlib.scheduler.Task;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class ProxyTask implements Task {
    private ScheduledTask task;
    
    public ProxyTask(ScheduledTask task) {
        this.task = task;
    }
    
    @Override
    public int getTaskId() {
        return task.getId();
    }
    
    @Override
    public boolean isSync() {
        return false;
    }
    
    @Override
    public boolean cancel() {
         this.task.cancel();
         return true;
    }
}
