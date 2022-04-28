package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.server.*;
import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;

import java.net.*;

public class ProxyServerManager extends ServerManager {
    
    private NexusProxy plugin;
    
    public ProxyServerManager(NexusProxy plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void setupCurrentServer() {
        ProxyServer proxyServer = ProxyServer.getInstance();
        String ip = "";
        int port = 0;
        int maxPlayers = 0;
        for (ListenerInfo listener : proxyServer.getConfig().getListeners()) {
            InetSocketAddress socketAddress = (InetSocketAddress) listener.getSocketAddress();
            ip = socketAddress.getHostName();
            port = socketAddress.getPort();
            maxPlayers = listener.getMaxPlayers();
            break;
        }
        int multicraftId = plugin.getConfig().getInt("serverinfo.multicraftid");
        String name = plugin.getConfig().getString("serverinfo.name");
        int players = ProxyServer.getInstance().getOnlineCount();
        int hiddenPlayers = 0;
        String type = plugin.getConfig().getString("serverinfo.type");
        String status = "loading";
        String state = "none";
        this.currentServer = new ServerInfo(multicraftId, ip, name, port, players, maxPlayers, hiddenPlayers, type, status, state);
        if (multicraftId > 0) {
            NexusAPI.getApi().getDataManager().pushServerInfoAsync(this.currentServer);
        }
    }
}
