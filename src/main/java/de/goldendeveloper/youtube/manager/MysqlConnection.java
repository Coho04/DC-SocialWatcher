package de.goldendeveloper.youtube.manager;

import de.goldendeveloper.mysql.MYSQL;
import de.goldendeveloper.mysql.entities.Database;
import de.goldendeveloper.mysql.entities.Table;
import de.goldendeveloper.youtube.manager.errors.CustomExceptionHandler;

import java.util.Arrays;

public class MysqlConnection {

    private final MYSQL mysql;
    public static String dbName = "gd_sozialwatcher";
    public static String youtubeTableName = "Youtube";

    public MysqlConnection(String hostname, String username, String password, int port) {
        mysql = new MYSQL(hostname, username, password, port, new CustomExceptionHandler());
        if (!mysql.existsDatabase(dbName)) {
            mysql.createDatabase(dbName);
        }
        Database db = mysql.getDatabase(dbName);
        createTable(db, youtubeTableName, new String[]{"DiscordServerID", "DiscordChannelID", "YoutubeChannelID", "LastVideoID"});
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