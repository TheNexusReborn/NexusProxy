package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.stats.StatRegistry;
import com.thenexusreborn.api.util.Operator;
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
                    nexusPlayer = NexusAPI.getApi().getDataManager().loadPlayer(player.getUniqueId());
                }
                if (nexusPlayer.getFirstJoined() == 0) {
                    nexusPlayer.setFirstJoined(System.currentTimeMillis());
                }
                nexusPlayer.setLastLogin(System.currentTimeMillis());
    
                for (String statName : StatRegistry.getStats()) {
                    if (!nexusPlayer.hasStat(statName)) {
                        nexusPlayer.changeStat(statName, StatRegistry.getDefaultValue(statName), Operator.ADD);
                    }
                }
                
                getPlayers().put(nexusPlayer.getUniqueId(), nexusPlayer);
            });
        }
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        NexusPlayer nexusPlayer = getPlayers().get(e.getPlayer().getUniqueId());
        if (nexusPlayer != null) {
            NexusAPI.getApi().getThreadFactory().runAsync(() -> {
                nexusPlayer.setLastLogout(System.currentTimeMillis());
                long playTime = nexusPlayer.getLastLogout() - nexusPlayer.getLastLogin();
                nexusPlayer.setPlayTime(nexusPlayer.getPlayTime() + (playTime / 50));
                NexusAPI.getApi().getDataManager().refreshPlayerStats(nexusPlayer);
                nexusPlayer.consolodateStats();
                saveToMySQLAsync(nexusPlayer);
            });
            this.players.remove(nexusPlayer.getUniqueId());
        }
    }
}
