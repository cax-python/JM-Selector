package com.cax.select.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//albums 联合主键 album_id, preference

public class SqliteHistoryRepository implements HistoryRepository, AutoCloseable {

    private final Connection conn;

    public SqliteHistoryRepository(String dbPath) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTable();
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS albums (" +
                "album_id TEXT NOT NULL," +
                "title TEXT," +
                "verdict INTEGER NOT NULL," +
                "reason TEXT," +
                "pass_ratio REAL," +
                "total_images INTEGER," +
                "pass_images INTEGER," +
                "preference TEXT NOT NULL," +
                "params TEXT," +
                "scanned_at TEXT," +
                "PRIMARY KEY (album_id, preference))";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    @Override
    public AlbumRecord find(String albumId, String preference) {
        String sql = "SELECT album_id,title,verdict,reason,pass_ratio,total_images,pass_images,preference,params,scanned_at " +
                "FROM albums WHERE album_id = ? AND preference = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, albumId);
            ps.setString(2, preference);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new AlbumRecord(
                        rs.getString("album_id"),
                        rs.getString("title"),
                        rs.getInt("verdict") != 0,
                        rs.getString("reason"),
                        rs.getDouble("pass_ratio"),
                        rs.getInt("total_images"),
                        rs.getInt("pass_images"),
                        rs.getString("preference"),
                        rs.getString("params"),
                        rs.getString("scanned_at"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询历史失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void upsert(AlbumRecord r) {
        String sql = "INSERT OR REPLACE INTO albums " +
                "(album_id,title,verdict,reason,pass_ratio,total_images,pass_images,preference,params,scanned_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.albumId);
            ps.setString(2, r.title);
            ps.setInt(3, r.selected ? 1 : 0);
            ps.setString(4, r.reason);
            ps.setDouble(5, r.passRatio);
            ps.setInt(6, r.totalImages);
            ps.setInt(7, r.passImages);
            ps.setString(8, r.preference);
            ps.setString(9, r.params);
            ps.setString(10, r.scannedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("写入历史失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignore) {
        }
    }
}
