package server.dao;

import com.google.gson.*;
import giaothuc.DuLieu;
import server.model.CuocGoiModel;
import server.model.KetQuaLuuModel;
import server.model.LoiNghiepVuModel;

import java.sql.*;
import java.time.ZoneOffset;
import java.util.*;

public final class TinNhanDao {
    private final CSDLDao csdl;

    public TinNhanDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    static final String chonTin =
            "SELECT t.*,n.ten_hien_thi,p.ma_luu,p.ten_tep,p.kich_thuoc,p.sha256,p.loai_tep FROM tin_nhan t JOIN nguoi_dung n ON n.id=t.nguoi_gui_id LEFT JOIN tep_dinh_kem p ON p.tin_nhan_id=t.id ";

    public boolean daLuuCuocGoi(long nguoiGoi, UUID id) throws SQLException {
        try (Connection db = csdl.moKetNoi()) {
            return timCuocGoi(db, nguoiGoi, id) != null;
        }
    }

    public KetQuaLuuModel luuCuocGoi(CuocGoiModel cuocGoi) throws SQLException {
        try (Connection db = csdl.moKetNoi()) {
            JsonObject daLuu = timCuocGoi(db, cuocGoi.nguoiGoi(), cuocGoi.id());
            if (daLuu != null) {
                return new KetQuaLuuModel(daLuu, false);
            }
            String sql = """
                    INSERT INTO tin_nhan(ma_gui,nguoi_gui_id,nguoi_nhan_id,noi_dung,bam_noi_dung,
                        loai_tin,cuoc_goi_id,ket_qua_goi,thoi_luong_goi,goi_video)
                    VALUES(?,?,?,?,?,'cuoc_goi',?,?,?,?)
                    """;
            String noiDung = cuocGoi.noiDung();
            try (PreparedStatement lenh = db.prepareStatement(sql)) {
                lenh.setString(1, UUID.randomUUID().toString());
                lenh.setLong(2, cuocGoi.nguoiGoi());
                lenh.setLong(3, cuocGoi.nguoiNhan());
                lenh.setString(4, noiDung);
                lenh.setString(5, TuongTacTinNhanDao.bamChu(noiDung));
                lenh.setString(6, cuocGoi.id().toString());
                lenh.setString(7, cuocGoi.ketQua().name());
                lenh.setLong(8, cuocGoi.soGiay());
                lenh.setBoolean(9, cuocGoi.video());
                lenh.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException loi) {
                daLuu = timCuocGoi(db, cuocGoi.nguoiGoi(), cuocGoi.id());
                if (daLuu != null) {
                    return new KetQuaLuuModel(daLuu, false);
                }
                throw loi;
            }
            return new KetQuaLuuModel(timCuocGoi(db, cuocGoi.nguoiGoi(), cuocGoi.id()), true);
        }
    }

