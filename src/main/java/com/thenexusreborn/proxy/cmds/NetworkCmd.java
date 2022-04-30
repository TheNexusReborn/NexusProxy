package com.thenexusreborn.proxy.cmds;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.proxy.NexusProxy;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class NetworkCmd extends Command {
    private final NexusProxy plugin;
    
    public NetworkCmd(NexusProxy plugin) {
        super("network");
        this.plugin = plugin;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        Rank senderRank;
        if (!(sender instanceof ProxiedPlayer)) {
            senderRank = Rank.ADMIN;
        } else {
            NexusPlayer nexusPlayer = NexusAPI.getApi().getPlayerManager().getNexusPlayer(((ProxiedPlayer) sender).getUniqueId());
            senderRank = nexusPlayer.getRank();
        }
        
        if (senderRank.ordinal() > Rank.ADMIN.ordinal()) {
            sender.sendMessage(new ComponentBuilder("You do not have permission to use that command.").color(ChatColor.RED).create());
            return;
        }
        
        if (!(args.length > 0)) {
            sender.sendMessage(new ComponentBuilder("You do not have enough arguments").color(ChatColor.RED).create());
            return;
        }
        
        if (args[0].equalsIgnoreCase("motd")) {
            if (!(args.length > 2)) {
                sender.sendMessage(new ComponentBuilder("You must provide a line number and the text").color(ChatColor.RED).create());
                return;
            }
            
            int line = Integer.parseInt(args[1]);
            StringBuilder textBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                textBuilder.append(args[i]).append(" ");
            }
            
            String text = textBuilder.substring(0, textBuilder.length() - 1);
            if (line == 1) {
                plugin.getMotd().setLine1(text);
            } else if (line == 2) {
                plugin.getMotd().setLine2(text);
            } else {
                sender.sendMessage(new ComponentBuilder("Invalid line number, only 1 and 2 is supported").color(ChatColor.RED).create());
                return;
            }
            
            sender.sendMessage(new ComponentBuilder("You set the motd line ").color(ChatColor.YELLOW).append(line + "").color(ChatColor.AQUA).append(" to ").color(ChatColor.YELLOW)
                    .append(TextComponent.fromLegacyText(text)).create());
        }
    }
}
