package server.model;

import com.google.gson.*;
import giaothuc.DuLieu;
import server.dao.CSDLDao;
import server.dao.TepDao;

import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.image.BufferedImage;

/**
 * Lưu bytes trên ổ đĩa, metadata gắn với tin nhắn trong MariaDB.
 */
public final class TepModel {
    public static final int kichThuocKhoi = 24576;
    public static final long gioiHanTep = 10L * 1024 * 1024;
    private static final int SO_KHUNG_THOAI = 16_000 * 60;
    private static final long GIOI_HAN_TIN_THOAI = 2_000_000;
    private final TepDao tepDao;
    private final Path thuMuc;
    public TepModel(CSDLDao csdl, Path thuMuc) throws IOException {
        this.tepDao = new TepDao(csdl);
        this.thuMuc = thuMuc.toAbsolutePath().normalize();
        Files.createDirectories(this.thuMuc);
    }

    public record ThongTin(long nguoiGui, Long nguoiNhan, String maGui, String ten, long kichThuoc,
                           String sha, String noiDung, Long traLoi, boolean tinThoai) {
    }

    public Path taoTam() throws IOException {
        return Files.createTempFile(thuMuc, "dang-nhan-", ".part");
    }

    public ThongTin kiemTra(long nguoiGui, JsonObject duLieu) throws SQLException {
        String ten = DuLieu.chu(duLieu, "tenTep"), ma = DuLieu.chu(duLieu, "maGui"), sha =
                DuLieu.chu(duLieu, "sha256"), noiDung = DuLieu.chu(duLieu, "noiDung").strip();
        long kichThuoc = so(duLieu, "kichThuoc");
        Long dich = DuLieu.id(duLieu, "nguoiNhanId", true);
        var coTinThoai = duLieu.get("tinThoai");
        if (coTinThoai != null && (!coTinThoai.isJsonPrimitive() || !coTinThoai.getAsJsonPrimitive().isBoolean())) {
            throw new IllegalArgumentException();
        }
        boolean tinThoai = coTinThoai != null && coTinThoai.getAsBoolean();
        if (ten.isBlank() || ten.length() > 180 || ten.equals(".") || ten.equals("..") ||
                ten.indexOf('/') >= 0 || ten.indexOf('\\') >= 0 ||
                ten.chars().anyMatch(Character::isISOControl)) {
            throw new LoiNghiepVuModel("tenTepSai", "Tên tệp không hợp lệ (tối đa 180 ký tự).");
        }
        if (kichThuoc > gioiHanTep) {
            throw new LoiNghiepVuModel("tepQuaLon", "Tệp tối đa 10 MB.");
        }
        if (tinThoai && (kichThuoc <= 44 || kichThuoc > GIOI_HAN_TIN_THOAI
                || !ten.toLowerCase(Locale.ROOT).endsWith(".wav"))) {
            throw tinThoaiSai();
        }
        if (!ma.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}") || !sha.matches("[a-f0-9]{64}") ||
                noiDung.length() > 4000) {
            throw new IllegalArgumentException();
        }
        if (dich != null) {
            if (dich == nguoiGui) {
                throw new LoiNghiepVuModel("nguoiNhanSai", "Chọn người khác để gửi tệp.");
            }
            tepDao.kiemTraNguoiNhan(nguoiGui, dich);
        }
        String macDinh = tinThoai ? "Tin nhắn thoại" : "Tệp: " + ten;
        return new ThongTin(nguoiGui, dich, ma, ten, kichThuoc, sha,
                noiDung.isEmpty() ? macDinh : noiDung, DuLieu.id(duLieu, "traLoiId", true), tinThoai);
    }

    public static long so(JsonObject duLieu, String ten) {
        String chu = DuLieu.chu(duLieu, ten);
        if (!chu.matches("[0-9]{1,9}")) {
            throw new IllegalArgumentException();
        }
        return Long.parseLong(chu);
    }

    public JsonObject timDaGui(ThongTin thongTin) throws SQLException {
        return tepDao.timDaGui(thongTin);
    }

    public KetQuaLuuModel hoanTat(ThongTin thongTin, Path tam) throws SQLException, IOException {
        if (thongTin.tinThoai()) {
            kiemTraTinThoai(tam);
        }
        byte[] anhThu = thongTin.tinThoai() ? null : taoAnhThu(tam);
        String maLuu = UUID.randomUUID().toString();
        return tepDao.hoanTat(thongTin, tam, duongDan(maLuu), maLuu, anhThu);
    }

    private void kiemTraTinThoai(Path tep) throws IOException {
        try {
            var thongTin = AudioSystem.getAudioFileFormat(tep.toFile());
            if (!AudioFileFormat.Type.WAVE.equals(thongTin.getType())) {
                throw tinThoaiSai();
            }
            try (var vao = AudioSystem.getAudioInputStream(tep.toFile())) {
                AudioFormat dinhDang = vao.getFormat();
                long soKhung = vao.getFrameLength();
                if (!AudioFormat.Encoding.PCM_SIGNED.equals(dinhDang.getEncoding())
                        || dinhDang.getSampleRate() != 16_000 || dinhDang.getChannels() != 1
                        || dinhDang.getSampleSizeInBits() != 16 || dinhDang.getFrameSize() != 2 || dinhDang.isBigEndian()
                        || soKhung <= 0 || soKhung > SO_KHUNG_THOAI
                        || vao.transferTo(OutputStream.nullOutputStream()) != soKhung * 2) {
                    throw tinThoaiSai();
                }
            }
        } catch (UnsupportedAudioFileException loi) {
            throw tinThoaiSai();
        }
    }

    private LoiNghiepVuModel tinThoaiSai() {
        return new LoiNghiepVuModel("tinThoaiSai", "Đoạn ghi âm phải là WAV hợp lệ, dài tối đa 60 giây.");
    }

    /**
     * Quyền tải đã bị chặn trong SQL; dọn bytes sau khi thu hồi đã commit.
     */
    public void xoaSauThuHoi(long tinId) throws SQLException {
        String ma = tepDao.maTepDaThuHoi(tinId);
        if (ma != null) {
            try {
                Files.deleteIfExists(duongDan(ma));
            } catch (IOException loi) {
                System.err.println("Chưa dọn được bytes của tệp đã thu hồi.");
            }
        }
    }

    private Path duongDan(String ma) throws IOException {
        if (!ma.matches("[a-f0-9-]{36}")) {
            throw new IOException("Mã tệp lưu không hợp lệ.");
        }
        Path tep = thuMuc.resolve(ma + ".bin").normalize();
        if (!tep.getParent().equals(thuMuc)) {
            throw new IOException();
        }
        return tep;
    }

    private record TepDuocPhep(Path tep, long kichThuoc, String sha, String ten, String loai, byte[] anhThu) {
    }

    private TepDuocPhep choPhep(long nguoiDung, long tinId, boolean layAnh) throws SQLException, IOException {
        var duLieu = tepDao.docTepDuocPhep(nguoiDung, tinId, layAnh);
        Path tep = duongDan(duLieu.ma());
        if (!Files.isRegularFile(tep) || Files.size(tep) != duLieu.kichThuoc()) {
            throw new LoiNghiepVuModel("tepKhongCon", "Tệp trên máy chủ không còn đầy đủ.");
        }
        return new TepDuocPhep(tep, duLieu.kichThuoc(), duLieu.sha(), duLieu.ten(), duLieu.loai(), duLieu.anhThu());
    }

    public KetQuaLuuModel chuyenTiep(long nguoi, long tinId, JsonObject duLieu,
                                            String noiDung) throws SQLException, IOException {
        var tep = choPhep(nguoi, tinId, false);
        JsonObject khai = duLieu.deepCopy();
        khai.addProperty("tenTep", tep.ten());
        khai.addProperty("kichThuoc", Long.toString(tep.kichThuoc()));
        khai.addProperty("sha256", tep.sha());
        khai.addProperty("noiDung", noiDung);
        khai.addProperty("tinThoai", tep.loai().equals("thoai"));
        khai.remove("traLoiId");
        ThongTin thongTin = kiemTra(nguoi, khai);
        JsonObject cu = timDaGui(thongTin);
        if (cu != null) {
            return new KetQuaLuuModel(cu, false);
        }
        Path tam = taoTam();
        try {
            Files.copy(tep.tep(), tam, StandardCopyOption.REPLACE_EXISTING);
            return hoanTat(thongTin, tam);
        } finally {
            Files.deleteIfExists(tam);
        }
    }

    public JsonObject thongTinTai(long nguoiDung, long tinId) throws SQLException, IOException {
        var tep = choPhep(nguoiDung, tinId, false);
        return DuLieu.tao("tenTep", tep.ten(), "kichThuoc", Long.toString(tep.kichThuoc()), "sha256",
                tep.sha());
    }

    public JsonObject docKhoi(long nguoiDung, long tinId, long viTri) throws SQLException, IOException {
        var tep = choPhep(nguoiDung, tinId, false);
        if (viTri < 0 || viTri > tep.kichThuoc()) {
            throw new LoiNghiepVuModel("viTriSai", "Vị trí khối không hợp lệ.");
        }
        byte[] khoi = new byte[(int) Math.min(kichThuocKhoi, tep.kichThuoc() - viTri)];
        try (RandomAccessFile doc = new RandomAccessFile(tep.tep().toFile(), "r")) {
            doc.seek(viTri);
            doc.readFully(khoi);
        }
        return DuLieu.tao("viTri", Long.toString(viTri), "duLieu",
                Base64.getEncoder().encodeToString(khoi));
    }

    public JsonObject anhThu(long nguoiDung, long tinId) throws SQLException, IOException {
        var tep = choPhep(nguoiDung, tinId, true);
        if (tep.anhThu() == null) {
            throw new LoiNghiepVuModel("khongCoAnhThu", "Tệp này không có ảnh xem trước.");
        }
        return DuLieu.tao("anh", Base64.getEncoder().encodeToString(tep.anhThu()));
    }

    private byte[] taoAnhThu(Path tep) {
        ImageReader doc = null;
        try (ImageInputStream vao = ImageIO.createImageInputStream(tep.toFile())) {
            var cacBoDoc = ImageIO.getImageReaders(vao);
            if (!cacBoDoc.hasNext()) {
                return null;
            }
            doc = cacBoDoc.next();
            doc.setInput(vao);
            String loai = doc.getFormatName();
            int rong = doc.getWidth(0), cao = doc.getHeight(0);
            if (!(loai.equalsIgnoreCase("PNG") || loai.equalsIgnoreCase("JPEG")) || rong < 1 || cao < 1 ||
                    rong > 12000 || cao > 12000) {
                return null;
            }
            var cauHinh = doc.getDefaultReadParam();
            int buoc = Math.max(1, Math.max(rong, cao) / 480);
            cauHinh.setSourceSubsampling(buoc, buoc, 0, 0);
            BufferedImage goc = doc.read(0, cauHinh);
            double tiLe = Math.min(1, 320.0 / Math.max(goc.getWidth(), goc.getHeight()));
            BufferedImage thu = new BufferedImage(Math.max(1, (int) (goc.getWidth() * tiLe)),
                    Math.max(1, (int) (goc.getHeight() * tiLe)), BufferedImage.TYPE_INT_RGB);
            var ve = thu.createGraphics();
            try {
                ve.setColor(java.awt.Color.WHITE);
                ve.fillRect(0, 0, thu.getWidth(), thu.getHeight());
                ve.drawImage(goc, 0, 0, thu.getWidth(), thu.getHeight(), null);
            } finally {
                ve.dispose();
            }
            ByteArrayOutputStream ra = new ByteArrayOutputStream();
            ImageIO.write(thu, "JPEG", ra);
            return ra.size() <= 24576 ? ra.toByteArray() : null;
        } catch (IOException | RuntimeException loi) {
            return null;
        } finally {
            if (doc != null) {
                doc.dispose();
            }
        }
    }
}
