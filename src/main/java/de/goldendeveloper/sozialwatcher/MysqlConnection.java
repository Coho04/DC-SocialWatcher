package de.goldendeveloper.sozialwatcher;

import de.goldendeveloper.mysql.MYSQL;
import de.goldendeveloper.mysql.entities.Database;
import de.goldendeveloper.mysql.entities.Table;
import de.goldendeveloper.mysql.errors.ExceptionHandler;
import de.goldendeveloper.mysql.exceptions.NoConnectionException;
import de.goldendeveloper.sozialwatcher.errors.CustomExceptionHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MysqlConnection {

    private final MYSQL mysql;
    public static String dbName = "GD-SozialWatcher";
    public static String youtubeTableName = "Youtube";

    public static String twitchTableName = "Twitcher";

    public static String colmDcServer = "DiscordServer";
    public static String colmDcStreamNotifyChannel = "DcStreamNotifyChannelID";
    public static String colmDcStreamNotifyRole = "DcStreamNotifyRoleID";
    public static String colmTwitchChannel = "TwitchChannel";

    public MysqlConnection(String hostname, String username, String password, int port) throws NoConnectionException, SQLException {
        mysql = new MYSQL(hostname, username, password, port, new CustomExceptionHandler());
        if (!mysql.existsDatabase(dbName)) {
            mysql.createDatabase(dbName);
        }
        Database db = mysql.getDatabase(dbName);
        createTable(db, youtubeTableName, new String[]{"DiscordServerID", "DiscordChannelID", "YoutubeChannelID", "LastVideoID"});
        createTable(db, twitchTableName, new String[]{colmDcServer, colmDcStreamNotifyChannel, colmDcStreamNotifyRole, colmTwitchChannel});
        System.out.println("MYSQL Fertig");
    }

    private void createTable(Database db, String tableName, String[] columns) {
        if (!db.existsTable(tableName)) {
            db.createTable(tableName);
        }
        Table table = db.getTable(tableName);
        Arrays.stream(columns).forEach(column -> {
            if (!table.existsColumn(column))
                table.addColumn(column);
        });
    }

    public MYSQL getMysql() {
        return mysql;
    }
}