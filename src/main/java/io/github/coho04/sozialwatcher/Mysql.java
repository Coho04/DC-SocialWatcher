package io.github.coho04.sozialwatcher;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.sentry.Sentry;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class Mysql {

    private final HikariDataSource source;

    public Mysql()  {
        this.source = getConfig();
        try {
            Statement statement = this.source.getConnection().createStatement();
            statement.execute("CREATE DATABASE IF NOT EXISTS `sozial_watcher_db`;");
            statement.execute("USE `sozial_watcher_db`;");
            statement.execute("CREATE TABLE IF NOT EXISTS discord_guild (id INT AUTO_INCREMENT NOT NULL PRIMARY KEY, guild_id MEDIUMTEXT NULL );");
            statement.execute("CREATE TABLE IF NOT EXISTS twitch_channel(id INT AUTO_INCREMENT NOT NULL  PRIMARY KEY, twitch_channel VARCHAR(255) NULL);");
            statement.execute("CREATE TABLE IF NOT EXISTS youtube_channel(id INT AUTO_INCREMENT NOT NULL  PRIMARY KEY, youtube_channel VARCHAR(255) NULL, last_video_uuid VARCHAR(255) NULL);");
            statement.execute("CREATE TABLE IF NOT EXISTS youtube_guild(id INT AUTO_INCREMENT NOT NULL  PRIMARY KEY, discord_guild_id INT NULL, youtube_channel_id INT NULL, discord_text_channel_id mediumtext NULL, FOREIGN KEY (discord_guild_id) REFERENCES discord_guild (id), FOREIGN KEY (youtube_channel_id) REFERENCES youtube_channel (id));");
            statement.execute("CREATE TABLE IF NOT EXISTS twitch_guilds(id INT AUTO_INCREMENT PRIMARY KEY, discord_guild_id INT NULL, twitch_channel_id INT NULL,discord_text_channel_id mediumtext NULL,discord_role_id mediumtext NULL,FOREIGN KEY (discord_guild_id) REFERENCES discord_guild (id),FOREIGN KEY (twitch_channel_id) REFERENCES twitch_channel (id));");
            try {
                ResultSet resultSet = this.source.getConnection().getMetaData().getIndexInfo(null, null, "twitch_guilds", false, false);
                boolean indexExists = false;
                while (resultSet.next()) {
                    if ("discord_guild_id".equals(resultSet.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                resultSet.close();
                if (!indexExists) {
                    statement.execute("CREATE INDEX discord_guild_id ON twitch_guilds (discord_guild_id);");
                }

                resultSet = this.source.getConnection().getMetaData().getIndexInfo(null, null, "youtube_guild", false, false);
                indexExists = false;
                while (resultSet.next()) {
                    if ("discord_guild_id".equals(resultSet.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                resultSet.close();
                if (!indexExists) {
                    statement.execute("CREATE INDEX discord_guild_id ON youtube_guild (discord_guild_id);");
                }

                resultSet = this.source.getConnection().getMetaData().getIndexInfo(null, null, "twitch_guilds", false, false);
                indexExists = false;
                while (resultSet.next()) {
                    if ("twitch_channel_id".equals(resultSet.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                resultSet.close();
                if (!indexExists) {
                    statement.execute("CREATE INDEX twitch_channel_id ON twitch_guilds (twitch_channel_id);");
                }

                resultSet = this.source.getConnection().getMetaData().getIndexInfo(null, null, "youtube_guild", false, false);
                indexExists = false;
                while (resultSet.next()) {
                    if ("youtube_channel_id".equals(resultSet.getString("INDEX_NAME"))) {
                        indexExists = true;
                        break;
                    }
                }
                resultSet.close();
                if (!indexExists) {
                    statement.execute("CREATE INDEX youtube_channel_id ON youtube_guild (youtube_channel_id);");
                }
            } catch (SQLException exception) {
                System.out.println(exception.getMessage());
                Sentry.captureException(exception);
            }

            statement.close();
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
        System.out.println("[MYSQL] Initialized MySQL!");
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