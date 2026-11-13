package server.hatang;

import server.model.KetQuaLuuModel;
import server.model.TepModel;
import server.model.LoiNghiepVuModel;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Một lần nhận đang dở cho mỗi socket; đóng socket sẽ xóa tệp .part.
 */
public final class BoNhanTep implements AutoCloseable {
    private final TepModel kho;
    private TepModel.ThongTin thongTin;
    private Path tepTam;
    private OutputStream boGhi;
    private MessageDigest bam;
    private long daNhan;

    public BoNhanTep(TepModel kho) {
        this.kho = kho;
    }

    public synchronized JsonObject batDau(long nguoiDung,
                                          JsonObject duLieu) throws SQLException, IOException {
        if (thongTin != null) {
            throw new LoiNghiepVuModel("dangNhanTep", "Đang có một tệp chưa hoàn tất.");
        }
        TepModel.ThongTin moi = kho.kiemTra(nguoiDung, duLieu);
        JsonObject daGui = kho.timDaGui(moi);
        if (daGui != null) {
            return DuLieu.tao("daCo", true, "tinNhan", daGui);
        }
        Path tam = kho.taoTam();
        try {
            boGhi = new BufferedOutputStream(Files.newOutputStream(tam));
            bam = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException | IOException loi) {
            Files.deleteIfExists(tam);
            throw new IOException("Không mở được tệp tạm.", loi);
        }
        tepTam = tam;
        thongTin = moi;
        daNhan = 0;
        return DuLieu.tao("daCo", false, "daNhan", "0");
    }

    public synchronized JsonObject nhanKhoi(long nguoiDung, JsonObject duLieu) throws IOException {
        canDangNhan(nguoiDung);
        try {
            long viTri = TepModel.so(duLieu, "viTri");
            String chu = DuLieu.chu(duLieu, "duLieu");
            if (chu.length() > 32768) {
                throw new IllegalArgumentException();
            }
            byte[] khoi = Base64.getDecoder().decode(chu);
            if (viTri != daNhan || khoi.length < 1 || khoi.length > TepModel.kichThuocKhoi ||
                    daNhan + khoi.length > thongTin.kichThuoc()) {
                throw new LoiNghiepVuModel("khoiSai", "Khối tệp sai vị trí hoặc vượt dung lượng khai báo.");
            }
            boGhi.write(khoi);
            bam.update(khoi);
            daNhan += khoi.length;
            return DuLieu.tao("daNhan", Long.toString(daNhan));
        } catch (IOException | RuntimeException loi) {
            close();
            throw loi;
        }
    }

    public synchronized KetQuaLuuModel ketThuc(long nguoiDung) throws IOException, SQLException {
        canDangNhan(nguoiDung);
        try {
            boGhi.close();
            boGhi = null;
            if (daNhan != thongTin.kichThuoc() ||
                    !HexFormat.of().formatHex(bam.digest()).equals(thongTin.sha())) {
                throw new LoiNghiepVuModel("tepKhongKhop", "Tệp chưa đầy đủ hoặc mã SHA-256 không khớp.");
            }
            return kho.hoanTat(thongTin, tepTam);
        } finally {
            close();
        }
    }

    private void canDangNhan(long nguoiDung) {
        if (thongTin == null || thongTin.nguoiGui() != nguoiDung) {
            throw new LoiNghiepVuModel("chuaGuiTep", "Chưa bắt đầu gửi tệp trên kết nối này.");
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (boGhi != null) {
                boGhi.close();
            }
        } catch (IOException loi) {/* Dọn tiếp tệp tạm. */}
        try {
            if (tepTam != null) {
                Files.deleteIfExists(tepTam);
            }
        } catch (IOException loi) {
            System.err.println("Chưa dọn được tệp truyền dở.");
        }
        boGhi = null;
        tepTam = null;
        thongTin = null;
        bam = null;
        daNhan = 0;
    }
}
