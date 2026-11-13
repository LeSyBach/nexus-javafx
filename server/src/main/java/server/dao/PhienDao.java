package server.dao;

import server.hatang.BaoMat;
import server.model.NguoiDungModel;
import server.model.LoiNghiepVuModel;

import java.sql.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSDL chỉ lưu SHA-256 của mã phiên ngẫu nhiên, không lưu mã gốc hay mật khẩu.
 */
public final class PhienDao {
    private final CSDLDao csdl;

    public PhienDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public static String bam(String ma) {
        if (ma == null || !ma.matches("[A-Za-z0-9_-]{43}")) {
            throw new LoiNghiepVuModel("phienKhongHopLe",
                    "Phiên đăng nhập không còn hiệu lực. Hãy đăng nhập lại.");
        }
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(ma.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException loi) {
            throw new IllegalStateException(loi);
        }
    }

    public record PhienDaTao(NguoiDungModel nguoi, String ma) {
    }

    public PhienDaTao dangNhap(String email, String matKhau) throws SQLException {
        String sqlTimTaiKhoan =
                "SELECT id,ten_hien_thi,email,mat_khau_bam FROM nguoi_dung WHERE email=? FOR UPDATE";
        String sqlLuuPhien = "INSERT INTO phien_dang_nhap(bam_ma,nguoi_dung_id) VALUES(?,?)";

        try (Connection ketNoi = csdl.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                NguoiDungModel nguoi = null;
                try (PreparedStatement tim = ketNoi.prepareStatement(sqlTimTaiKhoan)) {
                    tim.setString(1, email);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (dong.next()) {
                            nguoi = new NguoiDungModel(dong.getLong("id"), dong.getString("ten_hien_thi"),
                                    dong.getString("email"), dong.getString("mat_khau_bam"));
                        }
                    }
                }

                String matKhauBam = nguoi == null ? null : nguoi.matKhauBam();
                if (!BaoMat.doiChieu(matKhau, matKhauBam)) {
                    throw new LoiNghiepVuModel("dangNhapSai", "Email hoặc mật khẩu chưa đúng.");
                }

                byte[] cacByte = new byte[32];
                new SecureRandom().nextBytes(cacByte);
                String ma = Base64.getUrlEncoder().withoutPadding().encodeToString(cacByte);

                try (PreparedStatement them = ketNoi.prepareStatement(sqlLuuPhien)) {
                    them.setString(1, bam(ma));
                    them.setLong(2, nguoi.id());
                    them.executeUpdate();
                }

                ketNoi.commit();
                return new PhienDaTao(nguoi, ma);
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    public String tao(long nguoiDung) throws SQLException {
        byte[] cacByte = new byte[32];
        new SecureRandom().nextBytes(cacByte);
        String ma = Base64.getUrlEncoder().withoutPadding().encodeToString(cacByte);
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "INSERT INTO phien_dang_nhap(bam_ma,nguoi_dung_id) VALUES(?,?)")) {
            lenh.setString(1, bam(ma));
            lenh.setLong(2, nguoiDung);
            lenh.executeUpdate();
        }
        return ma;
    }

    public NguoiDungModel khoiPhuc(String ma) throws SQLException {
        String sql =
                "SELECT n.id,n.ten_hien_thi,n.email,n.mat_khau_bam FROM phien_dang_nhap p JOIN nguoi_dung n ON n.id=p.nguoi_dung_id WHERE p.bam_ma=?";
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(sql)) {
            lenh.setString(1, bam(ma));
            try (ResultSet dong = lenh.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("phienKhongHopLe",
                            "Phiên đăng nhập không còn hiệu lực. Hãy đăng nhập lại.");
                }
                return new NguoiDungModel(dong.getLong("id"), dong.getString("ten_hien_thi"),
                        dong.getString("email"), dong.getString("mat_khau_bam"));
            }
        }
    }

    public boolean conHieuLuc(String bam, long nguoiDung) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "SELECT 1 FROM phien_dang_nhap WHERE bam_ma=? AND nguoi_dung_id=?")) {
            lenh.setString(1, bam);
            lenh.setLong(2, nguoiDung);
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next();
            }
        }
    }

    public void huy(String bam) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi();
             PreparedStatement lenh = ketNoi.prepareStatement("DELETE FROM phien_dang_nhap WHERE bam_ma=?")) {
            lenh.setString(1, bam);
            lenh.executeUpdate();
        }
    }
}
