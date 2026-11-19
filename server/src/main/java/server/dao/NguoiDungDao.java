package server.dao;

import server.model.LoiNghiepVuModel;
import server.model.NguoiDungModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class NguoiDungDao {
    private final CSDLDao csdl;

    public NguoiDungDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public boolean tonTai(long id) throws SQLException {
        try (Connection db = csdl.moKetNoi();
             PreparedStatement lenh = db.prepareStatement("SELECT 1 FROM nguoi_dung WHERE id=?")) {
            lenh.setLong(1, id);
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next();
            }
        }
    }

    public NguoiDungModel timTheoSoDienThoai(long nguoiTim, String so) throws SQLException {
        String soGoc = so.startsWith("+") ? so.substring(1) : so;
        String soNoiDia = soGoc.matches("84[0-9]{9}") ? "0" + soGoc.substring(2) : soGoc;
        String soQuocTe = soNoiDia.matches("0[0-9]{9}") ? "84" + soNoiDia.substring(1) : soGoc;

        String sql = """
                SELECT n.* FROM nguoi_dung n
                JOIN quyen_rieng_tu q ON q.nguoi_dung_id=n.id
                LEFT JOIN ban_be b ON b.nguoi_nho_id=LEAST(n.id,?) AND b.nguoi_lon_id=GREATEST(n.id,?)
                WHERE n.id<>? AND n.so_dien_thoai IN (?,?,?,?)
                  AND (q.so_dien_thoai='moi_nguoi' OR (q.so_dien_thoai='ban_be' AND b.trang_thai='ban_be'))
                LIMIT 2
                """;

        try (Connection db = csdl.moKetNoi(); PreparedStatement lenh = db.prepareStatement(sql)) {
            lenh.setLong(1, nguoiTim);
            lenh.setLong(2, nguoiTim);
            lenh.setLong(3, nguoiTim);
            lenh.setString(4, so);
            lenh.setString(5, soNoiDia);
            lenh.setString(6, soQuocTe);
            lenh.setString(7, "+" + soQuocTe);

            try (ResultSet dong = lenh.executeQuery()) {
                if (!dong.next()) {
                    return null;
                }
                NguoiDungModel ketQua = docNguoi(dong);
                return dong.next() ? null : ketQua;
            }
        }
    }

    public NguoiDungModel timTheoEmail(String email) throws SQLException {
        String sql = "SELECT id,ten_hien_thi,email,mat_khau_bam FROM nguoi_dung WHERE email=?";

        try (Connection ketNoi = csdl.moKetNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {
            lenh.setString(1, email);
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next() ? docNguoi(dong) : null;
            }
        }
    }

    public NguoiDungModel themNguoi(String ten, String email, String matKhauBam) throws SQLException {
        String sql = "INSERT INTO nguoi_dung(ten_hien_thi,email,mat_khau_bam) VALUES(?,?,?)";

        try (Connection ketNoi = csdl.moKetNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {
            lenh.setString(1, ten);
            lenh.setString(2, email);
            lenh.setString(3, matKhauBam);
            lenh.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException loi) {
            throw new LoiNghiepVuModel("emailDaDung", "Email đã được đăng ký.");
        }

        return timTheoEmail(email);
    }

    public List<NguoiDungModel> danhSachNguoi() throws SQLException {
        List<NguoiDungModel> cacNguoi = new ArrayList<>();
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "SELECT id,ten_hien_thi,email,mat_khau_bam FROM nguoi_dung ORDER BY ten_hien_thi,id LIMIT 200");
             ResultSet dong = lenh.executeQuery()) {
            while (dong.next()) {
                cacNguoi.add(docNguoi(dong));
            }
        }
        return cacNguoi;
    }

    private NguoiDungModel docNguoi(ResultSet dong) throws SQLException {
        return new NguoiDungModel(dong.getLong("id"), dong.getString("ten_hien_thi"), dong.getString("email"),
                dong.getString("mat_khau_bam"));
    }
}
