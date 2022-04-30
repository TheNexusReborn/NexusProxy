package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.tags.Tag;
import com.thenexusreborn.proxy.NexusProxy;

import java.util.*;

public class ProxyPlayerFactory extends PlayerFactory {
    
    private NexusProxy plugin;
    
    public ProxyPlayerFactory(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public NexusPlayer createPlayer(UUID uuid, Map<Rank, Long> ranks, long firstJoined, long lastLogin, long lastLogout, long playtime, String lastKnownName, Tag tag, Set<Tag> unlockedTags, int lastPlaytimeNotification) {
        return new ProxyNexusPlayer(uuid, ranks, firstJoined, lastLogin, lastLogout, playtime, lastKnownName, tag, unlockedTags, lastPlaytimeNotification);
    }
    
    @Override
    public NexusPlayer createPlayer(UUID uniqueId, String name) {
        return new ProxyNexusPlayer(uniqueId, name);
    }
}
