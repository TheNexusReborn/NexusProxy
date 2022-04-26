package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class ProxyPlayerManager extends PlayerManager implements Listener {
    @Override
    public NexusPlayer createPlayerData(UUID uniqueId, String name) {
        NexusPlayer nexusPlayer = NexusAPI.getApi().getPlayerFactory().createPlayer(uniqueId, name);
        NexusAPI.getApi().getDataManager().pushPlayerAsync(nexusPlayer);
        return nexusPlayer;
    }
    
    @EventHandler
    public void onPostLogin(PostLoginEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (!getPlayers().containsKey(player.getUniqueId())) {
            NexusAPI.getApi().getThreadFactory().runAsync(() -> {
                NexusPlayer nexusPlayer;
                if (!hasData(player.getUniqueId())) {
                    nexusPlayer = createPlayerData(player.getUniqueId(), player.getName());
                } else {
                    nexusPlayer = getNexusPlayer(player.getUniqueId());
                }
                getPlayers().put(nexusPlayer.getUniqueId(), nexusPlayer);
            });
        }
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        NexusPlayer nexusPlayer = getPlayers().get(e.getPlayer().getUniqueId());
        if (nexusPlayer != null) {
            saveToMySQLAsync(nexusPlayer);
            nexusPlayer.setLastLogout(System.currentTimeMillis());
            this.players.remove(nexusPlayer.getUniqueId());
        }
    }
}
