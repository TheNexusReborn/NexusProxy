package com.thenexusreborn.proxy.cmds;

import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

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
        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            if (server.getName().toLowerCase().contains("hub")) {
                player.connect(server);
                player.sendMessage(new ComponentBuilder("Sent you to the hub").color(ChatColor.YELLOW).create());
                break;
            }
        }
    }
}
