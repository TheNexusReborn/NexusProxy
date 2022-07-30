package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.tags.Tag;
import com.thenexusreborn.proxy.NexusProxy;

import java.util.*;

public class ProxyPlayerFactory extends PlayerFactory {
    
    private final NexusProxy plugin;
    
    public ProxyPlayerFactory(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public NexusPlayer createPlayer(UUID uuid, Map<Rank, Long> ranks, long firstJoined, long lastLogin, long lastLogout, String lastKnownName, Tag tag, Set<String> unlockedTags, boolean prealpha, boolean alpha, boolean beta) {
        return new ProxyNexusPlayer(uuid, ranks, firstJoined, lastLogin, lastLogout, lastKnownName, tag, unlockedTags, prealpha, alpha, beta);
    }
    
    @Override
    public NexusPlayer createPlayer(UUID uniqueId, String name) {
        return new ProxyNexusPlayer(uniqueId, name);
    }
}
