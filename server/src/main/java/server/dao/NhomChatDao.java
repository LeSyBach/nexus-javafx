package server.dao;

import server.model.LoiNghiepVuModel;
import server.model.KetQuaLuuModel;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.sql.*;
import java.util.*;

/**
 * Lưu thành viên và tin nhắn nhóm. Mọi truy vấn đọc/ghi tin đều kiểm tra thành viên.
 */
public final class NhomChatDao {
    private static final String chonNhom = """
            SELECT g.id,g.ten,g.anh_dai_dien IS NOT NULL co_anh,COALESCE(t.tao_luc,g.tao_luc) hoat_dong,
                LEFT(t.noi_dung,80) tin_cuoi,
                (SELECT COUNT(*) FROM thanh_vien_nhom v WHERE v.nhom_id=g.id) so_thanh_vien
            FROM nhom_chat g
            LEFT JOIN tin_nhan_nhom t ON t.id=(SELECT MAX(m.id) FROM tin_nhan_nhom m WHERE m.nhom_id=g.id)
            """;
    private static final String chonTin = """
            SELECT t.id,t.nhom_id,t.nguoi_gui_id,t.ma_gui,t.noi_dung,t.tao_luc,u.ten_hien_thi
            FROM tin_nhan_nhom t JOIN nguoi_dung u ON u.id=t.nguoi_gui_id
            """;
    private static final Gson JSON = new Gson();
    private final CSDLDao csdl;

