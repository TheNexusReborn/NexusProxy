package com.thenexusreborn.proxy.api.scheduler;

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
        return true;
    }
    
    @Override
    public boolean cancel() {
        task.cancel();
        return true;
    }
}
