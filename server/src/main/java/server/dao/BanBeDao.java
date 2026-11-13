package server.dao;

import server.model.LoiNghiepVuModel;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.sql.*;
import java.util.Set;

/**
 * JDBC cho quan hệ bạn bè; mỗi cặp tài csdlản chỉ có một dòng.
 */
public final class BanBeDao {
    private final CSDLDao csdl;

    public BanBeDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public JsonArray danhSach(long nguoiDung, long sauId, Set<Long> dangOnline) throws SQLException {
        String sql = """
                SELECT n.id,n.ten_hien_thi,b.nguoi_gui_id,b.trang_thai
                FROM ban_be b JOIN nguoi_dung n ON n.id=
                    CASE WHEN b.nguoi_nho_id=? THEN b.nguoi_lon_id ELSE b.nguoi_nho_id END
                WHERE (b.nguoi_nho_id=? OR b.nguoi_lon_id=?) AND n.id>?
                ORDER BY n.id LIMIT 50
                """;
        JsonArray ketQua = new JsonArray();
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(sql)) {
            lenh.setLong(1, nguoiDung);
            lenh.setLong(2, nguoiDung);
            lenh.setLong(3, nguoiDung);
            lenh.setLong(4, sauId);
            try (ResultSet dong = lenh.executeQuery()) {
                while (dong.next()) {
                    long id = dong.getLong("id");
                    ketQua.add(DuLieu.tao("id", Long.toString(id), "tenHienThi",
                            dong.getString("ten_hien_thi"),
                            "dangOnline", dangOnline.contains(id), "quanHe", quanHe(dong, nguoiDung)));
                }
            }
        }
        return ketQua;
    }

    public String quanHe(long nguoiDung, long doiPhuong) throws SQLException {
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(
                "SELECT nguoi_gui_id,trang_thai FROM ban_be WHERE nguoi_nho_id=? AND nguoi_lon_id=?")) {
            lenh.setLong(1, Math.min(nguoiDung, doiPhuong));
            lenh.setLong(2, Math.max(nguoiDung, doiPhuong));
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next() ? quanHe(dong, nguoiDung) : "chuaKetBan";
            }
        }
    }

    private String quanHe(ResultSet dong, long nguoiDung) throws SQLException {
        return dong.getString("trang_thai").equals("ban_be") ? "banBe" :
                dong.getLong("nguoi_gui_id") == nguoiDung ? "daGui" : "daNhan";
    }

    /**
     * Khóa tài csdlản theo thứ tự ID trước khi đọc/sửa cặp để tránh lời mời trùng khi gửi đồng thời.
     */
    public void thaoTac(long nguoiDung, long doiPhuong, String thaoTac) throws SQLException {
        if (nguoiDung == doiPhuong) {
            throw new LoiNghiepVuModel("khongTuKetBan", "Bạn không thể kết bạn với chính mình.");
        }
        if (!Set.of("gui", "chapNhan", "tuChoi", "thuHoi", "huyBan").contains(thaoTac)) {
            throw new LoiNghiepVuModel("thaoTacSai", "Thao tác bạn bè chưa hợp lệ.");
        }
        long nho = Math.min(nguoiDung, doiPhuong), lon = Math.max(nguoiDung, doiPhuong);
        try (Connection ketNoi = csdl.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                try (PreparedStatement khoa = ketNoi.prepareStatement(
                        "SELECT id FROM nguoi_dung WHERE id IN (?,?) ORDER BY id FOR UPDATE")) {
                    khoa.setLong(1, nho);
                    khoa.setLong(2, lon);
                    int so = 0;
                    try (ResultSet dong = khoa.executeQuery()) {
                        while (dong.next()) {
                            so++;
                        }
                    }
                    if (so != 2) {
                        throw new LoiNghiepVuModel("khongCoNguoiNhan", "Không tìm thấy tài csdlản này.");
                    }
                }
                String quanHe = "chuaKetBan";
                try (PreparedStatement tim = ketNoi.prepareStatement(
                        "SELECT nguoi_gui_id,trang_thai FROM ban_be WHERE nguoi_nho_id=? AND nguoi_lon_id=? FOR UPDATE")) {
                    tim.setLong(1, nho);
                    tim.setLong(2, lon);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (dong.next()) {
                            quanHe = quanHe(dong, nguoiDung);
                        }
                    }
                }
                switch (thaoTac) {
                    case "gui" -> {
                        if (quanHe.equals("daNhan")) {
                            throw new LoiNghiepVuModel("loiMoiDangCho",
                                    "Người này đã gửi lời mời. Hãy chọn Chấp nhận.");
                        }
                        if (quanHe.equals("chuaKetBan")) {
                            try (PreparedStatement them = ketNoi.prepareStatement(
                                    "INSERT INTO ban_be(nguoi_nho_id,nguoi_lon_id,nguoi_gui_id) VALUES(?,?,?)")) {
                                them.setLong(1, nho);
                                them.setLong(2, lon);
                                them.setLong(3, nguoiDung);
                                them.executeUpdate();
                            }
                        }
                        // Gửi lại cùng lời mời hoặc đã là bạn: không tạo thêm dòng.
                    }
                    case "chapNhan" -> {
                        if (!quanHe.equals("daNhan") && !quanHe.equals("banBe")) {
                            throw saiTrangThai();
                        }
                        if (quanHe.equals("daNhan")) {
                            doiTrangThai(ketNoi, nho, lon, false);
                        }
                    }
                    case "tuChoi", "thuHoi", "huyBan" -> {
                        String canCo = thaoTac.equals("tuChoi") ? "daNhan" :
                                thaoTac.equals("thuHoi") ? "daGui" : "banBe";
                        if (!quanHe.equals(canCo)) {
                            throw saiTrangThai();
                        }
                        doiTrangThai(ketNoi, nho, lon, true);
                    }
                    default -> throw new IllegalArgumentException();
                }
                ketNoi.commit();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    private LoiNghiepVuModel saiTrangThai() {
        return new LoiNghiepVuModel("quanHeDaDoi",
                "Trạng thái đã thay đổi hoặc bạn không có quyền thực hiện. Hãy tải lại danh sách.");
    }

    private void doiTrangThai(Connection ketNoi, long nho, long lon, boolean xoa) throws SQLException {
        try (PreparedStatement lenh = ketNoi.prepareStatement(xoa ?
                "DELETE FROM ban_be WHERE nguoi_nho_id=? AND nguoi_lon_id=?" :
                "UPDATE ban_be SET trang_thai='ban_be' WHERE nguoi_nho_id=? AND nguoi_lon_id=?")) {
            lenh.setLong(1, nho);
            lenh.setLong(2, lon);
            lenh.executeUpdate();
        }
    }
}
