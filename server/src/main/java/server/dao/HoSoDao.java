package server.dao;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;
import server.hatang.BaoMat;
import server.model.LoiNghiepVuModel;

import java.sql.*;
import java.util.Base64;

public final class HoSoDao {
    private final CSDLDao csdl;

    public HoSoDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public JsonObject doc(long id) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "SELECT n.*,COALESCE(q.email,'chi_minh_toi') quyen_email,COALESCE(q.so_dien_thoai,'chi_minh_toi') quyen_dien_thoai FROM nguoi_dung n LEFT JOIN quyen_rieng_tu q ON q.nguoi_dung_id=n.id WHERE n.id=?")) {
            lenh.setLong(1, id);
            try (ResultSet dong = lenh.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("khongCoTaiKhoan", "Không tìm thấy tài khoản.");
                }
                byte[] anh = dong.getBytes("anh_dai_dien");
                return DuLieu.tao("id", dong.getString("id"), "tenHienThi", dong.getString("ten_hien_thi"),
                        "email", dong.getString("email"),
                        "quyenEmail", quyenTuCSDL(dong.getString("quyen_email")), "quyenDienThoai",
                        quyenTuCSDL(dong.getString("quyen_dien_thoai")),
                        "soDienThoai", dong.getString("so_dien_thoai"), "gioiThieu",
                        dong.getString("gioi_thieu"),
                        "anhDaiDien", anh == null ? "" : Base64.getEncoder().encodeToString(anh),
                        "phienBan", dong.getString("phien_ban_ho_so"), "thamGia",
                        dong.getTimestamp("tao_luc").toLocalDateTime().toLocalDate().toString());
            }
        }
    }

    public void luuQuyen(long nguoi, String email, String dienThoai) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "INSERT INTO quyen_rieng_tu(nguoi_dung_id,email,so_dien_thoai) VALUES(?,?,?) ON DUPLICATE KEY UPDATE email=VALUES(email),so_dien_thoai=VALUES(so_dien_thoai)")) {
            lenh.setLong(1, nguoi);
            lenh.setString(2, quyenChoCSDL(email));
            lenh.setString(3, quyenChoCSDL(dienThoai));
            lenh.executeUpdate();
        }
    }

    private static String quyenTuCSDL(String quyen) {
        return switch (quyen) {
            case "moi_nguoi" -> "moiNguoi";
            case "ban_be" -> "banBe";
            default -> "chiMinhToi";
        };
    }

    private static String quyenChoCSDL(String quyen) {
        return switch (quyen) {
            case "moiNguoi" -> "moi_nguoi";
            case "banBe" -> "ban_be";
            case "chiMinhToi" -> "chi_minh_toi";
            default -> throw new IllegalArgumentException("Quyền riêng tư không hợp lệ.");
        };
    }

    public void capNhat(long id, String ten, String dienThoai, String gioiThieu, byte[] anh,
                       long phienBan) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "UPDATE nguoi_dung SET ten_hien_thi=?,so_dien_thoai=?,gioi_thieu=?,anh_dai_dien=?,phien_ban_ho_so=phien_ban_ho_so+1 WHERE id=? AND phien_ban_ho_so=?")) {
            lenh.setString(1, ten);
            lenh.setString(2, dienThoai);
            lenh.setString(3, gioiThieu);
            lenh.setBytes(4, anh);
            lenh.setLong(5, id);
            lenh.setLong(6, phienBan);
            if (lenh.executeUpdate() != 1) {
                throw new LoiNghiepVuModel("hoSoDaDoi",
                        "Hồ sơ đã thay đổi ở nơi khác. Hãy Hủy rồi mở chỉnh sửa lại.");
            }
        }
    }

    public void doiMatKhau(long id, String cu, String moi, String phienHienTai) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                try (PreparedStatement tim = ketNoi.prepareStatement(
                        "SELECT mat_khau_bam FROM nguoi_dung WHERE id=? FOR UPDATE")) {
                    tim.setLong(1, id);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (!dong.next() || !BaoMat.doiChieu(cu, dong.getString(1))) {
                            throw new LoiNghiepVuModel("matKhauCuSai", "Mật khẩu hiện tại chưa đúng.");
                        }
                    }
                }
                try (PreparedStatement lenh = ketNoi.prepareStatement(
                        "UPDATE nguoi_dung SET mat_khau_bam=? WHERE id=?")) {
                    lenh.setString(1, BaoMat.bam(moi));
                    lenh.setLong(2, id);
                    lenh.executeUpdate();
                }
                try (PreparedStatement lenh = ketNoi.prepareStatement(
                        "DELETE FROM phien_dang_nhap WHERE nguoi_dung_id=? AND bam_ma<>?")) {
                    lenh.setLong(1, id);
                    lenh.setString(2, phienHienTai);
                    lenh.executeUpdate();
                }
                ketNoi.commit();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }
}
