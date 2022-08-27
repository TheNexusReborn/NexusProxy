package com.thenexusreborn.proxy;

import com.thenexusreborn.api.NexusAPI;
import com.thenexusreborn.api.data.DataManager;
import com.thenexusreborn.api.data.codec.*;
import com.thenexusreborn.api.data.objects.Database;
import com.thenexusreborn.api.gamearchive.*;
import com.thenexusreborn.api.migration.Migrator;
import com.thenexusreborn.api.player.*;
import com.thenexusreborn.api.punishment.*;
import com.thenexusreborn.api.registry.StatRegistry;
import com.thenexusreborn.api.server.ServerInfo;
import com.thenexusreborn.api.stats.*;
import com.thenexusreborn.api.tags.Tag;
import com.thenexusreborn.api.tournament.Tournament;
import com.thenexusreborn.api.util.Version;
import com.thenexusreborn.api.util.Version.Stage;

import java.sql.*;
import java.util.*;

public class DataBackendMigrator extends Migrator {
    public DataBackendMigrator() {
        super(new Version(1, 5, 0, Stage.ALPHA), null);
    }
    
    @Override
    public boolean migrate() {
        NexusAPI.getApi().getLogger().info("Starting Data Migration for version " + this.targetVersion);
        DataManager dataManager = NexusAPI.getApi().getDataManager();
        Database database = NexusAPI.getApi().getPrimaryDatabase();
        
        NexusAPI.getApi().getLogger().info("Converting IP Data");
        Set<IPEntry> ipHistory = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from iphistory;");
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                ipHistory.add(new IPEntry(resultSet.getString("ip"), uuid));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

//        NexusAPI.getApi().getLogger().info("Converting StatInfo Data");
//        List<Stat.Info> statInfos = new ArrayList<>();
//        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
//            ResultSet statResultSet = statement.executeQuery("select * from statinfo;");
//            while (statResultSet.next()) {
//                String name = StatHelper.formatStatName(statResultSet.getString("name"));
//                StatType type = StatType.valueOf(statResultSet.getString("type"));
//                Object defaultValue = StatHelper.parseValue(type, statResultSet.getString("defaultValue"));
//                Stat.Info statInfo = new Stat.Info(name, type, defaultValue);
//                statInfos.add(statInfo);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
        
        NexusAPI.getApi().getLogger().info("Converting Stat Data");
    
        StatRegistry registry = StatHelper.getRegistry();
        registry.register("sg_score", StatType.INTEGER, 100);
        registry.register("sg_kills", StatType.INTEGER, 0);
        registry.register("sg_highest_kill_streak", StatType.INTEGER, 0);
        registry.register("sg_games", StatType.INTEGER, 0);
        registry.register("sg_wins", StatType.INTEGER, 0);
        registry.register("sg_win_streak", StatType.INTEGER, 0);
        registry.register("sg_deaths", StatType.INTEGER, 0);
        registry.register("sg_deathmatches_reached", StatType.INTEGER, 0);
        registry.register("sg_chests_looted", StatType.INTEGER, 0);
        registry.register("sg_assists", StatType.INTEGER, 0);
        registry.register("sg_times_mutated", StatType.INTEGER, 0);
        registry.register("sg_mutation_kills", StatType.INTEGER, 0);
        registry.register("sg_mutation_deaths", StatType.INTEGER, 0);
        registry.register("sg_mutation_passes", StatType.INTEGER, 0);
        registry.register("sg_sponsored_others", StatType.INTEGER, 0);
        registry.register("sg_sponsors_received", StatType.INTEGER, 0);
        registry.register("sg_tournament_points", StatType.INTEGER, 0);
        registry.register("sg_tournament_kills", StatType.INTEGER, 0);
        registry.register("sg_tournament_wins", StatType.INTEGER, 0);
        registry.register("sg_tournament_survives", StatType.INTEGER, 0);
        registry.register("sg_tournament_chests_looted", StatType.INTEGER, 0);
        registry.register("sg_tournament_assists", StatType.INTEGER, 0);
        
        Set<Stat> stats = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from stats");
            while (resultSet.next()) {
                String name = StatHelper.formatStatName(resultSet.getString("name"));
                if (name.equals("sg_sponsor_received")) {
                    name = "sg_sponsors_received";
                }
                StatType type;
                Stat.Info info = StatHelper.getInfo(name);
                if (info == null) {
                    info = new Stat.Info(name, StatType.DOUBLE, StatType.DOUBLE.getDefaultValue());
                }
                
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                try {
                    type = StatType.valueOf(resultSet.getString("type"));
                } catch (Exception e) {
                    if (info == null) {
                        continue;
                    }
                    type = info.getType();
                }
                Object value = StatHelper.parseValue(type, resultSet.getString("value"));
                long created = Long.parseLong(resultSet.getString("created"));
                long modified = Long.parseLong(resultSet.getString("modified"));
                
                Stat stat = new Stat(info, 0, uuid, value, created, modified);
                stats.add(stat);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

//        NexusAPI.getApi().getLogger().info("Converting StatChanges Data");
//        List<StatChange> statChanges = new ArrayList<>();
//        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
//            ResultSet statChangesResultSet = statement.executeQuery("select * from statchanges;");
//            while (statChangesResultSet.next()) {
//                int id = statChangesResultSet.getInt("id");
//                String name = statChangesResultSet.getString("statName");
//                StatOperator operator = StatOperator.valueOf(statChangesResultSet.getString("operator"));
//                long timestamp = Long.parseLong(statChangesResultSet.getString("timestamp"));
//                UUID uuid = UUID.fromString(statChangesResultSet.getString("uuid"));
//                StatType type = StatType.DOUBLE;
//                Object value = StatHelper.parseValue(type, statChangesResultSet.getString("value"));
//                
//                StatChange statChange = new StatChange(StatHelper.getInfo(name), id, uuid, value, operator, timestamp);
//                statChanges.add(statChange);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
        
        NexusAPI.getApi().getLogger().info("Converting PreferenceInfo Data");
        Set<Preference.Info> preferenceInfos = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from preferenceinfo;");
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String displayName = resultSet.getString("displayName");
                String description = resultSet.getString("description");
                boolean defaultValue = Boolean.parseBoolean("defaultValue");
                Preference.Info preferenceInfo = new Preference.Info(name, displayName, description, defaultValue);
                preferenceInfos.add(preferenceInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Converting Preference Data");
        Set<Preference> preferences = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from playerpreferences;");
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                boolean value = Boolean.parseBoolean(resultSet.getString("value"));
                UUID player = UUID.fromString(resultSet.getString("uuid"));
                
                Preference.Info info = null;
                for (Preference.Info object : preferenceInfos) {
                    if (object.getName().equalsIgnoreCase(name)) {
                        info = object;
                    }
                }
                
                if (info == null) {
                    continue;
                }
                
                Preference preference = new Preference(info, player, 0, value);
                preferences.add(preference);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Converting Player Data");
        Set<NexusPlayer> players = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from players;");
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                long firstJoined, lastLogin, lastLogout, playtime = 0;
                String lastKnownName, rawRanks;
                Map<Rank, Long> ranks;
                Tag tag = null;
                Set<String> unlockedTags = new HashSet<>();
                boolean prealpha, alpha, beta;
                firstJoined = Long.parseLong(resultSet.getString("firstJoined"));
                lastLogin = Long.parseLong(resultSet.getString("lastLogin"));
                lastKnownName = resultSet.getString("lastKnownName");
                ranks = new RanksCodec().decode(resultSet.getString("ranks"));
                
                String rawTag = resultSet.getString("tag");
                if (rawTag != null && !rawTag.equalsIgnoreCase("null")) {
                    tag = new Tag(rawTag);
                }
                lastLogout = Long.parseLong(resultSet.getString("lastLogout"));
                
                String rawUnlockedTags = resultSet.getString("unlockedTags");
                if (rawUnlockedTags != null && !rawUnlockedTags.equals("")) {
                    String[] split = rawUnlockedTags.split(",");
                    unlockedTags.addAll(Arrays.asList(split));
                }
                
                prealpha = Boolean.parseBoolean(resultSet.getString("prealpha"));
                alpha = Boolean.parseBoolean(resultSet.getString("alpha"));
                beta = Boolean.parseBoolean(resultSet.getString("beta"));
                
                NexusPlayer player = new NexusPlayer(uuid, lastKnownName);
                player.setFirstJoined(firstJoined);
                player.setLastLogin(lastLogin);
                player.setLastLogout(lastLogout);
                player.setTag(tag);
                player.setUnlockedTags(unlockedTags);
                player.setPrealpha(prealpha);
                player.setAlpha(alpha);
                player.setBeta(beta);
                ranks.forEach(player::addRank);
                
//                for (Preference preference : preferences) {
//                    if (preference.getUuid().equals(uuid)) {
//                        player.addPreference(preference);
//                    }
//                }
//                for (Stat stat : stats) {
//                    if (stat.getUuid().equals(uuid)) {
//                        player.addStat(stat);
//                    }
//                }
                
                players.add(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Converting ServerInfo Data");
        Set<ServerInfo> serverInfos = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from serverinfo;");
            while (resultSet.next()) {
                String ip = resultSet.getString("ip");
                String name = resultSet.getString("name");
                int port = resultSet.getInt("port");
                int serverPlayers = resultSet.getInt("players");
                int maxPlayers = resultSet.getInt("maxPlayers");
                int hiddenPlayers = resultSet.getInt("hiddenPlayers");
                int multicraftId = resultSet.getInt("multicraftId");
                String type = resultSet.getString("type");
                String status = resultSet.getString("status");
                String state = resultSet.getString("state");
                
                serverInfos.add(new ServerInfo(multicraftId, ip, name, port, serverPlayers, maxPlayers, hiddenPlayers, type, status, state));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Converting Game Data");
        Set<GameInfo> games = new TreeSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement infoStatement = connection.createStatement(); Statement actionStatement = connection.createStatement()) {
            ResultSet resultSet = infoStatement.executeQuery("select * from games;");
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                long gameStart = resultSet.getLong("start");
                long gameEnd = resultSet.getLong("end");
                String serverName = resultSet.getString("serverName");
                String[] gamePlayers = resultSet.getString("players").split(",");
                String winner = resultSet.getString("winner");
                String mapName = resultSet.getString("mapName");
                String settings = resultSet.getString("settings");
                String firstBlood = resultSet.getString("firstBlood");
                int playerCount = resultSet.getInt("playerCount");
                long length = resultSet.getLong("length");
                GameInfo gameInfo = new GameInfo(id, gameStart, gameEnd, serverName, gamePlayers, winner, mapName, settings, firstBlood, playerCount, length);
                games.add(gameInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Converting GameAction Data");
        Set<GameAction> gameActions = new HashSet<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet actionSet = statement.executeQuery("select * from gameactions;");
            while (actionSet.next()) {
                long timestamp = actionSet.getLong("timestamp");
                String type = actionSet.getString("type");
                String value = actionSet.getString("value");
                long gameId = actionSet.getLong("gameId");
                GameAction gameAction = new GameAction(gameId, timestamp, type, value);
                gameActions.add(gameAction);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("A total of " + gameActions.size() + " GameActions were found.");
        
        NexusAPI.getApi().getLogger().info("Adding Game Actions to the Games");
        actionLoop:
        for (GameAction action : gameActions) {
            for (GameInfo game : games) {
                if (game.getId() == action.getGameId()) {
                    game.getActions().add(action);
                    continue actionLoop;
                }
            }
        }
        
        NexusAPI.getApi().getLogger().info("Resetting Game IDs (To ensure that it works as intended)");
        games.forEach(gameInfo -> gameInfo.setId(0));
        
        NexusAPI.getApi().getLogger().info("Converting Punishment Data");
        List<Punishment> punishments = new ArrayList<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from punishments;");
            while (resultSet.next()) {
                long date = Long.parseLong(resultSet.getString("date"));
                long length = Long.parseLong(resultSet.getString("length"));
                String actor = resultSet.getString("actor");
                String target = resultSet.getString("target");
                String server = resultSet.getString("server");
                String reason = resultSet.getString("reason");
                PunishmentType type = PunishmentType.valueOf(resultSet.getString("type"));
                Visibility visibility = Visibility.valueOf(resultSet.getString("visibility"));
                PardonInfo pardonInfo = new PardonInfoCodec().decode(resultSet.getString("pardonInfo"));
                AcknowledgeInfo acknowledgeInfo = new AcknowledgeInfoCodec().decode(resultSet.getString("acknowledgeInfo"));
                Punishment punishment = new Punishment(date, length, actor, target, server, reason, type, visibility);
                punishment.setId(0);
                punishment.setPardonInfo(pardonInfo);
                punishment.setAcknowledgeInfo(acknowledgeInfo);
                punishments.add(punishment);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Converting Tournament Data");
        List<Tournament> tournaments = new ArrayList<>();
        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select * from tournaments;");
            while (resultSet.next()) {
                UUID host = UUID.fromString(resultSet.getString("host"));
                String name = resultSet.getString("name");
                boolean active = Boolean.parseBoolean(resultSet.getString("active"));
                int pointsPerKill = resultSet.getInt("pointsperkill");
                int pointsPerWin = resultSet.getInt("pointsperwin");
                int pointsPerSurvival = resultSet.getInt("pointspersurvival");
                String rawServers = resultSet.getString("servers");
                Tournament tournament = new Tournament(host, name);
                tournament.setId(0);
                tournament.setActive(active);
                tournament.setPointsPerKill(pointsPerKill);
                tournament.setPointsPerWin(pointsPerWin);
                tournament.setPointsPerSurvival(pointsPerSurvival);
                if (rawServers != null && !rawServers.equals("")) {
                    String[] servers = rawServers.split(",");
                    tournament.setServers(servers);
                } else {
                    tournament.setServers(new String[0]);
                }
                tournaments.add(tournament);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

//        NexusAPI.getApi().getLogger().info("Deleting old tables");
//        try (Connection connection = NexusAPI.getApi().getConnection(); Statement statement = connection.createStatement()) {
//            statement.execute("drop table if exists gameactions,games,iphistory,playerpreferences,players,preferenceinfo,punishments,serverinfo,stats,statchanges,tournaments;");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
        
        NexusAPI.getApi().getLogger().info("Creating New Tables");
        try {
            NexusAPI.getApi().getIOManager().setup();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        NexusAPI.getApi().getLogger().info("Saving Migrated Data");
        
        NexusAPI.getApi().getLogger().info("Saving IP Data: " + ipHistory.size());
        for (IPEntry ipEntry : ipHistory) {
            database.push(ipEntry);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Stat Data: " + stats.size());
        for (Stat stat : stats) {
            database.push(stat);
        }
        
        NexusAPI.getApi().getLogger().info("Saving PreferenceInfo Data: " + preferenceInfos.size());
        for (Preference.Info preferenceInfo : preferenceInfos) {
            database.push(preferenceInfo);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Preference Data: " + preferences.size());
        for (Preference preference : preferences) {
            database.push(preference);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Player Data: " + players.size());
        for (NexusPlayer player : players) {
            database.push(player);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Server Data: " + serverInfos.size());
        for (ServerInfo serverInfo : serverInfos) {
            database.push(serverInfo);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Game Data: " + games.size());
        for (GameInfo game : games) {
            database.push(game);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Punishment Data: " + punishments.size());
        for (Punishment punishment : punishments) {
            database.push(punishment);
        }
        
        NexusAPI.getApi().getLogger().info("Saving Tournament Data: " + tournaments.size());
        for (Tournament tournament : tournaments) {
            database.push(tournament);
        }
        
        NexusAPI.getApi().getLogger().info("Success");
        return true;
    }
}
