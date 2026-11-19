package server.dao;

import server.model.LoiNghiepVuModel;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.sql.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

/**
 * Quyền tham gia, trả lời, thu hồi và cảm xúc đều kiểm tra trên server.
 */
public final class TuongTacTinNhanDao {
    public static final Set<String> cacCamXuc = Set.of("👍", "❤️", "😂", "😮", "😢", "😡");
    private final CSDLDao csdl;

    public TuongTacTinNhanDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public static String bamChu(String chu) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(chu.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException loi) {
            throw new IllegalStateException(loi);
        }
    }

    static void kiemTraTraLoi(Connection db, long nguoiGui, Long nguoiNhan, Long traLoi) throws SQLException {
        if (traLoi == null) {
            return;
        }
        try (PreparedStatement tim = db.prepareStatement(
                "SELECT nguoi_gui_id,nguoi_nhan_id,da_thu_hoi FROM tin_nhan WHERE id=? FOR UPDATE")) {
            tim.setLong(1, traLoi);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next() || !cungHoiThoai(nguoiGui, nguoiNhan, dong.getLong("nguoi_gui_id"),
                        dong.getObject("nguoi_nhan_id", Long.class))) {
                    throw new LoiNghiepVuModel("traLoiSai",
                            "Tin được trả lời không thuộc cuộc trò chuyện này.");
                }
                if (dong.getBoolean("da_thu_hoi")) {
                    throw new LoiNghiepVuModel("tinDaThuHoi",
                            "Tin gốc đã được thu hồi. Hãy bỏ trích dẫn trước khi gửi.");
                }
            }
        }
    }

    private static boolean cungHoiThoai(long guiA, Long nhanA, long guiB, Long nhanB) {
        if (nhanA == null || nhanB == null) {
            return nhanA == null && nhanB == null;
        }
        return (guiA == guiB && nhanA.equals(nhanB)) || (guiA == nhanB && nhanA == guiB);
    }

    private boolean duocDoc(long nguoi, long gui, Long nhan) {
        return nhan == null || nguoi == gui || nguoi == nhan;
    }

    public JsonObject doc(long tinId, long nguoi) throws SQLException {
        JsonObject tin;
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(TinNhanDao.chonTin +
                "WHERE t.id=? AND (t.nguoi_nhan_id IS NULL OR t.nguoi_gui_id=? OR t.nguoi_nhan_id=?)")) {
            tim.setLong(1, tinId);
            tim.setLong(2, nguoi);
            tim.setLong(3, nguoi);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("tinKhongHopLe",
                            "Tin không tồn tại hoặc bạn không thuộc cuộc trò chuyện.");
                }
                tin = TinNhanDao.docTin(dong);
            }
        }
        boSung(List.of(tin), nguoi);
        return tin;
    }

    public void thuHoi(long nguoi, long tinId) throws SQLException {
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                try (PreparedStatement tim = db.prepareStatement(
                        "SELECT nguoi_gui_id,da_thu_hoi,loai_tin FROM tin_nhan WHERE id=? FOR UPDATE")) {
                    tim.setLong(1, tinId);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (!dong.next() || dong.getLong("nguoi_gui_id") != nguoi) {
                            throw new LoiNghiepVuModel("khongDuocThuHoi",
                                    "Bạn chỉ được thu hồi tin do mình gửi.");
                        }
                        if ("cuoc_goi".equals(dong.getString("loai_tin"))) {
                            throw new LoiNghiepVuModel("khongTheThuHoiCuocGoi",
                                    "Chỉ có thể xóa mục cuộc gọi ở phía bạn.");
                        }
                        if (dong.getBoolean("da_thu_hoi")) {
                            db.commit();
                            return;
                        }
                    }
                }
                try (PreparedStatement lenh = db.prepareStatement(
                        "UPDATE tin_nhan SET da_thu_hoi=TRUE,noi_dung='',phien_ban_tin=phien_ban_tin+1 WHERE id=?")) {
                    lenh.setLong(1, tinId);
                    lenh.executeUpdate();
                }
                try (PreparedStatement lenh = db.prepareStatement(
                        "DELETE FROM cam_xuc_tin_nhan WHERE tin_nhan_id=?")) {
                    lenh.setLong(1, tinId);
                    lenh.executeUpdate();
                }
                try (PreparedStatement lenh = db.prepareStatement(
                        "UPDATE tep_dinh_kem SET anh_thu=NULL WHERE tin_nhan_id=?")) {
                    lenh.setLong(1, tinId);
                    lenh.executeUpdate();
                }
                db.commit();
            } catch (SQLException | RuntimeException loi) {
                db.rollback();
                throw loi;
            }
        }
    }

    public void datCamXuc(long nguoi, long tinId, String bieuCam) throws SQLException {
        if (!bieuCam.isEmpty() && !cacCamXuc.contains(bieuCam)) {
            throw new LoiNghiepVuModel("camXucSai", "Chọn cảm xúc được hỗ trợ.");
        }
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                try (PreparedStatement tim = db.prepareStatement(
                        "SELECT nguoi_gui_id,nguoi_nhan_id,da_thu_hoi FROM tin_nhan WHERE id=? FOR UPDATE")) {
                    tim.setLong(1, tinId);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (!dong.next() || !duocDoc(nguoi, dong.getLong("nguoi_gui_id"),
                                dong.getObject("nguoi_nhan_id", Long.class))) {
                            throw new LoiNghiepVuModel("tinKhongHopLe",
                                    "Bạn không thuộc cuộc trò chuyện này.");
                        }
                        if (dong.getBoolean("da_thu_hoi")) {
                            throw new LoiNghiepVuModel("tinDaThuHoi",
                                    "Không thể thả cảm xúc lên tin đã thu hồi.");
                        }
                    }
                }
                String cu = "";
                try (PreparedStatement tim = db.prepareStatement(
                        "SELECT bieu_cam FROM cam_xuc_tin_nhan WHERE tin_nhan_id=? AND nguoi_dung_id=?")) {
                    tim.setLong(1, tinId);
                    tim.setLong(2, nguoi);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (dong.next()) {
                            cu = dong.getString(1);
                        }
                    }
                }
                if (!cu.equals(bieuCam)) {
                    try (PreparedStatement lenh = db.prepareStatement(bieuCam.isEmpty() ?
                            "DELETE FROM cam_xuc_tin_nhan WHERE tin_nhan_id=? AND nguoi_dung_id=?" :
                            "INSERT INTO cam_xuc_tin_nhan(tin_nhan_id,nguoi_dung_id,bieu_cam) VALUES(?,?,?) ON DUPLICATE KEY UPDATE bieu_cam=VALUES(bieu_cam)")) {
                        lenh.setLong(1, tinId);
                        lenh.setLong(2, nguoi);
                        if (!bieuCam.isEmpty()) {
                            lenh.setString(3, bieuCam);
                        }
                        lenh.executeUpdate();
                    }
                    try (PreparedStatement lenh = db.prepareStatement(
                            "UPDATE tin_nhan SET phien_ban_tin=phien_ban_tin+1 WHERE id=?")) {
                        lenh.setLong(1, tinId);
                        lenh.executeUpdate();
                    }
                }
                db.commit();
            } catch (SQLException | RuntimeException loi) {
                db.rollback();
                throw loi;
            }
        }
    }

    /**
     * Đọc thêm theo lô, không tạo một kết nối SQL cho từng bong bóng tin.
     */
    public void boSung(Collection<JsonObject> cacTin, long nguoi) throws SQLException {
        if (cacTin.isEmpty()) {
            return;
        }
        Map<Long, JsonObject> theoId = new LinkedHashMap<>();
        Set<Long> tinGoc = new HashSet<>();
        for (JsonObject tin : cacTin) {
            theoId.put(Long.parseLong(DuLieu.chu(tin, "id")), tin);
            tin.add("camXuc", new JsonArray());
            tin.addProperty("camXucCuaToi", "");
            if (!tin.get("daThuHoi").getAsBoolean() && !DuLieu.chu(tin, "traLoiId").isEmpty()) {
                tinGoc.add(Long.parseLong(DuLieu.chu(tin, "traLoiId")));
            }
        }
        try (Connection db = csdl.moKetNoi()) {
            String dau = String.join(",", Collections.nCopies(theoId.size(), "?"));
            try (PreparedStatement tim = db.prepareStatement(
                    "SELECT tin_nhan_id,bieu_cam,COUNT(*) so_luong,MAX(nguoi_dung_id=?) cua_toi FROM cam_xuc_tin_nhan WHERE tin_nhan_id IN (" +
                            dau + ") GROUP BY tin_nhan_id,bieu_cam ORDER BY bieu_cam")) {
                tim.setLong(1, nguoi);
                int i = 2;
                for (long id : theoId.keySet()) {
                    tim.setLong(i++, id);
                }
                try (ResultSet dong = tim.executeQuery()) {
                    while (dong.next()) {
                        JsonObject tin = theoId.get(dong.getLong("tin_nhan_id"));
                        if (tin.get("daThuHoi").getAsBoolean()) {
                            continue;
                        }
                        String cam = dong.getString("bieu_cam");
                        tin.getAsJsonArray("camXuc")
                                .add(DuLieu.tao("bieuCam", cam, "soLuong", dong.getLong("so_luong")));
                        if (dong.getBoolean("cua_toi")) {
                            tin.addProperty("camXucCuaToi", cam);
                        }
                    }
                }
            }
            Map<Long, JsonObject> cacTinGoc = new HashMap<>();
            if (!tinGoc.isEmpty()) {
                dau = String.join(",", Collections.nCopies(tinGoc.size(), "?"));
                try (PreparedStatement tim = db.prepareStatement(
                        "SELECT t.id,t.nguoi_gui_id,t.nguoi_nhan_id,t.noi_dung,t.loai_tin,t.da_thu_hoi,t.phien_ban_tin,n.ten_hien_thi FROM tin_nhan t JOIN nguoi_dung n ON n.id=t.nguoi_gui_id WHERE t.id IN (" +
                                dau +
                                ") AND NOT EXISTS (SELECT 1 FROM tin_da_an a WHERE a.tin_nhan_id=t.id AND a.nguoi_dung_id=?)")) {
                    int i = 1;
                    for (long id : tinGoc) {
                        tim.setLong(i++, id);
                    }
                    tim.setLong(i, nguoi);
                    try (ResultSet dong = tim.executeQuery()) {
                        while (dong.next()) {
                            boolean daThu = dong.getBoolean("da_thu_hoi");
                            String chu = daThu ? "Tin nhắn đã được thu hồi" : dong.getString("noi_dung");
                            cacTinGoc.put(dong.getLong("id"),
                                    DuLieu.tao("id", dong.getString("id"), "nguoiGuiId",
                                            dong.getString("nguoi_gui_id"), "nguoiNhanId",
                                            dong.getString("nguoi_nhan_id"), "tenNguoiGui",
                                            dong.getString("ten_hien_thi"), "noiDung", rutGon(chu),
                                            "loaiTin", daThu ? "vanBan" :
                                                    TinNhanDao.loaiTinTuCSDL(dong.getString("loai_tin")),
                                            "daThuHoi", daThu, "phienBan",
                                            dong.getString("phien_ban_tin")));
                        }
                    }
                }
            }
            for (JsonObject tin : cacTin) {
                String ma = DuLieu.chu(tin, "traLoiId");
                if (ma.isEmpty() || tin.get("daThuHoi").getAsBoolean()) {
                    continue;
                }
                JsonObject goc = cacTinGoc.get(Long.parseLong(ma));
                Long nhan = DuLieu.id(tin, "nguoiNhanId", true);
                if (goc != null && cungHoiThoai(Long.parseLong(DuLieu.chu(tin, "nguoiGuiId")), nhan,
                        Long.parseLong(DuLieu.chu(goc, "nguoiGuiId")),
                        DuLieu.id(goc, "nguoiNhanId", true))) {
                    JsonObject trich = goc.deepCopy();
                    trich.remove("nguoiNhanId");
                    tin.add("trichDan", trich);
                } else {
                    tin.add("trichDan",
                            DuLieu.tao("id", ma, "tenNguoiGui", "", "noiDung", "Tin gốc không còn",
                                    "daThuHoi", true, "phienBan", "0"));
                }
            }
        }
    }

    public static String rutGon(String chu) {
        return chu.substring(0,
                chu.offsetByCodePoints(0, Math.min(120, chu.codePointCount(0, chu.length()))));
    }
}
