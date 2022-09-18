package com.thenexusreborn.proxy.cmds;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.server.ServerInfo;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.*;

public class HubCommand extends Command {
    public HubCommand() {
        super("hub");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only players may use that command.").color(ChatColor.RED).create());
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) sender;
        List<com.thenexusreborn.api.server.ServerInfo> hubServers = NexusAPI.getApi().getServerManager().getServersByType("hub");
    
        if (hubServers.size() == 0) {
            player.sendMessage(new ComponentBuilder("Could not find a hub server").color(ChatColor.RED).create());
            return;
        }
        findHub(player, hubServers);
    }
    
    private void findHub(ProxiedPlayer player, List<ServerInfo> hubServers) {
        for (ServerInfo hubServer : hubServers) {
            if (hubServer.getStatus().equalsIgnoreCase("online")) {
                net.md_5.bungee.api.config.ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(hubServer.getName());
                player.connect(serverInfo, (status, throwable) -> {
                    if (!status) {
                        List<ServerInfo> hubs = new ArrayList<>(hubServers);
                        hubs.remove(hubServer);
                        if (hubs.size() == 0) {
                            player.sendMessage(new ComponentBuilder("Could not find a hub server").color(ChatColor.RED).create());
                            return;
                        }
                        findHub(player, hubs);
                    } else {
                        player.sendMessage(new ComponentBuilder("Sent you to the hub").color(ChatColor.YELLOW).create());
                    }
                });
            }
        }
    }
}
