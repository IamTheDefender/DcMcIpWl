package xyz.iamthedefender.dragonmc.database;

import lombok.Getter;
import xyz.iamthedefender.dragonmc.DragonMcPlugin;

import java.io.File;
import java.sql.*;

public class PluginDB {
    private final String path;
    @Getter
    private Connection conn;

    public PluginDB() {
        this.path = new File(DragonMcPlugin.getInstance().getDataFolder().getParentFile().getParentFile(), "launcher/dragon.db").getAbsolutePath();
    }

    private Connection connection() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + path);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() throws Exception {
        new java.io.File(path).getParentFile().mkdirs();
        conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("PRAGMA journal_mode=WAL");
            st.executeUpdate("PRAGMA foreign_keys=ON");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS linked_accounts (discord_id TEXT PRIMARY KEY, mc_username TEXT NOT NULL, uuid TEXT, is_admin INTEGER NOT NULL DEFAULT 0, linked_at INTEGER NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pending_links (discord_id TEXT PRIMARY KEY, mc_username TEXT NOT NULL, code TEXT NOT NULL, created_at INTEGER NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS whitelisted_ips (discord_id TEXT NOT NULL, ip TEXT NOT NULL, confirmed_at INTEGER NOT NULL, PRIMARY KEY (discord_id, ip))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pending_ip_confirms (discord_id TEXT NOT NULL, mc_username TEXT NOT NULL, ip TEXT NOT NULL, location TEXT, message_id TEXT, created_at INTEGER NOT NULL, PRIMARY KEY (discord_id, ip))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS bans (id INTEGER PRIMARY KEY AUTOINCREMENT, mc_username TEXT, uuid TEXT, ip TEXT, reason TEXT NOT NULL DEFAULT 'No reason given', banned_by TEXT NOT NULL, source TEXT NOT NULL DEFAULT 'minecraft', banned_at INTEGER NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS mutes (mc_username TEXT PRIMARY KEY, reason TEXT NOT NULL DEFAULT 'No reason given', muted_by TEXT NOT NULL, muted_at INTEGER NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS aliases (alias TEXT PRIMARY KEY, command TEXT NOT NULL)");
        }
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    private boolean q1(String sql, String p1) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p1);
            return ps.executeQuery().next();
        }
    }

    private String qs(String sql, String p1, String col) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p1);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(col) : null;
        }
    }

    public boolean isBanned(String mc) throws SQLException {
        return q1("SELECT 1 FROM bans WHERE mc_username=? AND active=1 LIMIT 1", mc);
    }

    public boolean isIpBanned(String ip) throws SQLException {
        return q1("SELECT 1 FROM bans WHERE ip=? AND active=1 LIMIT 1", ip);
    }

    public String getBanReason(String mc) throws SQLException {
        String r = qs("SELECT reason FROM bans WHERE mc_username=? AND active=1 LIMIT 1", mc, "reason");
        return r != null ? r : "Banned";
    }

    public boolean isMuted(String mc) throws SQLException {
        return q1("SELECT 1 FROM mutes WHERE mc_username=? LIMIT 1", mc);
    }

    public boolean isLinked(String mc) throws SQLException {
        return q1("SELECT 1 FROM linked_accounts WHERE mc_username=? LIMIT 1", mc);
    }

    public String getDiscordIdForMc(String mc) throws SQLException {
        return qs("SELECT discord_id FROM linked_accounts WHERE mc_username=? LIMIT 1", mc, "discord_id");
    }

    public boolean isAdmin(String discordId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT is_admin FROM linked_accounts WHERE discord_id=? LIMIT 1")) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("is_admin") == 1;
        }
    }

    public boolean isIpWhitelisted(String discordId, String ip) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT 1 FROM whitelisted_ips WHERE discord_id=? AND ip=?")) {
            ps.setString(1, discordId);
            ps.setString(2, ip);
            return ps.executeQuery().next();
        }
    }

    public void addWhitelistedIp(String discordId, String ip) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO whitelisted_ips (discord_id,ip,confirmed_at) VALUES (?,?,?)")) {
            ps.setString(1, discordId);
            ps.setString(2, ip);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public int getIpCount(String discordId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM whitelisted_ips WHERE discord_id=?")) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean hasPendingLink(String playerName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pending_links WHERE mc_username=? LIMIT 1")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public ResultSet getPendingLinkByCode(String code) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM pending_links WHERE code=? LIMIT 1");
        ps.setString(1, code);
        return ps.executeQuery();
    }

    public void completeLinkFromCode(String code, String uuid) throws SQLException {
        try (PreparedStatement sel = conn.prepareStatement("SELECT * FROM pending_links WHERE code=? LIMIT 1")) {
            sel.setString(1, code);
            ResultSet rs = sel.executeQuery();
            if (!rs.next()) return;
            String dId = rs.getString("discord_id"), mc = rs.getString("mc_username");
            try (PreparedStatement ins = conn.prepareStatement("INSERT OR REPLACE INTO linked_accounts (discord_id,mc_username,uuid,is_admin,linked_at) VALUES (?,?,?,0,?)")) {
                ins.setString(1, dId);
                ins.setString(2, mc);
                ins.setString(3, uuid);
                ins.setLong(4, System.currentTimeMillis());
                ins.executeUpdate();
            }
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM pending_links WHERE discord_id=?")) {
                del.setString(1, dId);
                del.executeUpdate();
            }
        }
    }

    public void banPlayer(String mc, String uuid, String reason, String by, String source) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO bans (mc_username,uuid,reason,banned_by,source,banned_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, mc);
            ps.setString(2, uuid);
            ps.setString(3, reason);
            ps.setString(4, by);
            ps.setString(5, source);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void unbanPlayer(String mc) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE bans SET active=0 WHERE mc_username=? AND active=1")) {
            ps.setString(1, mc);
            ps.executeUpdate();
        }
    }

    public void mutePlayer(String mc, String reason, String by) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO mutes (mc_username,reason,muted_by,muted_at) VALUES (?,?,?,?)")) {
            ps.setString(1, mc);
            ps.setString(2, reason);
            ps.setString(3, by);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void unmutePlayer(String mc) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM mutes WHERE mc_username=?")) {
            ps.setString(1, mc);
            ps.executeUpdate();
        }
    }

    public void saveAlias(String alias, String cmd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO aliases (alias,command) VALUES (?,?)")) {
            ps.setString(1, alias.toLowerCase());
            ps.setString(2, cmd);
            ps.executeUpdate();
        }
    }

    public String getAlias(String alias) throws SQLException {
        return qs("SELECT command FROM aliases WHERE alias=?", alias.toLowerCase(), "command");
    }

    public void deleteAlias(String alias) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM aliases WHERE alias=?")) {
            ps.setString(1, alias.toLowerCase());
            ps.executeUpdate();
        }
    }
}
