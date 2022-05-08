package com.thenexusreborn.proxy.cmds;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.helper.MojangHelper;
import com.thenexusreborn.api.player.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import javax.swing.plaf.metal.MetalCheckBoxUI;
import java.sql.*;
import java.util.UUID;

public class CreatePlayerCmd extends Command {
    public CreatePlayerCmd() {
        super("createplayer");
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
        
        if (!(args.length > 1)) {
            sender.sendMessage(new ComponentBuilder("You do not have enough arguments").color(ChatColor.RED).create());
            return;
        }
        
        //name and rank
        NexusAPI.getApi().getThreadFactory().runAsync(() -> {
            UUID uuid = MojangHelper.getUUIDFromName(args[0]);
            Rank rank = Rank.valueOf(args[1].toUpperCase());
            
            try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("select * from players where uuid='" + uuid.toString() + "';");
                if (resultSet.next()) {
                    sender.sendMessage(new ComponentBuilder("There is already a player with that name.").color(ChatColor.RED).create());
                    return;
                }
                
                NexusPlayer nexusPlayer = NexusAPI.getApi().getPlayerFactory().createPlayer(uuid, args[0]);
                nexusPlayer.setRank(rank, -1);
                NexusAPI.getApi().getDataManager().pushPlayer(nexusPlayer);
                sender.sendMessage(new ComponentBuilder("Created the player " + args[0]).color(ChatColor.YELLOW).create());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
