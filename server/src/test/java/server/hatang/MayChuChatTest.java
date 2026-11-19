package server.hatang;

import server.dao.NguoiDungDao;
import server.dao.TinNhanDao;
import server.hatang.BaoMat;
import server.dao.BanBeDao;
import server.dao.CSDLDao;
import server.model.NguoiDungModel;

import org.junit.jupiter.api.*;
import com.google.gson.*;
import giaothuc.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MayChuChatTest {
    private CSDLDao kho;
    private MayChuChat mayChu;
    private Thread luongMayChu;
    private final List<MayThu> cacMay = new ArrayList<>();
    private final List<String> cacEmail = new ArrayList<>();
    private final List<String> cacId = new ArrayList<>();
    private final String matKhau = "ThuNexusJava2026!";
    private String maLan;
    private JsonObject tinRieng;

    private static final class MayThu implements AutoCloseable {
        final Socket ketNoi;
        final DataOutputStream boGui;
        final DataInputStream boDoc;
        final List<ThongDiep> suKien = new ArrayList<>();

        MayThu(int cong) throws IOException {
            ketNoi = new Socket("127.0.0.1", cong);
            ketNoi.setSoTimeout(5000);
            boGui = new DataOutputStream(ketNoi.getOutputStream());
            boDoc = new DataInputStream(ketNoi.getInputStream());
        }

        JsonObject gui(String loai, JsonObject duLieu) throws IOException {
            ThongDiep yeuCau = ThongDiep.yeuCau(loai, duLieu);
            GiaoThuc.ghi(boGui, yeuCau);
            while (true) {
                ThongDiep tin = GiaoThuc.doc(boDoc);
                if (tin.loai().equals("phanHoi") && tin.ma().equals(yeuCau.ma())) {
                    return tin.duLieu();
                }
                suKien.add(tin);
            }
        }

        boolean daNhan(String maGui) throws IOException {
            gui("nhipTim", new JsonObject());
            return suKien.stream().filter(tin -> tin.loai().equals("tinNhanMoi")).anyMatch(
                    tin -> DuLieu.chu(tin.duLieu().getAsJsonObject("tinNhan"), "maGui").equals(maGui));
        }

        @Override
        public void close() throws IOException {
            ketNoi.close();
        }
    }

    @BeforeAll
    void chuanBi() throws Exception {
        Path tep = Path.of("../cau-hinh/may-chu.properties");
        if (!Files.exists(tep)) {
            throw new IllegalStateException(
                    "Tạo cau-hinh/may-chu.properties cho CSDL kiểm tra cục bộ trước khi chạy.");
        }
        CauHinh cauHinh = CauHinh.doc(tep);
        if (!cauHinh.urlCSDL().contains("127.0.0.1:3306/nexus_javafx")) {
            throw new IllegalStateException("Chỉ kiểm tra trên CSDL nexus_javafx cục bộ.");
        }
        kho = new CSDLDao(cauHinh);
        kho.kiemTra();
        mayChu = new MayChuChat(kho, "127.0.0.1", 0);
        luongMayChu = new Thread(() -> {
            try {
                mayChu.phucVu();
            } catch (IOException loi) {
                throw new UncheckedIOException(loi);
            }
        });
        luongMayChu.start();
        maLan = UUID.randomUUID().toString();
        for (int so = 0; so < 6; so++) {
            cacMay.add(new MayThu(mayChu.cong()));
            cacEmail.add("qa-java-" + so + "-" + maLan + "@example.invalid");
        }
    }

    @Test
    @Order(1)
    void chuaDangNhapKhongGuiDocHoacGiaDanhDuoc() throws Exception {
        JsonObject phanHoi =
                cacMay.get(0).gui("guiTin", DuLieu.tao("nguoiGuiId", "999", "noiDung", "Giả mạo"));
        assertFalse(phanHoi.get("thanhCong").getAsBoolean());
        assertEquals("chuaDangNhap", DuLieu.chu(phanHoi, "maLoi"));
        assertFalse(cacMay.get(0).gui("lichSu", new JsonObject()).get("thanhCong").getAsBoolean());
        for (String loai : List.of("layBanBe", "timTaiKhoan", "thaoTacBanBe", "layHoSo", "suaHoSo",
                "doiMatKhau")) {
            assertEquals("chuaDangNhap", DuLieu.chu(cacMay.get(0).gui(loai, new JsonObject()), "maLoi"));
        }
    }

    @Test
    @Order(2)
    void dangKyDangNhapSauClientVaKhongLoMatKhau() throws Exception {
        for (int so = 0; so < 6; so++) {
            MayThu may = cacMay.get(so);
            assertTrue(may.gui("dangKy",
                    DuLieu.tao("email", cacEmail.get(so), "matKhau", matKhau, "tenHienThi",
                            "Người thử " + so)).get("thanhCong").getAsBoolean());
            JsonObject phanHoi =
                    may.gui("dangNhap", DuLieu.tao("email", cacEmail.get(so), "matKhau", matKhau));
            assertTrue(phanHoi.get("thanhCong").getAsBoolean());
            JsonObject nguoi = phanHoi.getAsJsonObject("nguoiDung");
            cacId.add(DuLieu.chu(nguoi, "id"));
            assertFalse(nguoi.has("matKhauBam"));
        }
        assertEquals(6, mayChu.soKetNoi());
        assertFalse(cacMay.get(0).gui("dangKy",
                        DuLieu.tao("email", "khac@example.invalid", "matKhau", matKhau, "tenHienThi", "Khác"))
                .get("thanhCong").getAsBoolean());
        NguoiDungModel nguoi = new NguoiDungDao(kho).timTheoEmail(cacEmail.get(0));
        assertTrue(nguoi.matKhauBam().startsWith("pbkdf2$"));
        assertFalse(nguoi.matKhauBam().contains(matKhau));
    }

    @Test
    @Order(3)
    void broadcastDenDuSauClientQuaSocketThat() throws Exception {
        String ma = UUID.randomUUID().toString();
        assertTrue(cacMay.get(0).gui("guiTin", DuLieu.tao("noiDung", "Xin chào cả phòng 😊", "maGui", ma))
                .get("thanhCong").getAsBoolean());
        for (MayThu may : cacMay) {
            assertTrue(may.daNhan(ma));
        }
    }

    @Test
    @Order(4)
    void guiRiengChiHaiNguoiNhanVaKhongTinNguoiGuiTuClient() throws Exception {
        String ma = UUID.randomUUID().toString();
        JsonObject phanHoi = cacMay.get(0).gui("guiTin",
                DuLieu.tao("nguoiNhanId", cacId.get(1), "nguoiGuiId", cacId.get(2), "noiDung",
                        "Tin riêng A đến B", "maGui", ma));
        assertTrue(phanHoi.get("thanhCong").getAsBoolean());
        tinRieng = phanHoi.getAsJsonObject("tinNhan");
        assertEquals(cacId.get(0), DuLieu.chu(tinRieng, "nguoiGuiId"));
        assertTrue(cacMay.get(0).daNhan(ma));
        assertTrue(cacMay.get(1).daNhan(ma));
        for (int so = 2; so < 6; so++) {
            assertFalse(cacMay.get(so).daNhan(ma));
        }
    }

    @Test
    @Order(5)
    void lichSuKhongLoTinRiengVaGuiLaiKhongTrung() throws Exception {
        String ma = DuLieu.chu(tinRieng, "maGui");
        JsonObject phanHoi = cacMay.get(0).gui("guiTin",
                DuLieu.tao("nguoiNhanId", cacId.get(1), "noiDung", "Tin riêng A đến B", "maGui", ma));
        assertEquals(DuLieu.chu(tinRieng, "id"), DuLieu.chu(phanHoi.getAsJsonObject("tinNhan"), "id"));
        assertEquals(1, new TinNhanDao(kho).lichSu(Long.parseLong(cacId.get(0)), Long.parseLong(cacId.get(1)), null).size());
        JsonArray cuaC = cacMay.get(2).gui("lichSu", DuLieu.tao("nguoiNhanId", cacId.get(0)))
                .getAsJsonArray("tinNhan");
        assertEquals(0, cuaC.size());
        assertEquals("maGuiDaDung", DuLieu.chu(cacMay.get(0).gui("guiTin",
                        DuLieu.tao("nguoiNhanId", cacId.get(1), "noiDung", "Nội dung đổi", "maGui", ma)),
                "maLoi"));
    }

    @Test
    @Order(6)
    void khungQuaLonChiDongClientDoKhongLamSapServer() throws Exception {
        try (MayThu sai = new MayThu(mayChu.cong())) {
            sai.boGui.writeInt(65537);
            sai.boGui.flush();
            assertThrows(EOFException.class, () -> GiaoThuc.doc(sai.boDoc));
        }
        assertTrue(cacMay.get(0).gui("nhipTim", new JsonObject()).get("thanhCong").getAsBoolean());
        assertEquals("tinSai", DuLieu.chu(cacMay.get(0).gui("guiTin",
                DuLieu.tao("noiDung", "x".repeat(4001), "maGui", UUID.randomUUID().toString())), "maLoi"));
    }

    @Test
    @Order(7)
    void clientNgatKetNoiQuayLaiVanDocDuocTin() throws Exception {
        cacMay.get(1).close();
        MayThu moi = new MayThu(mayChu.cong());
        cacMay.set(1, moi);
        assertTrue(moi.gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau))
                .get("thanhCong").getAsBoolean());
        assertEquals(1,
                moi.gui("lichSu", DuLieu.tao("nguoiNhanId", cacId.get(0))).getAsJsonArray("tinNhan")
                        .size());
        assertTrue(moi.gui("dangXuat", new JsonObject()).get("thanhCong").getAsBoolean());
        assertFalse(moi.gui("lichSu", new JsonObject()).get("thanhCong").getAsBoolean());
    }

    private JsonObject timHoiThoai(MayThu may, String doiPhuong) throws IOException {
        JsonArray danhSach = may.gui("layHoiThoai", new JsonObject()).getAsJsonArray("hoiThoai");
        for (JsonElement phanTu : danhSach) {
            if (DuLieu.chu(phanTu.getAsJsonObject(), "id").equals(doiPhuong)) {
                return phanTu.getAsJsonObject();
            }
        }
        return null;
    }

    @Test
    @Order(8)
    void chuaDocTachTheoNguoiVaDuocLuuKhiDangNhapLai() throws Exception {
        MayThu a = cacMay.get(0), b = cacMay.get(1), c = cacMay.get(2);
        assertTrue(b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau))
                .get("thanhCong").getAsBoolean());
        assertEquals(1, timHoiThoai(b, cacId.get(0)).get("chuaDoc").getAsInt());
        JsonObject tinC = c.gui("guiTin",
                DuLieu.tao("nguoiNhanId", cacId.get(1), "noiDung", "C nhắn khi B đang xem A", "maGui",
                        UUID.randomUUID().toString())).getAsJsonObject("tinNhan");
        assertTrue(b.gui("danhDauDoc",
                        DuLieu.tao("nguoiNhanId", cacId.get(0), "tinCuoiId", DuLieu.chu(tinRieng, "id")))
                .get("thanhCong").getAsBoolean());
        assertEquals(0, timHoiThoai(b, cacId.get(0)).get("chuaDoc").getAsInt());
        assertEquals(1, timHoiThoai(b, cacId.get(2)).get("chuaDoc").getAsInt());
        assertNull(timHoiThoai(c, cacId.get(0)));
        assertFalse(c.gui("danhDauDoc",
                        DuLieu.tao("nguoiNhanId", cacId.get(0), "tinCuoiId", DuLieu.chu(tinRieng, "id")))
                .get("thanhCong").getAsBoolean());
        b.gui("dangXuat", new JsonObject());
        JsonObject tinMoi = a.gui("guiTin",
                DuLieu.tao("nguoiNhanId", cacId.get(1), "noiDung", "Tin khi B ngoại tuyến", "maGui",
                        UUID.randomUUID().toString())).getAsJsonObject("tinNhan");
        b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau));
        JsonObject hoiThoai = timHoiThoai(b, cacId.get(0));
        assertEquals(1, hoiThoai.get("chuaDoc").getAsInt());
        assertEquals(DuLieu.chu(tinMoi, "id"), DuLieu.chu(hoiThoai.getAsJsonObject("tinCuoi"), "id"));
        b.gui("danhDauDoc",
                DuLieu.tao("nguoiNhanId", cacId.get(0), "tinCuoiId", DuLieu.chu(tinMoi, "id")));
        b.gui("danhDauDoc",
                DuLieu.tao("nguoiNhanId", cacId.get(0), "tinCuoiId", DuLieu.chu(tinRieng, "id")));
        assertEquals(0, timHoiThoai(b, cacId.get(0)).get("chuaDoc").getAsInt());
        assertEquals(1, timHoiThoai(b, cacId.get(2)).get("chuaDoc").getAsInt());
        assertEquals(0,
                b.gui("layHoiThoai", DuLieu.tao("boQua", "50")).getAsJsonArray("hoiThoai").size());
    }

    private JsonObject thaoTac(int tu, int den, String lenh) throws IOException {
        return cacMay.get(tu).gui("thaoTacBanBe",
                DuLieu.tao("doiPhuongId", cacId.get(den), "thaoTac", lenh, "nguoiGuiId", cacId.get(5)));
    }

    private JsonArray cacBan(int so) throws IOException {
        return cacMay.get(so).gui("layBanBe", new JsonObject()).getAsJsonArray("banBe");
    }

    private void dung(JsonObject ketQua) {
        assertTrue(ketQua.get("thanhCong").getAsBoolean(), ketQua.toString());
    }

    private void sai(JsonObject ketQua) {
        assertFalse(ketQua.get("thanhCong").getAsBoolean(), ketQua.toString());
    }

    @Test
    @Order(9)
    void timChinhXacEmailKhongLoDuLieuDangNhapVaKhongTuKetBan() throws Exception {
        JsonObject nguoi = cacMay.get(0)
                .gui("timTaiKhoan", DuLieu.tao("email", cacEmail.get(1).toUpperCase(Locale.ROOT)))
                .getAsJsonObject("nguoiDung");
        assertEquals(cacId.get(1), DuLieu.chu(nguoi, "id"));
        assertEquals(Set.of("id", "tenHienThi", "dangOnline", "quanHe"), nguoi.keySet());
        assertFalse(cacMay.get(0).gui("timTaiKhoan", DuLieu.tao("email", cacEmail.get(0))).get("timThay")
                .getAsBoolean());
        assertFalse(cacMay.get(0)
                .gui("timTaiKhoan", DuLieu.tao("email", "khong-co-" + maLan + "@example.invalid"))
                .get("timThay").getAsBoolean());
        sai(thaoTac(0, 0, "gui"));
        sai(cacMay.get(0).gui("thaoTacBanBe",
                DuLieu.tao("doiPhuongId", "999999999999999999", "thaoTac", "gui")));
        sai(thaoTac(0, 1, "lenhSai"));
    }

    @Test
    @Order(10)
    void loiMoiChiDenDungNguoiVaKhongTuNhanHayThuHoiHo() throws Exception {
        cacMay.get(2).gui("nhipTim", new JsonObject());
        long soCu = cacMay.get(2).suKien.stream().filter(t -> t.loai().equals("banBeDoi")).count();
        dung(thaoTac(0, 1, "gui"));
        dung(thaoTac(0, 1, "gui"));
        assertEquals(1, cacBan(0).size());
        assertEquals(1, cacBan(1).size());
        assertEquals(0, cacBan(2).size());
        assertEquals("daGui", DuLieu.chu(cacBan(0).get(0).getAsJsonObject(), "quanHe"));
        assertEquals("daNhan", DuLieu.chu(cacBan(1).get(0).getAsJsonObject(), "quanHe"));
        assertTrue(cacMay.get(1).suKien.stream().anyMatch(t -> t.loai().equals("banBeDoi")));
        cacMay.get(2).gui("nhipTim", new JsonObject());
        assertEquals(soCu, cacMay.get(2).suKien.stream().filter(t -> t.loai().equals("banBeDoi")).count());
        sai(thaoTac(0, 1, "chapNhan"));
        sai(thaoTac(1, 0, "thuHoi"));
        sai(thaoTac(2, 0, "chapNhan"));
        sai(thaoTac(1, 0, "gui"));
    }

    @Test
    @Order(11)
    void chapNhanHaiChieuVaDangNhapLaiVanConBan() throws Exception {
        dung(thaoTac(1, 0, "chapNhan"));
        dung(thaoTac(1, 0, "chapNhan"));
        assertEquals("banBe", DuLieu.chu(cacBan(0).get(0).getAsJsonObject(), "quanHe"));
        assertEquals("banBe", DuLieu.chu(cacBan(1).get(0).getAsJsonObject(), "quanHe"));
        dung(cacMay.get(1).gui("dangXuat", new JsonObject()));
        dung(cacMay.get(1).gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau)));
        assertEquals("banBe", DuLieu.chu(cacBan(1).get(0).getAsJsonObject(), "quanHe"));
        assertEquals(2, new TinNhanDao(kho).lichSu(Long.parseLong(cacId.get(0)), Long.parseLong(cacId.get(1)), null).size());
    }

    @Test
    @Order(12)
    void huyBanThuHoiTuChoiVaLoiMoiKhiOffline() throws Exception {
        sai(thaoTac(2, 0, "huyBan"));
        dung(thaoTac(0, 1, "huyBan"));
        assertEquals(0, cacBan(1).size());
        assertEquals(2, new TinNhanDao(kho).lichSu(Long.parseLong(cacId.get(0)), Long.parseLong(cacId.get(1)), null).size());
        dung(thaoTac(0, 1, "gui"));
        sai(thaoTac(0, 1, "tuChoi"));
        dung(thaoTac(0, 1, "thuHoi"));
        assertEquals(0, cacBan(1).size());
        dung(cacMay.get(1).gui("dangXuat", new JsonObject()));
        dung(thaoTac(0, 1, "gui"));
        dung(cacMay.get(1).gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau)));
        assertEquals("daNhan", DuLieu.chu(cacBan(1).get(0).getAsJsonObject(), "quanHe"));
        dung(thaoTac(1, 0, "tuChoi"));
        assertEquals(0, cacBan(0).size());
    }

    @Test
    @Order(13)
    void guiDongThoiHaiChieuKhongTaoCapTrung() throws Exception {
        java.util.concurrent.ExecutorService luong = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.CountDownLatch batDau = new java.util.concurrent.CountDownLatch(1);
            var a = luong.submit(() -> {
                batDau.await();
                return thaoTac(0, 1, "gui");
            });
            var b = luong.submit(() -> {
                batDau.await();
                return thaoTac(1, 0, "gui");
            });
            batDau.countDown();
            JsonObject traA = a.get(8, java.util.concurrent.TimeUnit.SECONDS), traB =
                    b.get(8, java.util.concurrent.TimeUnit.SECONDS);
            assertNotEquals(traA.get("thanhCong").getAsBoolean(), traB.get("thanhCong").getAsBoolean());
            assertEquals(1, cacBan(0).size());
            assertEquals(1, cacBan(1).size());
            dung(traA.get("thanhCong").getAsBoolean() ? thaoTac(0, 1, "thuHoi") : thaoTac(1, 0, "thuHoi"));
        } finally {
            luong.shutdownNow();
        }
    }

    @Test
    @Order(14)
    void phanTrangBanBeKhongBoSotNguoiThuNhatNamMuoiMot() throws Exception {
        BanBeDao banBe = new BanBeDao(kho);
        long minh = Long.parseLong(cacId.get(3));
        String bam = BaoMat.bam(matKhau);
        for (int i = 0; i < 51; i++) {
            String email = "qa-ban-" + i + "-" + maLan + "@example.invalid";
            cacEmail.add(email);
            long id = new NguoiDungDao(kho).themNguoi("Bạn thử " + i, email, bam).id();
            banBe.thaoTac(minh, id, "gui");
        }
        JsonArray trang1 = cacBan(3);
        assertEquals(50, trang1.size());
        String sau = DuLieu.chu(trang1.get(49).getAsJsonObject(), "id");
        JsonArray trang2 =
                cacMay.get(3).gui("layBanBe", DuLieu.tao("sauId", sau)).getAsJsonArray("banBe");
        assertEquals(1, trang2.size());
        assertTrue(Long.parseLong(DuLieu.chu(trang2.get(0).getAsJsonObject(), "id")) > Long.parseLong(sau));
    }

    @Test
    @Order(15)
    void hoSoChiSuaCuaMinhVaKiemTraAnhThongTinPhienBan() throws Exception {
        MayThu e = cacMay.get(4);
        JsonObject banDau = e.gui("layHoSo", DuLieu.tao("id", cacId.get(0))).getAsJsonObject("hoSo");
        assertEquals(cacId.get(4), DuLieu.chu(banDau, "id"));
        var anh = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream mang = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(anh, "png", mang);
        JsonObject sua = DuLieu.tao("id", cacId.get(0), "tenHienThi", "Tên mới của E", "soDienThoai",
                "+84912345678", "gioiThieu", "Xin chào từ JavaFX", "anhDaiDien",
                Base64.getEncoder().encodeToString(mang.toByteArray()), "phienBan",
                DuLieu.chu(banDau, "phienBan"));
        JsonObject tra = e.gui("suaHoSo", sua);
        dung(tra);
        JsonObject moi = tra.getAsJsonObject("hoSo");
        assertEquals("Tên mới của E", DuLieu.chu(moi, "tenHienThi"));
        assertEquals("+84912345678", DuLieu.chu(moi, "soDienThoai"));
        assertFalse(DuLieu.chu(moi, "anhDaiDien").isBlank());
        assertEquals(banDau.get("email"), moi.get("email"));
        assertEquals("hoSoDaDoi", DuLieu.chu(e.gui("suaHoSo", sua), "maLoi"));
        sua.addProperty("phienBan", DuLieu.chu(moi, "phienBan"));
        sua.addProperty("soDienThoai", "abc");
        sai(e.gui("suaHoSo", sua));
        sua.addProperty("soDienThoai", "");
        sua.addProperty("gioiThieu", "x".repeat(241));
        sai(e.gui("suaHoSo", sua));
        sua.addProperty("gioiThieu", "");
        sua.addProperty("anhDaiDien", Base64.getEncoder().encodeToString("khong phai anh".getBytes()));
        sai(e.gui("suaHoSo", sua));
        sua.addProperty("anhDaiDien", "");
        dung(e.gui("suaHoSo", sua));
        assertEquals("",
                DuLieu.chu(e.gui("layHoSo", new JsonObject()).getAsJsonObject("hoSo"), "anhDaiDien"));
        assertEquals("Người thử 0",
                DuLieu.chu(cacMay.get(0).gui("layHoSo", new JsonObject()).getAsJsonObject("hoSo"),
                        "tenHienThi"));
    }

    @Test
    @Order(16)
    void khoiPhucPhienDoiMatKhauThuHoiPhienKhacVaDangXuat() throws Exception {
        try (MayThu a = new MayThu(mayChu.cong()); MayThu b = new MayThu(mayChu.cong());
             MayThu c = new MayThu(mayChu.cong())) {
            JsonObject traA = a.gui("dangNhap", DuLieu.tao("email", cacEmail.get(0), "matKhau", matKhau));
            dung(traA);
            String maA = DuLieu.chu(traA, "maPhien");
            JsonObject traB = b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(0), "matKhau", matKhau));
            dung(traB);
            String maB = DuLieu.chu(traB, "maPhien");
            assertNotEquals(maA, maB);
            assertEquals(43, maA.length());
            dung(c.gui("khoiPhucPhien", DuLieu.tao("maPhien", maA)));
            sai(a.gui("doiMatKhau",
                    DuLieu.tao("matKhauCu", "SaiMatKhau!", "matKhauMoi", "MatKhauMoi2026!")));
            dung(b.gui("nhipTim", new JsonObject()));
            dung(a.gui("doiMatKhau",
                    DuLieu.tao("matKhauCu", matKhau, "matKhauMoi", "MatKhauMoi2026!")));
            dung(c.gui("nhipTim", new JsonObject())); // C dùng cùng phiên A, vẫn được giữ.
            sai(b.gui("nhipTim", new JsonObject()));
            assertEquals("phienKhongHopLe",
                    DuLieu.chu(b.gui("khoiPhucPhien", DuLieu.tao("maPhien", maB)), "maLoi"));
            sai(b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(0), "matKhau", matKhau)));
            dung(b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(0), "matKhau", "MatKhauMoi2026!")));
            dung(a.gui("dangXuat", new JsonObject()));
            try (MayThu moi = new MayThu(mayChu.cong())) {
                assertEquals("phienKhongHopLe",
                        DuLieu.chu(moi.gui("khoiPhucPhien", DuLieu.tao("maPhien", maA)), "maLoi"));
            }
            sai(c.gui("nhipTim", new JsonObject()));
        }
    }

    @Test
    @Order(17)
    void dangXuatTaiKhoanNayKhongHuyTaiKhoanKhac() throws Exception {
        try (MayThu a = new MayThu(mayChu.cong()); MayThu b = new MayThu(mayChu.cong());
             MayThu moi = new MayThu(mayChu.cong())) {
            String maA =
                    DuLieu.chu(a.gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau)),
                            "maPhien");
            String maB =
                    DuLieu.chu(b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(5), "matKhau", matKhau)),
                            "maPhien");
            dung(a.gui("dangXuat", new JsonObject()));
            sai(moi.gui("khoiPhucPhien", DuLieu.tao("maPhien", maA)));
            dung(moi.gui("khoiPhucPhien", DuLieu.tao("maPhien", maB)));
            assertEquals(cacId.get(5),
                    DuLieu.chu(moi.gui("layHoSo", new JsonObject()).getAsJsonObject("hoSo"), "id"));
            try (Connection db = kho.moKetNoi(); PreparedStatement tim = db.prepareStatement(
                    "SELECT bam_ma FROM phien_dang_nhap WHERE nguoi_dung_id=?")) {
                tim.setLong(1, Long.parseLong(cacId.get(5)));
                try (ResultSet dong = tim.executeQuery()) {
                    while (dong.next()) {
                        assertEquals(64, dong.getString(1).length());
                        assertNotEquals(maB, dong.getString(1));
                    }
                }
            }
        }
    }

    @Test
    @Order(18)
    void tinNhomChiDenThanhVienVaLichSuPhanTrang() throws Exception {
        MayThu a = cacMay.get(0), b = cacMay.get(1), c = cacMay.get(2);
        dung(a.gui("dangNhap", DuLieu.tao("email", cacEmail.get(0), "matKhau", "MatKhauMoi2026!")));
        dung(thaoTac(0, 1, "gui"));
        dung(thaoTac(1, 0, "chapNhan"));
        JsonArray thanhVien = new JsonArray();
        thanhVien.add(cacId.get(1));
        JsonObject yeuCau = DuLieu.tao("ten", "Nhóm thử TCP", "thanhVien", thanhVien, "maTao",
                UUID.randomUUID().toString());
        JsonObject tra = a.gui("taoNhom", yeuCau);
        dung(tra);
        JsonObject nhom = tra.getAsJsonObject("nhom");
        String id = DuLieu.chu(nhom, "id");
        assertEquals(2, nhom.get("soThanhVien").getAsInt());
        assertEquals(id, DuLieu.chu(a.gui("taoNhom", yeuCau).getAsJsonObject("nhom"), "id"));
        assertTrue(b.gui("layNhom", new JsonObject()).getAsJsonArray("cacNhom").asList().stream()
                .anyMatch(n -> DuLieu.chu(n.getAsJsonObject(), "id").equals(id)));
        assertTrue(c.gui("layNhom", new JsonObject()).getAsJsonArray("cacNhom").isEmpty());

        String ma = UUID.randomUUID().toString();
        JsonObject lenh = DuLieu.tao("nhomId", id, "noiDung", "Chào nhóm", "maGui", ma, "nguoiGuiId",
                cacId.get(2));
        JsonObject gui = a.gui("guiTinNhom", lenh);
        dung(gui);
        assertEquals(cacId.get(0), DuLieu.chu(gui.getAsJsonObject("tinNhan"), "nguoiGuiId"));
        dung(a.gui("guiTinNhom", lenh));
        b.gui("nhipTim", new JsonObject());
        c.gui("nhipTim", new JsonObject());
        assertEquals(1, b.suKien.stream().filter(e -> e.loai().equals("tinNhanNhomMoi")
                && DuLieu.chu(e.duLieu().getAsJsonObject("tinNhan"), "maGui").equals(ma)).count());
        assertTrue(c.suKien.stream()
                .noneMatch(e -> e.loai().equals("tinNhanNhomMoi") || e.loai().equals("nhomDoi")));
        assertEquals("khongThuocNhom", DuLieu.chu(c.gui("guiTinNhom", lenh), "maLoi"));
        assertEquals("khongThuocNhom",
                DuLieu.chu(c.gui("lichSuNhom", DuLieu.tao("nhomId", id)), "maLoi"));

        for (int i = 0; i < 5; i++) {
            dung(a.gui("guiTinNhom", DuLieu.tao("nhomId", id, "noiDung", "<".repeat(4000), "maGui",
                    UUID.randomUUID().toString())));
        }
        JsonObject moi = b.gui("lichSuNhom", DuLieu.tao("nhomId", id));
        dung(moi);
        assertTrue(moi.get("coTinCu").getAsBoolean());
        String truoc = DuLieu.chu(moi.getAsJsonArray("tinNhan").get(0).getAsJsonObject(), "id");
        JsonArray cu = b.gui("lichSuNhom", DuLieu.tao("nhomId", id, "truocId", truoc))
                .getAsJsonArray("tinNhan");
        assertFalse(cu.isEmpty());
        assertNotEquals(truoc, DuLieu.chu(cu.get(cu.size() - 1).getAsJsonObject(), "id"));
    }

    @Test
    @Order(19)
    void timBangSoDienThoaiTonTrongQuyenRiengTu() throws Exception {
        MayThu a = cacMay.get(0), b = cacMay.get(1), c = cacMay.get(2);
        String so = "099" + String.format(Locale.ROOT, "%07d", new Random().nextInt(10_000_000));
        try (Connection db = kho.moKetNoi(); PreparedStatement lenh = db.prepareStatement(
                "UPDATE nguoi_dung SET so_dien_thoai=? WHERE id=?")) {
            lenh.setString(1, so);
            lenh.setLong(2, Long.parseLong(cacId.get(1)));
            lenh.executeUpdate();
        }
        JsonObject tim = DuLieu.tao("tuKhoa", "+84 " + so.substring(1, 4) + " " + so.substring(4));
        dung(b.gui("luuQuyenRiengTu",
                DuLieu.tao("quyenEmail", "chiMinhToi", "quyenDienThoai", "chiMinhToi")));
        assertFalse(a.gui("timTaiKhoan", tim).get("timThay").getAsBoolean());
        dung(b.gui("luuQuyenRiengTu",
                DuLieu.tao("quyenEmail", "chiMinhToi", "quyenDienThoai", "banBe")));
        assertFalse(c.gui("timTaiKhoan", tim).get("timThay").getAsBoolean());
        assertEquals(cacId.get(1),
                DuLieu.chu(a.gui("timTaiKhoan", tim).getAsJsonObject("nguoiDung"), "id"));
        dung(b.gui("luuQuyenRiengTu",
                DuLieu.tao("quyenEmail", "chiMinhToi", "quyenDienThoai", "moiNguoi")));
        JsonObject nguoi = c.gui("timTaiKhoan", tim).getAsJsonObject("nguoiDung");
        assertEquals(cacId.get(1), DuLieu.chu(nguoi, "id"));
        assertFalse(nguoi.has("soDienThoai"));
        assertEquals(cacId.get(1), DuLieu.chu(
                c.gui("timTaiKhoan", DuLieu.tao("tuKhoa", cacEmail.get(1))).getAsJsonObject("nguoiDung"),
                "id"));
    }

    @Test
    @Order(20)
    void lichSuCuocGoiChiHienChoHaiNguoiVaLuuKhiNgoaiTuyen() throws Exception {
        MayThu a = cacMay.get(0), b = cacMay.get(1), c = cacMay.get(2);
        String idA = cacId.get(0), idB = cacId.get(1);
        String maGoi = UUID.randomUUID().toString();
        JsonObject moi = DuLieu.tao("cuocGoiId", maGoi, "nguoiNhanId", idB, "port", "5100");
        JsonObject dieuKhien = DuLieu.tao("cuocGoiId", maGoi, "port", "5101");
        dung(a.gui("moiGoiThoai", moi));
        dung(b.gui("batMayGoiThoai", dieuKhien));
        dung(a.gui("ketThucGoiThoai", dieuKhien));
        dung(b.gui("ketThucGoiThoai", dieuKhien));
        JsonObject cuocGoi = lichSuGoi(b, idA).get(0);
        assertEquals("hoanTat", DuLieu.chu(cuocGoi, "ketQuaGoi"));
        assertTrue(cuocGoi.get("thoiLuongGoi").getAsLong() >= 0);
        assertEquals(DuLieu.chu(cuocGoi, "id"), DuLieu.chu(lichSuGoi(a, idB).get(0), "id"));
        assertTrue(a.daNhan(DuLieu.chu(cuocGoi, "maGui")));
        assertTrue(b.daNhan(DuLieu.chu(cuocGoi, "maGui")));
        assertFalse(c.daNhan(DuLieu.chu(cuocGoi, "maGui")));
        assertEquals("cuocGoiDaKetThuc", DuLieu.chu(a.gui("moiGoiThoai", moi), "maLoi"));

        dung(b.gui("dangXuat", new JsonObject()));
        moi.addProperty("cuocGoiId", UUID.randomUUID().toString());
        assertEquals("nguoiNhanNgoaiTuyen", DuLieu.chu(a.gui("moiGoiThoai", moi), "maLoi"));
        dung(b.gui("dangNhap", DuLieu.tao("email", cacEmail.get(1), "matKhau", matKhau)));
        List<JsonObject> daLuu = lichSuGoi(b, idA);
        assertEquals(List.of("hoanTat", "nho"), daLuu.stream().map(tin -> DuLieu.chu(tin, "ketQuaGoi")).toList());
        dung(b.gui("xoaTin", DuLieu.tao("tinNhanId", DuLieu.chu(daLuu.get(1), "id"))));
        assertEquals(1, lichSuGoi(b, idA).size());
        assertEquals(2, lichSuGoi(a, idB).size());
    }

    private List<JsonObject> lichSuGoi(MayThu may, String doiPhuong) throws IOException {
        JsonObject phanHoi = may.gui("lichSu", DuLieu.tao("nguoiNhanId", doiPhuong));
        dung(phanHoi);
        List<JsonObject> lichSu = new ArrayList<>();
        for (JsonElement phanTu : phanHoi.getAsJsonArray("tinNhan")) {
            JsonObject tin = phanTu.getAsJsonObject();
            if ("cuocGoi".equals(DuLieu.chu(tin, "loaiTin"))) {
                lichSu.add(tin);
            }
        }
        return lichSu;
    }

    @AfterAll
    void don() throws Exception {
        for (MayThu may : cacMay) {
            may.close();
        }
        if (mayChu != null) {
            mayChu.close();
        }
        if (luongMayChu != null) {
            luongMayChu.join(5000);
        }
        if (kho != null) {
            try (Connection ketNoi = kho.moKetNoi();
                 PreparedStatement lenh = ketNoi.prepareStatement("DELETE FROM nguoi_dung WHERE email=?")) {
                for (String email : cacEmail) {
                    lenh.setString(1, email);
                    lenh.executeUpdate();
                }
            }
        }
    }
}
