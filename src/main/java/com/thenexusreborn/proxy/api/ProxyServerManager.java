package com.thenexusreborn.proxy.api;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.server.*;
import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;

import java.net.*;
import java.util.*;

public class ProxyServerManager extends ServerManager {
    
    private final NexusProxy plugin;
    
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
        int multicraftId = plugin.getConfig().getInt("serverInfo.multicraftid");
        String name = plugin.getConfig().getString("serverInfo.name");
        int players = ProxyServer.getInstance().getOnlineCount();
        int hiddenPlayers = 0;
        String type = plugin.getConfig().getString("serverInfo.type");
        String status = "loading";
        String state = "none";
        this.currentServer = new ServerInfo(multicraftId, ip, name, port, players, maxPlayers, hiddenPlayers, type, status, state);
        long id = plugin.getConfig().getLong("serverInfo.id");
        NexusAPI.getApi().getDataManager().pushServerInfoAsync(this.currentServer);
        plugin.getConfig().set("serverInfo.id", this.currentServer.getId());
        plugin.saveConfig();
    }
    
    @Override
    public List<ServerInfo> getServers() {
        List<ServerInfo> servers = super.getServers();
        if (servers == null || servers.isEmpty()) {
            ProxyServer proxyServer = ProxyServer.getInstance();
            for (net.md_5.bungee.api.config.ServerInfo bungeeServer : proxyServer.getServers().values()) {
                InetSocketAddress address = (InetSocketAddress) bungeeServer.getSocketAddress();
                ServerInfo serverInfo = new ServerInfo(0, address.getHostName(), bungeeServer.getName(), address.getPort());
                servers.add(serverInfo);
            }
        }
        return servers;
    }
}