    private JsonObject timCuocGoi(Connection db, long nguoiGoi, UUID id) throws SQLException {
        try (PreparedStatement lenh = db.prepareStatement(chonTin + "WHERE t.nguoi_gui_id=? AND t.cuoc_goi_id=?")) {
            lenh.setLong(1, nguoiGoi);
            lenh.setString(2, id.toString());
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next() ? docTin(dong) : null;
            }
        }
    }

    public KetQuaLuuModel luuTin(long nguoiGui, Long nguoiNhan, String maGui, String noiDung) throws SQLException {
        return luuTin(nguoiGui, nguoiNhan, maGui, noiDung, null);
    }

    public KetQuaLuuModel luuTin(long nguoiGui, Long nguoiNhan, String maGui, String noiDung,
                            Long traLoi) throws SQLException {
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                // Cùng khóa với gửi tệp: một mã gửi chỉ tạo một tin, kể cả gửi lại sau thu hồi.
                try (PreparedStatement khoa = db.prepareStatement(
                        "SELECT id FROM nguoi_dung WHERE id=? FOR UPDATE")) {
                    khoa.setLong(1, nguoiGui);
                    try (ResultSet dong = khoa.executeQuery()) {
                        if (!dong.next()) {
                            throw new SQLException("Tài khoản không còn.");
                        }
                    }
                }
                try (PreparedStatement tim = db.prepareStatement(
                        chonTin + "WHERE t.nguoi_gui_id=? AND t.ma_gui=?")) {
                    tim.setLong(1, nguoiGui);
                    tim.setString(2, maGui);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (dong.next()) {
                            if (!dong.getString("loai_tin").equals("van_ban") ||
                                    !Objects.equals(nguoiNhan, dong.getObject("nguoi_nhan_id", Long.class)) ||
                                    !Objects.equals(traLoi, dong.getObject("tra_loi_id", Long.class)) ||
                                    !TuongTacTinNhanDao.bamChu(noiDung)
                                            .equals(dong.getString("bam_noi_dung"))) {
                                throw new LoiNghiepVuModel("maGuiDaDung", "Mã gửi đã dùng cho tin khác.");
                            }
                            JsonObject tin = docTin(dong);
                            db.commit();
                            return new KetQuaLuuModel(tin, false);
                        }
                    }
                }
                if (nguoiNhan != null) {
                    try (PreparedStatement tim = db.prepareStatement(
                            "SELECT id FROM nguoi_dung WHERE id=?")) {
                        tim.setLong(1, nguoiNhan);
                        try (ResultSet dong = tim.executeQuery()) {
                            if (!dong.next()) {
                                throw new LoiNghiepVuModel("khongCoNguoiNhan",
                                        "Không tìm thấy người nhận.");
                            }
                        }
                    }
                }
                kiemTraGuiRieng(db, nguoiGui, nguoiNhan);
                TuongTacTinNhanDao.kiemTraTraLoi(db, nguoiGui, nguoiNhan, traLoi);
                long id;
                try (PreparedStatement them = db.prepareStatement(
                        "INSERT INTO tin_nhan(ma_gui,nguoi_gui_id,nguoi_nhan_id,noi_dung,tra_loi_id,bam_noi_dung) VALUES(?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    them.setString(1, maGui);
                    them.setLong(2, nguoiGui);
                    them.setObject(3, nguoiNhan, Types.BIGINT);
                    them.setString(4, noiDung);
                    them.setObject(5, traLoi, Types.BIGINT);
                    them.setString(6, TuongTacTinNhanDao.bamChu(noiDung));
                    them.executeUpdate();
                    try (ResultSet dong = them.getGeneratedKeys()) {
                        if (!dong.next()) {
                            throw new SQLException();
                        }
                        id = dong.getLong(1);
                    }
                }
                try (PreparedStatement tim = db.prepareStatement(chonTin + "WHERE t.id=?")) {
                    tim.setLong(1, id);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (!dong.next()) {
                            throw new SQLException();
                        }
                        JsonObject tin = docTin(dong);
                        db.commit();
                        return new KetQuaLuuModel(tin, true);
                    }
                }
            } catch (SQLException | RuntimeException loi) {
                db.rollback();
                throw loi;
            }
        }
    }

    public JsonArray lichSu(long nguoiDangDung, Long doiPhuong, Long truocId) throws SQLException {
        String dieuKien = doiPhuong == null ? "t.nguoi_nhan_id IS NULL" :
                "((t.nguoi_gui_id=? AND t.nguoi_nhan_id=?) OR (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?))";
        String locTinCu = truocId == null ? "" : " AND t.id<?";
        String locTinDaAn = " AND NOT EXISTS (SELECT 1 FROM tin_da_an a WHERE a.tin_nhan_id=t.id AND a.nguoi_dung_id="
                + nguoiDangDung + ")";
        String sql = chonTin + "WHERE " + dieuKien + locTinCu + locTinDaAn + " ORDER BY t.id DESC LIMIT 50";

        List<JsonObject> cacTin = new ArrayList<>();
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(sql)) {
            int viTri = 1;
            if (doiPhuong != null) {
                lenh.setLong(viTri++, nguoiDangDung);
                lenh.setLong(viTri++, doiPhuong);
                lenh.setLong(viTri++, doiPhuong);
                lenh.setLong(viTri++, nguoiDangDung);
            }
            if (truocId != null) {
                lenh.setLong(viTri, truocId);
            }

            try (ResultSet dong = lenh.executeQuery()) {
                int tongByte = 2;
                while (dong.next()) {
                    JsonObject tin = docTin(dong);
                    String jsonTin = new Gson().toJson(tin);
                    int soByte = jsonTin.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1;
                    if (tongByte + soByte > 50000 && !cacTin.isEmpty()) {
                        break;
                    }
                    cacTin.add(tin);
                    tongByte += soByte;
                }
            }
        }

        Collections.reverse(cacTin);
        JsonArray ketQua = new JsonArray();
        cacTin.forEach(ketQua::add);
        return ketQua;
    }

    public boolean conTinCu(long nguoiDung, Long doiPhuong, JsonArray cacTin) throws SQLException {
        if (cacTin.isEmpty()) {
            return false;
        }
        String dieuKien = doiPhuong == null ? "nguoi_nhan_id IS NULL" :
                "((nguoi_gui_id=? AND nguoi_nhan_id=?) OR (nguoi_gui_id=? AND nguoi_nhan_id=?))";
        try (Connection db = csdl.moKetNoi(); PreparedStatement lenh = db.prepareStatement(
                "SELECT id FROM tin_nhan WHERE id<? AND " + dieuKien +
                        " AND NOT EXISTS (SELECT 1 FROM tin_da_an a WHERE a.tin_nhan_id=tin_nhan.id AND a.nguoi_dung_id=" +
                        nguoiDung + ") LIMIT 1")) {
            lenh.setLong(1, Long.parseLong(DuLieu.chu(cacTin.get(0).getAsJsonObject(), "id")));
            if (doiPhuong != null) {
                lenh.setLong(2, nguoiDung);
                lenh.setLong(3, doiPhuong);
                lenh.setLong(4, doiPhuong);
                lenh.setLong(5, nguoiDung);
            }
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next();
            }
        }
    }

    /**
     * Gom theo doi phuong; chi doc du lieu cua tai khoan da xac thuc.
     */
    public boolean daChan(long nguoi, long dich) throws SQLException {
        try (Connection db = csdl.moKetNoi(); PreparedStatement lenh = db.prepareStatement(
                "SELECT 1 FROM nguoi_bi_chan WHERE nguoi_dung_id=? AND doi_phuong_id=?")) {
            lenh.setLong(1, nguoi);
            lenh.setLong(2, dich);
            try (ResultSet dong = lenh.executeQuery()) {
                return dong.next();
            }
        }
    }

    public void datChan(long nguoi, long dich, boolean chan) throws SQLException {
        String sql = chan ? "INSERT IGNORE INTO nguoi_bi_chan VALUES (?,?)" :
                "DELETE FROM nguoi_bi_chan WHERE nguoi_dung_id=? AND doi_phuong_id=?";
        try (Connection db = csdl.moKetNoi(); PreparedStatement lenh = db.prepareStatement(sql)) {
            lenh.setLong(1, nguoi);
            lenh.setLong(2, dich);
            lenh.executeUpdate();
        }
    }

    static void kiemTraGuiRieng(Connection db, long gui, Long nhan) throws SQLException {
        if (nhan == null) {
            return;
        }
        try (PreparedStatement lenh = db.prepareStatement(
                "SELECT 1 FROM nguoi_bi_chan WHERE (nguoi_dung_id=? AND doi_phuong_id=?) OR (nguoi_dung_id=? AND doi_phuong_id=?) LIMIT 1")) {
            lenh.setLong(1, gui);
            lenh.setLong(2, nhan);
            lenh.setLong(3, nhan);
            lenh.setLong(4, gui);
            try (ResultSet dong = lenh.executeQuery()) {
                if (dong.next()) {
                    throw new LoiNghiepVuModel("khongTheNhanTin",
                            "Bạn không thể gửi tin nhắn trong cuộc trò chuyện này.");
                }
            }
        }
    }

    public void xoaHoiThoai(long nguoi, long dich) throws SQLException {
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                try (PreparedStatement an = db.prepareStatement("""
                        INSERT IGNORE INTO tin_da_an(nguoi_dung_id,tin_nhan_id)
                        SELECT ?,id FROM tin_nhan WHERE (nguoi_gui_id=? AND nguoi_nhan_id=?) OR (nguoi_gui_id=? AND nguoi_nhan_id=?)
                        """)) {
                    an.setLong(1, nguoi);
                    an.setLong(2, nguoi);
                    an.setLong(3, dich);
                    an.setLong(4, dich);
                    an.setLong(5, nguoi);
                    an.executeUpdate();
                }
                try (PreparedStatement xoa = db.prepareStatement(
                        "DELETE FROM hoi_thoai_luu_tru WHERE nguoi_dung_id=? AND doi_phuong_id=?")) {
                    xoa.setLong(1, nguoi);
                    xoa.setLong(2, dich);
                    xoa.executeUpdate();
                }
                db.commit();
            } catch (SQLException | RuntimeException loi) {
                db.rollback();
                throw loi;
            }
        }
    }

    public void datLuuTru(long nguoi, long dich, boolean luu) throws SQLException {
        String sql = luu ? "INSERT IGNORE INTO hoi_thoai_luu_tru VALUES (?,?)" :
                "DELETE FROM hoi_thoai_luu_tru WHERE nguoi_dung_id=? AND doi_phuong_id=?";
        try (Connection db = csdl.moKetNoi(); PreparedStatement lenh = db.prepareStatement(sql)) {
            lenh.setLong(1, nguoi);
            lenh.setLong(2, dich);
            lenh.executeUpdate();
        }
    }

    public JsonArray hoiThoai(long nguoiDung, int boQua) throws SQLException {
        String sql = """
                SELECT t.*, n.ten_hien_thi,p.ma_luu,p.ten_tep,p.kich_thuoc,p.sha256,p.loai_tep,
                  CASE WHEN t.nguoi_nhan_id IS NULL THEN 0
                       WHEN t.nguoi_gui_id=? THEN t.nguoi_nhan_id ELSE t.nguoi_gui_id END AS dich_id,
                  COALESCE(d.ten_hien_thi, 'Phòng chat chung') AS ten_dich,
                  COALESCE(m.tin_cuoi_id,0) AS moc_da_doc, a_luu.doi_phuong_id IS NOT NULL AS da_luu_tru
                FROM tin_nhan t
                JOIN nguoi_dung n ON n.id=t.nguoi_gui_id
                LEFT JOIN tep_dinh_kem p ON p.tin_nhan_id=t.id
                LEFT JOIN nguoi_dung d ON d.id=CASE WHEN t.nguoi_gui_id=? THEN t.nguoi_nhan_id ELSE t.nguoi_gui_id END
                LEFT JOIN moc_doc m ON m.nguoi_dung_id=? AND m.doi_phuong_id=
                  CASE WHEN t.nguoi_nhan_id IS NULL THEN 0 WHEN t.nguoi_gui_id=? THEN t.nguoi_nhan_id ELSE t.nguoi_gui_id END
                LEFT JOIN hoi_thoai_luu_tru a_luu ON a_luu.nguoi_dung_id=? AND a_luu.doi_phuong_id=d.id
                WHERE (t.nguoi_nhan_id IS NULL OR t.nguoi_gui_id=? OR t.nguoi_nhan_id=?)
                  AND NOT EXISTS (SELECT 1 FROM tin_da_an a WHERE a.tin_nhan_id=t.id AND a.nguoi_dung_id=?)
                ORDER BY t.id
                """;
        Map<String, JsonObject> cacDich = new LinkedHashMap<>();
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement lenh = ketNoi.prepareStatement(sql)) {
            for (int viTri = 1; viTri <= 8; viTri++) {
                lenh.setLong(viTri, nguoiDung);
            }
            try (ResultSet dong = lenh.executeQuery()) {
                while (dong.next()) {
                    String id = dong.getString("dich_id");
                    JsonObject hoiThoai =
                            cacDich.computeIfAbsent(id, khoa -> DuLieu.tao("id", khoa, "chuaDoc", 0));
                    hoiThoai.addProperty("tenHienThi", dong.getString("ten_dich"));
                    hoiThoai.addProperty("daLuuTru", dong.getBoolean("da_luu_tru"));
                    JsonObject tin = docTin(dong);
                    tin.remove("tep");
                    tin.remove("maGui");
                    tin.remove("tenNguoiGui");
                    String noiDung = DuLieu.chu(tin, "noiDung");
                    tin.addProperty("noiDung", noiDung.substring(0, noiDung.offsetByCodePoints(0,
                            Math.min(60, noiDung.codePointCount(0, noiDung.length())))));
                    hoiThoai.add("tinCuoi", tin);
                    if (!dong.getBoolean("da_thu_hoi") && dong.getLong("nguoi_gui_id") != nguoiDung &&
                            dong.getLong("id") > dong.getLong("moc_da_doc")) {
                        hoiThoai.addProperty("chuaDoc", hoiThoai.get("chuaDoc").getAsLong() + 1);
                    }
                }
            }
        }
        JsonArray ketQua = new JsonArray();
        cacDich.values().stream().sorted(Comparator.<JsonObject>comparingLong(
                        dich -> Long.parseLong(DuLieu.chu(dich.getAsJsonObject("tinCuoi"), "id"))).reversed())
                .skip(boQua).limit(50).forEach(ketQua::add);
        return ketQua;
    }

    /**
     * Chi danh dau toi tin da hien thi, khong nuot tin moi den trong luc tai lich su.
     */
    public void danhDauDoc(long nguoiDung, Long doiPhuong, long tinCuoi) throws SQLException {
        String dieuKien = doiPhuong == null ? "nguoi_nhan_id IS NULL" :
                "((nguoi_gui_id=? AND nguoi_nhan_id=?) OR (nguoi_gui_id=? AND nguoi_nhan_id=?))";
        try (Connection ketNoi = csdl.moKetNoi(); PreparedStatement tim = ketNoi.prepareStatement(
                "SELECT id FROM tin_nhan WHERE id=? AND " + dieuKien)) {
            tim.setLong(1, tinCuoi);
            if (doiPhuong != null) {
                tim.setLong(2, nguoiDung);
                tim.setLong(3, doiPhuong);
                tim.setLong(4, doiPhuong);
                tim.setLong(5, nguoiDung);
            }
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("tinKhongHopLe",
                            "Tin nhắn không thuộc cuộc trò chuyện của bạn.");
                }
            }
            try (PreparedStatement lenh = ketNoi.prepareStatement(
                    "INSERT INTO moc_doc(nguoi_dung_id,doi_phuong_id,tin_cuoi_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE tin_cuoi_id=GREATEST(tin_cuoi_id,VALUES(tin_cuoi_id))")) {
                lenh.setLong(1, nguoiDung);
                lenh.setLong(2, doiPhuong == null ? 0 : doiPhuong);
                lenh.setLong(3, tinCuoi);
                lenh.executeUpdate();
            }
        }
    }

    public boolean daAnTin(long nguoi, long tin) throws SQLException {
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(
                "SELECT 1 FROM tin_da_an WHERE nguoi_dung_id=? AND tin_nhan_id=?")) {
            tim.setLong(1, nguoi);
            tim.setLong(2, tin);
            try (ResultSet dong = tim.executeQuery()) {
                return dong.next();
            }
        }
    }

    public void anTin(long nguoi, long tinId) throws SQLException {
        new TuongTacTinNhanDao(csdl).doc(tinId, nguoi); // Kiểm tra thuộc cuộc trò chuyện.
        try (Connection db = csdl.moKetNoi(); PreparedStatement them = db.prepareStatement(
                "INSERT IGNORE INTO tin_da_an(nguoi_dung_id,tin_nhan_id) VALUES(?,?)")) {
            them.setLong(1, nguoi);
            them.setLong(2, tinId);
            them.executeUpdate();
        }
    }

    static String loaiTinTuCSDL(String loaiTin) {
        return switch (loaiTin) {
            case "van_ban" -> "vanBan";
            case "cuoc_goi" -> "cuocGoi";
            default -> loaiTin;
        };
    }

    static JsonObject docTin(ResultSet dong) throws SQLException {
        Long nguoiNhan = dong.getObject("nguoi_nhan_id", Long.class);
        String thoiDiem = dong.getTimestamp("tao_luc").toLocalDateTime().toInstant(ZoneOffset.UTC).toString();
        JsonObject tin =
                DuLieu.tao("id", dong.getString("id"), "maGui", dong.getString("ma_gui"), "nguoiGuiId",
                        dong.getString("nguoi_gui_id"),
                        "nguoiNhanId", nguoiNhan == null ? null : nguoiNhan.toString(), "tenNguoiGui",
                        dong.getString("ten_hien_thi"), "noiDung", dong.getString("noi_dung"), "taoLuc",
                        thoiDiem, "loaiTin", loaiTinTuCSDL(dong.getString("loai_tin")));
        boolean daThuHoi = dong.getBoolean("da_thu_hoi");
        tin.addProperty("daThuHoi", daThuHoi);
        tin.addProperty("phienBan", dong.getString("phien_ban_tin"));
        tin.addProperty("traLoiId", dong.getString("tra_loi_id"));
        tin.add("camXuc", new JsonArray());
        tin.addProperty("camXucCuaToi", "");
        if (daThuHoi) {
            tin.addProperty("noiDung", "Tin nhắn đã được thu hồi");
        }
        if (!daThuHoi && "cuocGoi".equals(DuLieu.chu(tin, "loaiTin"))) {
            tin.addProperty("ketQuaGoi", dong.getString("ket_qua_goi"));
            tin.addProperty("thoiLuongGoi", dong.getLong("thoi_luong_goi"));
            tin.addProperty("goiVideo", dong.getBoolean("goi_video"));
        }
        if (!daThuHoi && dong.getString("ma_luu") != null) {
            tin.add("tep",
                    DuLieu.tao("tenTep", dong.getString("ten_tep"), "kichThuoc", dong.getLong("kich_thuoc"),
                            "sha256", dong.getString("sha256"), "loaiTep", dong.getString("loai_tep")));
        }
        return tin;
    }
}
