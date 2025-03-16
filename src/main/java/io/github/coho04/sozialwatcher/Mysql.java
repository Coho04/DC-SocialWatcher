package io.github.coho04.sozialwatcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.sentry.Sentry;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class Mysql {

    private final HikariDataSource source;

    public Mysql() {
        this.source = getConfig();
        try (Connection connection = this.source.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("CREATE DATABASE IF NOT EXISTS `" + Main.getCustomConfig().getMysqlDatabase() + "`;");
            statement.execute("USE `" + Main.getCustomConfig().getMysqlDatabase() + "`;");
            statement.execute("CREATE TABLE IF NOT EXISTS discord_guild (id INT AUTO_INCREMENT NOT NULL PRIMARY KEY, guild_id MEDIUMTEXT NULL );");
            statement.execute("CREATE TABLE IF NOT EXISTS twitch_channel(id INT AUTO_INCREMENT NOT NULL  PRIMARY KEY, twitch_channel VARCHAR(255) NULL);");
            statement.execute("CREATE TABLE IF NOT EXISTS youtube_channel(id INT AUTO_INCREMENT NOT NULL  PRIMARY KEY, youtube_channel VARCHAR(255) NULL, last_video_uuid VARCHAR(255) NULL);");
            statement.execute("CREATE TABLE IF NOT EXISTS youtube_guild(id INT AUTO_INCREMENT NOT NULL  PRIMARY KEY, discord_guild_id INT NULL, youtube_channel_id INT NULL, discord_text_channel_id mediumtext NULL, FOREIGN KEY (discord_guild_id) REFERENCES discord_guild (id), FOREIGN KEY (youtube_channel_id) REFERENCES youtube_channel (id));");
            statement.execute("CREATE TABLE IF NOT EXISTS twitch_guilds(id INT AUTO_INCREMENT PRIMARY KEY, discord_guild_id INT NULL, twitch_channel_id INT NULL, discord_text_channel_id mediumtext NULL, discord_role_id mediumtext NULL, FOREIGN KEY (discord_guild_id) REFERENCES discord_guild (id), FOREIGN KEY (twitch_channel_id) REFERENCES twitch_channel (id));");

            checkAndCreateIndex(connection, "twitch_guilds", "discord_guild_id");
            checkAndCreateIndex(connection, "youtube_guild", "discord_guild_id");
            checkAndCreateIndex(connection, "twitch_guilds", "twitch_channel_id");
            checkAndCreateIndex(connection, "youtube_guild", "youtube_channel_id");

        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
        System.out.println("[MYSQL] Initialized MySQL!");
    }


    private void checkAndCreateIndex(Connection connection, String tableName, String indexName) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            boolean indexExists = false;
            while (resultSet.next()) {
                if (indexName.equals(resultSet.getString("INDEX_NAME"))) {
                    indexExists = true;
                    break;
                }
            }
            if (!indexExists) {
                statement.execute("CREATE INDEX " + indexName + " ON " + tableName + " (" + indexName + ");");
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
    }

    private static @NotNull HikariDataSource getConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + Main.getCustomConfig().getMysqlHostname() + ":" + Main.getCustomConfig().getMysqlPort());
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
        config.setInitializationFailTimeout(0);
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(60));
        config.setUsername(Main.getCustomConfig().getMysqlUsername());
        config.setPassword(Main.getCustomConfig().getMysqlPassword());
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    public HikariDataSource getSource() {
        return source;
    }
}