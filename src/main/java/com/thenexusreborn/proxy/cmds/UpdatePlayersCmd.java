package com.thenexusreborn.proxy.cmds;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.helper.MojangHelper;
import com.thenexusreborn.api.player.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.*;
import java.util.UUID;

public class UpdatePlayersCmd extends Command {
    public UpdatePlayersCmd() {
        super("updateplayers");
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
        
        //name and rank
        NexusAPI.getApi().getThreadFactory().runAsync(() -> {
            try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("select * from players;");
                while (resultSet.next()) {
                    String rawUUID = resultSet.getString("uuid");
                    UUID uuid = UUID.fromString(rawUUID);
                    NexusPlayer nexusPlayer = NexusAPI.getApi().getDataManager().loadPlayer(uuid);
                    nexusPlayer.setLastKnownName(MojangHelper.getNameFromUUID(uuid));
                    NexusAPI.getApi().getDataManager().pushPlayer(nexusPlayer);
                }
                sender.sendMessage(new ComponentBuilder("Players updated").color(ChatColor.GREEN).create());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
