package server.dao;

import server.model.LoiNghiepVuModel;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.sql.*;

public final class ThongTinChatDao {
    private final CSDLDao csdl;

    public ThongTinChatDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public JsonObject doc(long nguoi, Long dich, String loai, Long truoc) throws SQLException {
        String dk = dich == null ? "t.nguoi_nhan_id IS NULL" :
                "((t.nguoi_gui_id=? AND t.nguoi_nhan_id=?) OR (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?))";
        String loc = switch (loai) {
            case "ghim" -> " AND EXISTS(SELECT 1 FROM tin_ghim g WHERE g.tin_nhan_id=t.id)";
            case "anh" -> " AND t.loai_tin='anh'";
            case "tepRieng" -> " AND t.loai_tin='tep'";
            case "lienKet" -> " AND (t.noi_dung LIKE '%https://%' OR t.noi_dung LIKE '%http://%')";
            default -> " AND t.loai_tin IN ('anh','tep')";
        };
        JsonArray tin = new JsonArray();
        long daDoc = 0;
        try (Connection db = csdl.moKetNoi()) {
            if (dich != null) {
                try (PreparedStatement q = db.prepareStatement(
                        "SELECT tin_cuoi_id FROM moc_doc WHERE nguoi_dung_id=? AND doi_phuong_id=?")) {
                    q.setLong(1, dich);
                    q.setLong(2, nguoi);
                    try (ResultSet d = q.executeQuery()) {
                        if (d.next()) {
                            daDoc = d.getLong(1);
                        }
                    }
                }
            }
            Long maGhim = null;
            if (loai.equals("ghim")) {
                String tim =
                        "SELECT g.tin_nhan_id FROM tin_ghim g JOIN tin_nhan t ON t.id=g.tin_nhan_id WHERE " +
                                dk +
                                " ORDER BY g.ghim_luc DESC,t.id DESC LIMIT 1";
                try (PreparedStatement q = db.prepareStatement(tim)) {
                    ganCap(q, nguoi, dich);
                    try (ResultSet d = q.executeQuery()) {
                        if (d.next()) {
                            maGhim = d.getLong(1);
                        }
                    }
                }
                if (maGhim == null) {
                    return DuLieu.tao("tinNhan", tin, "daDocId", Long.toString(daDoc));
                }
            }
            String locTin = maGhim == null ? dk + loc : "t.id=?";
            String sql = TinNhanDao.chonTin + "WHERE " + locTin +
                    " AND t.da_thu_hoi=FALSE AND NOT EXISTS(SELECT 1 FROM tin_da_an a WHERE a.tin_nhan_id=t.id AND a.nguoi_dung_id=?)" +
                    (truoc == null || maGhim != null ? "" : " AND t.id<?") + " ORDER BY t.id DESC LIMIT 20";
            try (PreparedStatement q = db.prepareStatement(sql)) {
                int i = 1;
                if (maGhim != null) {
                    q.setLong(i++, maGhim);
                } else if (dich != null) {
                    ganCap(q, nguoi, dich);
                    i = 5;
                }
                q.setLong(i++, nguoi);
                if (truoc != null && maGhim == null) {
                    q.setLong(i, truoc);
                }
                try (ResultSet d = q.executeQuery()) {
                    while (d.next()) {
                        JsonObject t = TinNhanDao.docTin(d);
                        if (t.get("daThuHoi").getAsBoolean()) {
                            continue;
                        }
                        String chu = DuLieu.chu(t, "noiDung");
                        t.addProperty("noiDung", TuongTacTinNhanDao.rutGon(chu));
                        tin.add(t);
                    }
                }
            }
        }
        JsonObject ghim = loai.equals("ghim") && !tin.isEmpty() ? tin.get(0).getAsJsonObject() : null;
        return DuLieu.tao("tinNhan", tin, "daDocId", Long.toString(daDoc), "ghimMoi", ghim);
    }

    public void ghim(long nguoi, long id, boolean bo) throws SQLException {
        JsonObject tin = new TuongTacTinNhanDao(csdl).doc(id, nguoi);
        if (tin.get("daThuHoi").getAsBoolean()) {
            throw new LoiNghiepVuModel("tinDaThuHoi", "Tin đã thu hồi.");
        }
        Long nhan = DuLieu.id(tin, "nguoiNhanId", true);
        long gui = DuLieu.id(tin, "nguoiGuiId", false);
        String cap = nhan == null ? "t.nguoi_nhan_id IS NULL" :
                "((t.nguoi_gui_id=? AND t.nguoi_nhan_id=?) OR (t.nguoi_gui_id=? AND t.nguoi_nhan_id=?))";
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                if (bo) {
                    try (PreparedStatement tim = db.prepareStatement(
                            "SELECT g.tin_nhan_id FROM tin_ghim g JOIN tin_nhan t ON t.id=g.tin_nhan_id WHERE " +
                                    cap + " ORDER BY g.ghim_luc DESC,t.id DESC LIMIT 1")) {
                        ganCap(tim, gui, nhan);
                        try (ResultSet dong = tim.executeQuery()) {
                            if (!dong.next() || dong.getLong(1) != id) {
                                db.rollback();
                                return;
                            }
                        }
                    }
                }
                try (PreparedStatement xoa = db.prepareStatement(
                        "DELETE g FROM tin_ghim g JOIN tin_nhan t ON t.id=g.tin_nhan_id WHERE " + cap)) {
                    ganCap(xoa, gui, nhan);
                    xoa.executeUpdate();
                }
                if (!bo) {
                    try (PreparedStatement them = db.prepareStatement(
                            "INSERT INTO tin_ghim(tin_nhan_id,nguoi_ghim_id) VALUES(?,?)")) {
                        them.setLong(1, id);
                        them.setLong(2, nguoi);
                        them.executeUpdate();
                    }
                }
                db.commit();
            } catch (SQLException | RuntimeException loi) {
                db.rollback();
                throw loi;
            }
        }
    }

    private void ganCap(PreparedStatement lenh, long gui, Long nhan) throws SQLException {
        if (nhan == null) {
            return;
        }
        lenh.setLong(1, gui);
        lenh.setLong(2, nhan);
        lenh.setLong(3, nhan);
        lenh.setLong(4, gui);
    }
}
