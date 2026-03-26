package xyz.iamthedefender.dragonmc.bot.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private final String path;
    private Connection conn;

    public Database(String path) {
        this.path = path;
    }

    public void init() throws Exception {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("PRAGMA journal_mode=WAL");
            st.executeUpdate("PRAGMA foreign_keys=ON");

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS linked_accounts (
                        discord_id  TEXT PRIMARY KEY,
                        mc_username TEXT NOT NULL,
                        uuid        TEXT,
                        is_admin    INTEGER NOT NULL DEFAULT 0,
                        linked_at   INTEGER NOT NULL
                    )""");

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pending_links (
                        discord_id  TEXT PRIMARY KEY,
                        mc_username TEXT NOT NULL,
                        code        TEXT NOT NULL,
                        created_at  INTEGER NOT NULL
                    )""");

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS whitelisted_ips (
                        discord_id   TEXT NOT NULL,
                        ip           TEXT NOT NULL,
                        confirmed_at INTEGER NOT NULL,
                        PRIMARY KEY (discord_id, ip)
                    )""");

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pending_ip_confirms (
                        discord_id  TEXT NOT NULL,
                        mc_username TEXT NOT NULL,
                        ip          TEXT NOT NULL,
                        location    TEXT,
                        message_id  TEXT,
                        created_at  INTEGER NOT NULL,
                        PRIMARY KEY (discord_id, ip)
                    )""");

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bans (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        mc_username TEXT,
                        uuid        TEXT,
                        ip          TEXT,
                        reason      TEXT NOT NULL DEFAULT 'No reason given',
                        banned_by   TEXT NOT NULL,
                        source      TEXT NOT NULL DEFAULT 'discord',
                        banned_at   INTEGER NOT NULL,
                        active      INTEGER NOT NULL DEFAULT 1
                    )""");

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS mutes (
                        mc_username TEXT PRIMARY KEY,
                        reason      TEXT NOT NULL DEFAULT 'No reason given',
                        muted_by    TEXT NOT NULL,
                        muted_at    INTEGER NOT NULL
                    )""");
        }
        System.out.println("[Dragon] Database initialised: " + path);
    }

    public Connection getConn() {
        return conn;
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
