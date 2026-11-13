package server.dao;

import server.hatang.CauHinh;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lop dung chung de mo ket noi CSDL cho cac DAO phia may chu.
 */
public final class CSDLDao {
    private final CauHinh cauHinh;

    public CSDLDao(CauHinh cauHinh) {
        this.cauHinh = cauHinh;
    }

    public Connection moKetNoi() throws SQLException {
        String url = cauHinh.urlCSDL();
        String taiKhoan = cauHinh.taiKhoanCSDL();
        String matKhau = cauHinh.matKhauCSDL();

        Connection ketNoi = DriverManager.getConnection(url, taiKhoan, matKhau);
        // Đọc và ghi thời điểm theo UTC cho mọi DAO.
        try (Statement lenh = ketNoi.createStatement()) {
            lenh.execute("SET time_zone = '+00:00'");
        }
        return ketNoi;
    }

    public void kiemTra() throws SQLException {
        try (Connection ketNoi = moKetNoi(); Statement lenh = ketNoi.createStatement()) {
            lenh.executeQuery("SELECT 1 FROM nguoi_dung LIMIT 1");
        }
    }
}
