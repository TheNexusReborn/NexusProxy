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
    public void execute(CommandSender sender, String[] strings) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only players may use that command.").color(ChatColor.RED).create());
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) sender;
        List<com.thenexusreborn.api.server.ServerInfo> hubServers = new ArrayList<>();
        for (com.thenexusreborn.api.server.ServerInfo server : NexusAPI.getApi().getServerManager().getServers()) {
            if (server.getType().equalsIgnoreCase("hub")) {
                hubServers.add(server);
            }
        }
        
        findHub(player, hubServers);
    }
    
    private void findHub(ProxiedPlayer player, List<ServerInfo> hubServers) {
        for (ServerInfo hubServer : hubServers) {
            if (hubServer.getState().equalsIgnoreCase("online")) {
                net.md_5.bungee.api.config.ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(hubServer.getName());
                player.connect(serverInfo, (status, throwable) -> {
                    if (!status) {
                        findHub(player, hubServers);
                    } else {
                        player.sendMessage(new ComponentBuilder("Sent you to the hub").color(ChatColor.YELLOW).create());
                    }
                });
            }
        }
    }
}
