package xyz.iamthedefender.dragonmc.bot.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDB {

    private final Database db;

    public AccountDB(Database db) {
        this.db = db;
    }

    private Connection c() {
        return db.getConn();
    }

    public boolean isLinked(String discordId) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT 1 FROM linked_accounts WHERE discord_id=? LIMIT 1")) {
            ps.setString(1, discordId);
            return ps.executeQuery().next();
        }
    }

    public boolean isUsernameTaken(String mc) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT 1 FROM linked_accounts WHERE mc_username=? LIMIT 1")) {
            ps.setString(1, mc);
            return ps.executeQuery().next();
        }
    }

    public String getUsernameForDiscord(String discordId) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT mc_username FROM linked_accounts WHERE discord_id=? LIMIT 1")) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("mc_username") : null;
        }
    }

    public String getDiscordIdForUsername(String mc) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT discord_id FROM linked_accounts WHERE mc_username=? LIMIT 1")) {
            ps.setString(1, mc);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("discord_id") : null;
        }
    }

    public boolean isAdmin(String discordId) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT is_admin FROM linked_accounts WHERE discord_id=? LIMIT 1")) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("is_admin") == 1;
        }
    }

    public void savePendingLink(String discordId, String mc, String code) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("INSERT OR REPLACE INTO pending_links (discord_id,mc_username,code,created_at) VALUES (?,?,?,?)")) {
            ps.setString(1, discordId);
            ps.setString(2, mc);
            ps.setString(3, code);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public ResultSet getPendingLinkByCode(String code) throws SQLException {
        PreparedStatement ps = c().prepareStatement("SELECT * FROM pending_links WHERE code=? LIMIT 1");
        ps.setString(1, code);
        return ps.executeQuery();
    }

    public int getIpCount(String discordId) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT COUNT(*) FROM whitelisted_ips WHERE discord_id=?")) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean isIpWhitelisted(String discordId, String ip) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT 1 FROM whitelisted_ips WHERE discord_id=? AND ip=?")) {
            ps.setString(1, discordId);
            ps.setString(2, ip);
            return ps.executeQuery().next();
        }
    }

    public void addWhitelistedIp(String discordId, String ip) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("INSERT OR IGNORE INTO whitelisted_ips (discord_id,ip,confirmed_at) VALUES (?,?,?)")) {
            ps.setString(1, discordId);
            ps.setString(2, ip);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void savePendingIpConfirm(String discordId, String mc, String ip, String location, String msgId) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("INSERT OR REPLACE INTO pending_ip_confirms (discord_id,mc_username,ip,location,message_id,created_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, discordId);
            ps.setString(2, mc);
            ps.setString(3, ip);
            ps.setString(4, location);
            ps.setString(5, msgId);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public ResultSet getPendingIpConfirmByMessage(String msgId) throws SQLException {
        PreparedStatement ps = c().prepareStatement("SELECT * FROM pending_ip_confirms WHERE message_id=? LIMIT 1");
        ps.setString(1, msgId);
        return ps.executeQuery();
    }

    public void removePendingIpConfirm(String discordId, String ip) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("DELETE FROM pending_ip_confirms WHERE discord_id=? AND ip=?")) {
            ps.setString(1, discordId);
            ps.setString(2, ip);
            ps.executeUpdate();
        }
    }

    public void banPlayer(String mc, String uuid, String reason, String by, String source) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("INSERT INTO bans (mc_username,uuid,reason,banned_by,source,banned_at) VALUES (?,?,?,?,?,?)")) {
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
        try (PreparedStatement ps = c().prepareStatement("UPDATE bans SET active=0 WHERE mc_username=? AND active=1")) {
            ps.setString(1, mc);
            ps.executeUpdate();
        }
    }

    public boolean isBanned(String mc) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT 1 FROM bans WHERE mc_username=? AND active=1 LIMIT 1")) {
            ps.setString(1, mc);
            return ps.executeQuery().next();
        }
    }

    public boolean isIpBanned(String ip) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT 1 FROM bans WHERE ip=? AND active=1 LIMIT 1")) {
            ps.setString(1, ip);
            return ps.executeQuery().next();
        }
    }

    public String getBanReason(String mc) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT reason FROM bans WHERE mc_username=? AND active=1 LIMIT 1")) {
            ps.setString(1, mc);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("reason") : "Banned";
        }
    }

    public void mutePlayer(String mc, String reason, String by) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("INSERT OR REPLACE INTO mutes (mc_username,reason,muted_by,muted_at) VALUES (?,?,?,?)")) {
            ps.setString(1, mc);
            ps.setString(2, reason);
            ps.setString(3, by);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void unmutePlayer(String mc) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("DELETE FROM mutes WHERE mc_username=?")) {
            ps.setString(1, mc);
            ps.executeUpdate();
        }
    }

    public boolean isMuted(String mc) throws SQLException {
        try (PreparedStatement ps = c().prepareStatement("SELECT 1 FROM mutes WHERE mc_username=? LIMIT 1")) {
            ps.setString(1, mc);
            return ps.executeQuery().next();
        }
    }
}