    public NhomChatDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public long tao(long nguoi, String ten, String ma, Set<Long> ban, byte[] anh) throws SQLException {
        ten = ten.strip();
        if (ten.isEmpty() || ten.length() > 80 || ten.chars().anyMatch(Character::isISOControl) ||
                ban.isEmpty() || ban.size() > 29 || ban.contains(nguoi)) {
            throw new LoiNghiepVuModel("nhomSai",
                    "Nhập tên nhóm tối đa 80 ký tự và chọn từ 1 đến 29 bạn bè.");
        }
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                try (PreparedStatement tim = db.prepareStatement(
                        "SELECT id FROM nhom_chat WHERE nguoi_tao_id=? AND ma_tao=?")) {
                    tim.setLong(1, nguoi);
                    tim.setString(2, ma);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (dong.next()) {
                            long id = dong.getLong(1);
                            db.commit();
                            return id;
                        }
                    }
                }
                try (PreparedStatement tim = db.prepareStatement(
                        "SELECT 1 FROM ban_be WHERE nguoi_nho_id=? AND nguoi_lon_id=? AND trang_thai='ban_be'")) {
                    for (long id : ban) {
                        tim.setLong(1, Math.min(nguoi, id));
                        tim.setLong(2, Math.max(nguoi, id));
                        try (ResultSet dong = tim.executeQuery()) {
                            if (!dong.next()) {
                                throw new LoiNghiepVuModel("khongPhaiBanBe",
                                        "Chỉ có thể thêm bạn bè vào nhóm.");
                            }
                        }
                        TinNhanDao.kiemTraGuiRieng(db, nguoi, id);
                    }
                }
                long id;
                try (PreparedStatement lenh = db.prepareStatement(
                        "INSERT INTO nhom_chat(nguoi_tao_id,ma_tao,ten,anh_dai_dien) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    lenh.setLong(1, nguoi);
                    lenh.setString(2, ma);
                    lenh.setString(3, ten);
                    lenh.setBytes(4, anh);
                    lenh.executeUpdate();
                    try (ResultSet khoa = lenh.getGeneratedKeys()) {
                        if (!khoa.next()) {
                            throw new SQLException("Thiếu mã nhóm");
                        }
                        id = khoa.getLong(1);
                    }
                }
                try (PreparedStatement them = db.prepareStatement(
                        "INSERT INTO thanh_vien_nhom VALUES(?,?)")) {
                    them.setLong(1, id);
                    them.setLong(2, nguoi);
                    them.addBatch();
                    for (long banId : ban) {
                        them.setLong(2, banId);
                        them.addBatch();
                    }
                    them.executeBatch();
                }
                db.commit();
                return id;
            } catch (SQLException | RuntimeException sai) {
                db.rollback();
                throw sai;
            }
        }
    }

    public Set<Long> thanhVien(long id) throws SQLException {
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(
                "SELECT nguoi_dung_id FROM thanh_vien_nhom WHERE nhom_id=?")) {
            tim.setLong(1, id);
            Set<Long> ds = new HashSet<>();
            try (ResultSet dong = tim.executeQuery()) {
                while (dong.next()) {
                    ds.add(dong.getLong(1));
                }
            }
            return ds;
        }
    }

    private void kiemTraThanhVien(Connection db, long nguoi, long nhom) throws SQLException {
        try (PreparedStatement tim = db.prepareStatement(
                "SELECT 1 FROM thanh_vien_nhom WHERE nhom_id=? AND nguoi_dung_id=?")) {
            tim.setLong(1, nhom);
            tim.setLong(2, nguoi);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("khongThuocNhom", "Bạn không thuộc nhóm này.");
                }
            }
        }
    }

    public JsonArray danhSach(long nguoi, Long sau) throws SQLException {
        String sql = chonNhom +
                " JOIN thanh_vien_nhom v ON v.nhom_id=g.id WHERE v.nguoi_dung_id=? AND g.id>? ORDER BY g.id LIMIT 50";
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(sql)) {
            tim.setLong(1, nguoi);
            tim.setLong(2, sau == null ? 0 : sau);
            JsonArray ds = new JsonArray();
            try (ResultSet dong = tim.executeQuery()) {
                while (dong.next()) {
                    ds.add(docNhom(dong));
                }
            }
            return ds;
        }
    }

    public JsonObject tomTat(long id) throws SQLException {
        try (Connection db = csdl.moKetNoi();
             PreparedStatement tim = db.prepareStatement(chonNhom + " WHERE g.id=?")) {
            tim.setLong(1, id);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    throw new SQLException("Không tìm thấy nhóm");
                }
                return docNhom(dong);
            }
        }
    }

    private JsonObject docNhom(ResultSet dong) throws SQLException {
        return DuLieu.tao("id", dong.getString("id"), "tenHienThi", dong.getString("ten"),
                "soThanhVien", dong.getInt("so_thanh_vien"), "coAnh", dong.getBoolean("co_anh"),
                "tinCuoi", dong.getString("tin_cuoi"),
                "hoatDong",
                dong.getTimestamp("hoat_dong").toLocalDateTime().toInstant(java.time.ZoneOffset.UTC)
                        .toString());
    }

    public String anh(long nguoi, long id) throws SQLException {
        try (Connection db = csdl.moKetNoi();
             PreparedStatement tim = db.prepareStatement("SELECT anh_dai_dien FROM nhom_chat WHERE id=?")) {
            kiemTraThanhVien(db, nguoi, id);
            tim.setLong(1, id);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    return "";
                }
                byte[] anh = dong.getBytes(1);
                return anh == null ? "" : Base64.getEncoder().encodeToString(anh);
            }
        }
    }

    public JsonObject lichSu(long nguoi, long id, Long truoc) throws SQLException {
        String sql = chonTin + " WHERE t.nhom_id=?" + (truoc == null ? "" : " AND t.id<?") +
                " ORDER BY t.id DESC LIMIT 21";
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(sql)) {
            kiemTraThanhVien(db, nguoi, id);
            tim.setLong(1, id);
            if (truoc != null) {
                tim.setLong(2, truoc);
            }
            List<JsonObject> ds = new ArrayList<>();
            int kichThuoc = 0;
            boolean con = false;
            try (ResultSet dong = tim.executeQuery()) {
                while (dong.next()) {
                    JsonObject tin = docTin(dong);
                    kichThuoc +=
                            JSON.toJson(tin).getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1;
                    if (ds.size() == 20 || kichThuoc > 48000) {
                        con = true;
                        break;
                    }
                    ds.add(tin);
                }
            }
            Collections.reverse(ds);
            JsonArray tin = new JsonArray();
            ds.forEach(tin::add);
            return DuLieu.tao("tinNhan", tin, "coTinCu", con);
        }
    }

    public KetQuaLuuModel gui(long nguoi, long id, String noiDung, String ma) throws SQLException {
        if (noiDung.isBlank() || noiDung.length() > 4000) {
            throw new LoiNghiepVuModel("tinSai", "Tin nhắn cần 1–4.000 ký tự.");
        }
        try (Connection db = csdl.moKetNoi()) {
            kiemTraThanhVien(db, nguoi, id);
            String khoa = " WHERE t.nhom_id=? AND t.nguoi_gui_id=? AND t.ma_gui=?";
            try (PreparedStatement tim = db.prepareStatement(chonTin + khoa)) {
                tim.setLong(1, id);
                tim.setLong(2, nguoi);
                tim.setString(3, ma);
                try (ResultSet dong = tim.executeQuery()) {
                    if (dong.next()) {
                        JsonObject tin = docTin(dong);
                        if (!noiDung.equals(DuLieu.chu(tin, "noiDung"))) {
                            throw new LoiNghiepVuModel("maGuiDaDung", "Mã gửi đã dùng cho tin khác.");
                        }
                        return new KetQuaLuuModel(tin, false);
                    }
                }
                try (PreparedStatement them = db.prepareStatement(
                        "INSERT INTO tin_nhan_nhom(nhom_id,nguoi_gui_id,ma_gui,noi_dung) VALUES(?,?,?,?)")) {
                    them.setLong(1, id);
                    them.setLong(2, nguoi);
                    them.setString(3, ma);
                    them.setString(4, noiDung);
                    them.executeUpdate();
                }
                try (ResultSet dong = tim.executeQuery()) {
                    if (!dong.next()) {
                        throw new SQLException("Không đọc được tin vừa lưu");
                    }
                    return new KetQuaLuuModel(docTin(dong), true);
                }
            }
        }
    }

    private JsonObject docTin(ResultSet dong) throws SQLException {
        return DuLieu.tao("id", dong.getString("id"), "nhomId", dong.getString("nhom_id"), "nguoiGuiId",
                dong.getString("nguoi_gui_id"),
                "tenNguoiGui", dong.getString("ten_hien_thi"), "maGui", dong.getString("ma_gui"),
                "noiDung", dong.getString("noi_dung"),
                "taoLuc", dong.getTimestamp("tao_luc").toLocalDateTime().toInstant(java.time.ZoneOffset.UTC)
                        .toString());
    }
}
