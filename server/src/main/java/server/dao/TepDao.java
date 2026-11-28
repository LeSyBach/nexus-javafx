package server.dao;

import com.google.gson.JsonObject;
import server.model.KetQuaLuuModel;
import server.model.LoiNghiepVuModel;
import server.model.TepModel.ThongTin;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.Objects;

public final class TepDao {
    private final CSDLDao csdl;

    public TepDao(CSDLDao csdl) {
        this.csdl = csdl;
    }

    public void kiemTraNguoiNhan(long nguoiGui, long dich) throws SQLException {
        try (Connection db = csdl.moKetNoi();
             PreparedStatement tim = db.prepareStatement("SELECT id FROM nguoi_dung WHERE id=?")) {
            tim.setLong(1, dich);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("khongCoNguoiNhan", "Không tìm thấy người nhận.");
                }
            }
            TinNhanDao.kiemTraGuiRieng(db, nguoiGui, dich);
        }
    }

    public JsonObject timDaGui(ThongTin thongTin) throws SQLException {
        try (Connection db = csdl.moKetNoi()) {
            return timDaGui(db, thongTin);
        }
    }

    private JsonObject timDaGui(Connection db, ThongTin thongTin) throws SQLException {
        try (PreparedStatement tim = db.prepareStatement(
                TinNhanDao.chonTin + "WHERE t.nguoi_gui_id=? AND t.ma_gui=?")) {
            tim.setLong(1, thongTin.nguoiGui());
            tim.setString(2, thongTin.maGui());
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    return null;
                }
                JsonObject tin = TinNhanDao.docTin(dong);
                if (dong.getString("ma_luu") == null ||
                        !Objects.equals(dong.getObject("nguoi_nhan_id", Long.class), thongTin.nguoiNhan()) ||
                        !Objects.equals(dong.getObject("tra_loi_id", Long.class), thongTin.traLoi()) ||
                        !TuongTacTinNhanDao.bamChu(thongTin.noiDung())
                                .equals(dong.getString("bam_noi_dung")) ||
                        !dong.getString("ten_tep").equals(thongTin.ten()) ||
                        thongTin.tinThoai() != "thoai".equals(dong.getString("loai_tep")) ||
                        dong.getLong("kich_thuoc") != thongTin.kichThuoc() ||
                        !dong.getString("sha256").equals(thongTin.sha())) {
                    throw new LoiNghiepVuModel("maGuiDaDung", "Mã gửi đã dùng cho tin khác.");
                }
                return tin;
            }
        }
    }

    public KetQuaLuuModel hoanTat(ThongTin thongTin, Path tam, Path dich,
                                         String maLuu, byte[] anhThu) throws SQLException, IOException {
        String loai = anhThu == null ? "tep" : "anh";
        boolean giuTep = false;
        try (Connection db = csdl.moKetNoi()) {
            db.setAutoCommit(false);
            try {
                // Tuần tự hóa gửi trùng cùng tài khoản trước khi kiểm tra ma_gui.
                try (PreparedStatement khoa = db.prepareStatement(
                        "SELECT id FROM nguoi_dung WHERE id=? FOR UPDATE")) {
                    khoa.setLong(1, thongTin.nguoiGui());
                    try (ResultSet dong = khoa.executeQuery()) {
                        if (!dong.next()) {
                            throw new SQLException("Tài khoản không còn tồn tại.");
                        }
                    }
                }
                JsonObject daGui = timDaGui(db, thongTin);
                if (daGui != null) {
                    db.commit();
                    return new KetQuaLuuModel(daGui, false);
                }
                TinNhanDao.kiemTraGuiRieng(db, thongTin.nguoiGui(), thongTin.nguoiNhan());
                TuongTacTinNhanDao.kiemTraTraLoi(db, thongTin.nguoiGui(), thongTin.nguoiNhan(),
                        thongTin.traLoi());
                Files.move(tam, dich, StandardCopyOption.ATOMIC_MOVE);
                long id;
                try (PreparedStatement them = db.prepareStatement(
                        "INSERT INTO tin_nhan(ma_gui,nguoi_gui_id,nguoi_nhan_id,noi_dung,loai_tin,tra_loi_id,bam_noi_dung) VALUES(?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    them.setString(1, thongTin.maGui());
                    them.setLong(2, thongTin.nguoiGui());
                    if (thongTin.nguoiNhan() == null) {
                        them.setNull(3, Types.BIGINT);
                    } else {
                        them.setLong(3, thongTin.nguoiNhan());
                    }
                    them.setString(4, thongTin.noiDung());
                    them.setString(5, loai);
                    them.setObject(6, thongTin.traLoi(), Types.BIGINT);
                    them.setString(7, TuongTacTinNhanDao.bamChu(thongTin.noiDung()));
                    them.executeUpdate();
                    try (ResultSet dong = them.getGeneratedKeys()) {
                        if (!dong.next()) {
                            throw new SQLException("Không lấy được mã tin.");
                        }
                        id = dong.getLong(1);
                    }
                }
                try (PreparedStatement them = db.prepareStatement(
                        "INSERT INTO tep_dinh_kem(tin_nhan_id,ma_luu,ten_tep,kich_thuoc,sha256,loai_tep,anh_thu) VALUES(?,?,?,?,?,?,?)")) {
                    them.setLong(1, id);
                    them.setString(2, maLuu);
                    them.setString(3, thongTin.ten());
                    them.setLong(4, thongTin.kichThuoc());
                    them.setString(5, thongTin.sha());
                    them.setString(6, thongTin.tinThoai() ? "thoai" : loai);
                    them.setBytes(7, anhThu);
                    them.executeUpdate();
                }
                JsonObject tin;
                try (PreparedStatement tim = db.prepareStatement(TinNhanDao.chonTin + "WHERE t.id=?")) {
                    tim.setLong(1, id);
                    try (ResultSet dong = tim.executeQuery()) {
                        if (!dong.next()) {
                            throw new SQLException();
                        }
                        tin = TinNhanDao.docTin(dong);
                    }
                }
                db.commit();
                giuTep = true;
                return new KetQuaLuuModel(tin, true);
            } catch (SQLException | IOException | RuntimeException loi) {
                db.rollback();
                throw loi;
            }
        } finally {
            if (!giuTep) {
                Files.deleteIfExists(dich);
            }
            Files.deleteIfExists(tam);
        }
    }

    public String maTepDaThuHoi(long tinId) throws SQLException {
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(
                "SELECT p.ma_luu FROM tep_dinh_kem p JOIN tin_nhan t ON t.id=p.tin_nhan_id WHERE t.id=? AND t.da_thu_hoi=TRUE")) {
            tim.setLong(1, tinId);
            try (ResultSet dong = tim.executeQuery()) {
                return dong.next() ? dong.getString(1) : null;
            }
        }
    }

    public record TepDaLuu(String ma, long kichThuoc, String sha, String ten, String loai, byte[] anhThu) {
    }

    public TepDaLuu docTepDuocPhep(long nguoiDung, long tinId, boolean layAnh) throws SQLException {
        String sql = "SELECT p.ma_luu,p.ten_tep,p.kich_thuoc,p.sha256,p.loai_tep" + (layAnh ? ",p.anh_thu" : "") +
                " FROM tep_dinh_kem p JOIN tin_nhan t ON t.id=p.tin_nhan_id WHERE t.id=? AND t.da_thu_hoi=FALSE AND (t.nguoi_nhan_id IS NULL OR t.nguoi_gui_id=? OR t.nguoi_nhan_id=?) AND NOT EXISTS (SELECT 1 FROM tin_da_an a WHERE a.nguoi_dung_id=? AND a.tin_nhan_id=t.id)";
        try (Connection db = csdl.moKetNoi(); PreparedStatement tim = db.prepareStatement(sql)) {
            tim.setLong(1, tinId);
            tim.setLong(2, nguoiDung);
            tim.setLong(3, nguoiDung);
            tim.setLong(4, nguoiDung);
            try (ResultSet dong = tim.executeQuery()) {
                if (!dong.next()) {
                    throw new LoiNghiepVuModel("khongCoQuyenTai",
                            "Tệp không tồn tại hoặc bạn không thuộc cuộc trò chuyện.");
                }
                return new TepDaLuu(dong.getString("ma_luu"), dong.getLong("kich_thuoc"),
                        dong.getString("sha256"), dong.getString("ten_tep"), dong.getString("loai_tep"),
                        layAnh ? dong.getBytes("anh_thu") : null);
            }
        }
    }
}
