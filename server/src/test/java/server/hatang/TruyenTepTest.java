package server.hatang;

import server.dao.NguoiDungDao;
import server.dao.TinNhanDao;
import server.hatang.BaoMat;
import server.dao.CSDLDao;
import server.model.TepModel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.google.gson.*;
import giaothuc.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.security.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TruyenTepTest {
    @TempDir
    static Path thuMuc;
    CSDLDao kho;
    MayChuChat server;
    Thread luong;
    final List<String> email = new ArrayList<>(), id = new ArrayList<>();
    final List<MayThu> cacMay = new ArrayList<>();
    final String matKhau = "TepTCP2026!";

    static class MayThu implements AutoCloseable {
        Socket socket;
        DataInputStream doc;
        DataOutputStream ghi;
        List<ThongDiep> suKien = new ArrayList<>();

        MayThu(int cong) throws IOException {
            socket = new Socket("127.0.0.1", cong);
            socket.setSoTimeout(10000);
            doc = new DataInputStream(socket.getInputStream());
            ghi = new DataOutputStream(socket.getOutputStream());
        }

        JsonObject gui(String loai, JsonObject duLieu) throws IOException {
            var lenh = ThongDiep.yeuCau(loai, duLieu);
            GiaoThuc.ghi(ghi, lenh);
            while (true) {
                var tra = GiaoThuc.doc(doc);
                if (tra.loai().equals("phanHoi") && tra.ma().equals(lenh.ma())) {
                    return tra.duLieu();
                }
                suKien.add(tra);
            }
        }

        public void close() throws IOException {
            socket.close();
        }
    }

    @BeforeAll
    void chuanBi() throws Exception {
        CauHinh cauHinh = CauHinh.doc(Path.of("../cau-hinh/may-chu.properties"));
        if (!cauHinh.urlCSDL().contains("127.0.0.1:3306/nexus_javafx")) {
            throw new IllegalStateException("Chỉ kiểm tra CSDL JavaFX cục bộ.");
        }
        kho = new CSDLDao(cauHinh);
        server = new MayChuChat(kho, "127.0.0.1", 0, thuMuc);
        luong = new Thread(() -> {
            try {
                server.phucVu();
            } catch (IOException loi) {
            }
        });
        luong.start();
        String bam = BaoMat.bam(matKhau);
        for (int i = 0; i < 3; i++) {
            String e = "qa-tep-" + UUID.randomUUID() + "@example.invalid";
            email.add(e);
            id.add(Long.toString(new NguoiDungDao(kho).themNguoi("Người thử tệp " + i, e, bam).id()));
            MayThu m = new MayThu(server.cong());
            cacMay.add(m);
            dung(m.gui("dangNhap", DuLieu.tao("email", e, "matKhau", matKhau)));
        }
    }

    static void dung(JsonObject o) {
        assertTrue(o.get("thanhCong").getAsBoolean(), o.toString());
    }

    static void sai(JsonObject o) {
        assertFalse(o.get("thanhCong").getAsBoolean(), o.toString());
    }

    static String sha(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    JsonObject thongTin(byte[] data, String ten, String ma) throws Exception {
        return DuLieu.tao("nguoiNhanId", id.get(1), "tenTep", ten, "kichThuoc",
                Integer.toString(data.length), "sha256", sha(data), "maGui", ma, "noiDung", "");
    }

    JsonObject guiTep(byte[] data, String ten, String ma) throws Exception {
        dung(cacMay.get(0).gui("tepBatDau", thongTin(data, ten, ma)));
        for (int i = 0; i < data.length; i += TepModel.kichThuocKhoi) {
            int so = Math.min(TepModel.kichThuocKhoi, data.length - i);
            JsonObject tra = cacMay.get(0).gui("tepGuiKhoi",
                    DuLieu.tao("viTri", Integer.toString(i), "duLieu",
                            Base64.getEncoder().encodeToString(Arrays.copyOfRange(data, i, i + so))));
            dung(tra);
            assertEquals(Integer.toString(i + so), DuLieu.chu(tra, "daNhan"));
        }
        JsonObject tra = cacMay.get(0).gui("tepKetThuc", new JsonObject());
        dung(tra);
        return tra.getAsJsonObject("tinNhan");
    }

    byte[] tai(MayThu m, String tinId) throws Exception {
        JsonObject meta = m.gui("tepThongTin", DuLieu.tao("tinNhanId", tinId));
        dung(meta);
        long size = Long.parseLong(DuLieu.chu(meta, "kichThuoc"));
        ByteArrayOutputStream duLieu = new ByteArrayOutputStream();
        for (long viTri = 0; viTri < size; ) {
            JsonObject khoi =
                    m.gui("tepDocKhoi", DuLieu.tao("tinNhanId", tinId, "viTri", Long.toString(viTri)));
            dung(khoi);
            assertEquals(Long.toString(viTri), DuLieu.chu(khoi, "viTri"));
            byte[] data = Base64.getDecoder().decode(DuLieu.chu(khoi, "duLieu"));
            duLieu.write(data);
            viTri += data.length;
        }
        assertEquals(DuLieu.chu(meta, "sha256"), sha(duLieu.toByteArray()));
        return duLieu.toByteArray();
    }

    long soPart() throws IOException {
        try (var tep = Files.list(thuMuc)) {
            return tep.filter(p -> p.toString().endsWith(".part")).count();
        }
    }

    @AfterEach
    void donPart() throws Exception {
        for (MayThu may : cacMay) {
            dung(may.gui("tepHuy", new JsonObject()));
        }
    }

    @Test
    void batBuocDangNhapVaKiemTraDuLieuDauVao() throws Exception {
        try (MayThu chua = new MayThu(server.cong())) {
            for (String loai : List.of("tepBatDau", "tepGuiKhoi", "tepKetThuc", "tepHuy",
                    "tepDocKhoi", "tepThongTin", "tepAnhThu")) {
                assertEquals("chuaDangNhap", DuLieu.chu(chua.gui(loai, new JsonObject()), "maLoi"));
            }
        }
        byte[] data = {1};
        String ma = UUID.randomUUID().toString();
        JsonObject meta = thongTin(data, "../thoat.txt", ma);
        sai(cacMay.get(0).gui("tepBatDau", meta));
        meta.addProperty("tenTep", "an-toan.txt");
        meta.addProperty("kichThuoc", Long.toString(TepModel.gioiHanTep + 1));
        sai(cacMay.get(0).gui("tepBatDau", meta));
        meta.addProperty("kichThuoc", "1");
        meta.addProperty("nguoiNhanId", id.get(0));
        sai(cacMay.get(0).gui("tepBatDau", meta));
        assertEquals(0, soPart());
    }

    @Test
    void truyenRiengGhepKhoiLichSuVaKhongLoChoNguoiThuBa() throws Exception {
        byte[] data = new byte[70001];
        new Random(7).nextBytes(data);
        String ma = UUID.randomUUID().toString();
        JsonObject tin = guiTep(data, "Bài tập mạng.zip", ma);
        String tinId = DuLieu.chu(tin, "id");
        assertEquals(id.get(0), DuLieu.chu(tin, "nguoiGuiId"));
        assertEquals(data.length, tin.getAsJsonObject("tep").get("kichThuoc").getAsInt());
        assertArrayEquals(data, tai(cacMay.get(1), tinId));
        for (String loai : List.of("tepThongTin", "tepDocKhoi", "tepAnhThu")) {
            sai(cacMay.get(2).gui(loai, DuLieu.tao("tinNhanId", tinId, "viTri", "0")));
        }
        for (int i = 0; i < 3; i++) {
            cacMay.get(i).gui("nhipTim", new JsonObject());
        }
        assertTrue(cacMay.get(1).suKien.stream().anyMatch(t -> t.loai().equals("tinNhanMoi") &&
                DuLieu.chu(t.duLieu().getAsJsonObject("tinNhan"), "maGui").equals(ma)));
        assertFalse(cacMay.get(2).suKien.stream().anyMatch(t -> t.loai().equals("tinNhanMoi") &&
                DuLieu.chu(t.duLieu().getAsJsonObject("tinNhan"), "maGui").equals(ma)));
        assertTrue(new TinNhanDao(kho).lichSu(Long.parseLong(id.get(1)), Long.parseLong(id.get(0)), null).asList().stream()
                .anyMatch(e -> DuLieu.chu(e.getAsJsonObject(), "id").equals(tinId) &&
                        e.getAsJsonObject().has("tep")));
    }

    @Test
    void saiShaHoacThieuDuLieuKhongTaoTinVaDonTepTam() throws Exception {
        JsonObject meta = thongTin(new byte[]{1, 2, 3}, "loi.bin", UUID.randomUUID().toString());
        meta.addProperty("sha256", "0".repeat(64));
        dung(cacMay.get(0).gui("tepBatDau", meta));
        dung(cacMay.get(0).gui("tepGuiKhoi", DuLieu.tao("viTri", "0", "duLieu",
                Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}))));
        assertEquals("tepKhongKhop",
                DuLieu.chu(cacMay.get(0).gui("tepKetThuc", new JsonObject()), "maLoi"));
        assertEquals(0, soPart());
        meta = thongTin(new byte[]{1}, "thieu.bin", UUID.randomUUID().toString());
        dung(cacMay.get(0).gui("tepBatDau", meta));
        sai(cacMay.get(0).gui("tepKetThuc", new JsonObject()));
        assertEquals(0, soPart());
    }

    @Test
    void saiViTriVuotDungLuongVaHuyKhongLamHongSocket() throws Exception {
        for (int i = 0; i < 2; i++) {
            dung(cacMay.get(0)
                    .gui("tepBatDau", thongTin(new byte[]{1}, "khoi.bin", UUID.randomUUID().toString())));
            sai(cacMay.get(0).gui("tepGuiKhoi", DuLieu.tao("viTri", i == 0 ? "1" : "0", "duLieu",
                    Base64.getEncoder().encodeToString(new byte[]{1, 2}))));
            assertEquals(0, soPart());
        }
        dung(cacMay.get(0)
                .gui("tepBatDau", thongTin(new byte[]{1}, "huy.bin", UUID.randomUUID().toString())));
        assertEquals(1, soPart());
        dung(cacMay.get(0).gui("tepHuy", new JsonObject()));
        assertEquals(0, soPart());
        dung(cacMay.get(0).gui("nhipTim", new JsonObject()));
    }

    @Test
    void ngatSocketDonTepDangNhan() throws Exception {
        try (MayThu moi = new MayThu(server.cong())) {
            dung(moi.gui("dangNhap", DuLieu.tao("email", email.get(0), "matKhau", matKhau)));
            dung(moi.gui("tepBatDau",
                    thongTin(new byte[]{1}, "mat-ket-noi.bin", UUID.randomUUID().toString())));
            assertEquals(1, soPart());
        }
        long het = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (soPart() != 0 && System.nanoTime() < het) {
            Thread.sleep(30);
        }
        assertEquals(0, soPart());
    }

    @Test
    void guiLaiSauMatAckKhongTrungVaKhongDoiNoiDung() throws Exception {
        byte[] data = {3, 2, 1};
        String ma = UUID.randomUUID().toString();
        JsonObject tin = guiTep(data, "thu.txt", ma);
        JsonObject tra = cacMay.get(0).gui("tepBatDau", thongTin(data, "thu.txt", ma));
        dung(tra);
        assertTrue(tra.get("daCo").getAsBoolean());
        assertEquals(tin.get("id"), tra.getAsJsonObject("tinNhan").get("id"));
        sai(cacMay.get(0).gui("tepBatDau", thongTin(data, "ten-khac.txt", ma)));
        assertEquals("maGuiDaDung", DuLieu.chu(cacMay.get(0).gui("guiTin",
                DuLieu.tao("nguoiNhanId", id.get(1), "noiDung", DuLieu.chu(tin, "noiDung"), "maGui",
                        ma)), "maLoi"));
    }

    @Test
    void tepRongVaTepMuoiMBDungBienTruyenDayDu() throws Exception {
        JsonObject rong = guiTep(new byte[0], "rong.txt", UUID.randomUUID().toString());
        assertEquals(0, tai(cacMay.get(1), DuLieu.chu(rong, "id")).length);
        byte[] data = new byte[(int) TepModel.gioiHanTep];
        new Random(13).nextBytes(data);
        JsonObject lon = guiTep(data, "du-10mb.bin", UUID.randomUUID().toString());
        assertArrayEquals(data, tai(cacMay.get(1), DuLieu.chu(lon, "id")));
    }

    @Test
    void anhThuDuocNhanDienTheoDuLieuVaNguoiNhanMoiDuocXem() throws Exception {
        var anh = new java.awt.image.BufferedImage(700, 500, java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream ra = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(anh, "PNG", ra);
        JsonObject tin = guiTep(ra.toByteArray(), "anh.bin", UUID.randomUUID().toString());
        assertEquals("anh", DuLieu.chu(tin, "loaiTin"));
        JsonObject tra = cacMay.get(1).gui("tepAnhThu", DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id")));
        dung(tra);
        byte[] anhThu = Base64.getDecoder().decode(DuLieu.chu(tra, "anh"));
        assertTrue(anhThu.length <= 24576);
        var doc = javax.imageio.ImageIO.read(new ByteArrayInputStream(anhThu));
        assertTrue(doc.getWidth() <= 320 && doc.getHeight() <= 320);
        sai(cacMay.get(2).gui("tepAnhThu", DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id"))));
        // Người nhận mở lại bằng kết nối mới vẫn tải được tệp trong lịch sử.
        try (MayThu moi = new MayThu(server.cong())) {
            dung(moi.gui("dangNhap", DuLieu.tao("email", email.get(1), "matKhau", matKhau)));
            assertArrayEquals(ra.toByteArray(), tai(moi, DuLieu.chu(tin, "id")));
        }
    }

    @Test
    void lichSuChuThichDaiVanNamTrongKhungTCPVaDocTiepDuoc() throws Exception {
        for (int i = 0; i < 6; i++) {
            JsonObject meta = thongTin(new byte[0], "chú-thích-" + i + ".txt", UUID.randomUUID().toString());
            meta.addProperty("noiDung", "ữ".repeat(4000));
            dung(cacMay.get(0).gui("tepBatDau", meta));
            dung(cacMay.get(0).gui("tepKetThuc", new JsonObject()));
        }
        JsonObject tra = cacMay.get(1).gui("lichSu", DuLieu.tao("nguoiNhanId", id.get(0)));
        dung(tra);
        assertTrue(tra.get("coTinCu").getAsBoolean());
        JsonArray trang = tra.getAsJsonArray("tinNhan");
        assertTrue(trang.size() < 6);
        assertTrue(new Gson().toJson(tra).getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 65536);
        String truoc = DuLieu.chu(trang.get(0).getAsJsonObject(), "id");
        JsonObject cu =
                cacMay.get(1).gui("lichSu", DuLieu.tao("nguoiNhanId", id.get(0), "truocId", truoc));
        dung(cu);
        assertTrue(cu.getAsJsonArray("tinNhan").size() > 0);
    }

    @AfterAll
    void don() throws Exception {
        for (MayThu may : cacMay) {
            may.close();
        }
        if (server != null) {
            server.close();
        }
        if (luong != null) {
            luong.join(5000);
        }
        if (kho != null) {
            try (Connection db = kho.moKetNoi();
                 PreparedStatement lenh = db.prepareStatement("DELETE FROM nguoi_dung WHERE email=?")) {
                for (String e : email) {
                    lenh.setString(1, e);
                    lenh.executeUpdate();
                }
            }
        }
    }
}
