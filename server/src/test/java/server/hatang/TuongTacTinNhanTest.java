package server.hatang;

import server.dao.NguoiDungDao;
import server.hatang.BaoMat;
import server.dao.BanBeDao;
import server.dao.CSDLDao;
import server.model.HoSoModel;

import com.google.gson.*;
import giaothuc.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TuongTacTinNhanTest {
    @TempDir
    static Path tep;
    CSDLDao kho;
    MayChuChat mayChu;
    Thread luong;
    final List<String> email = new ArrayList<>(), id = new ArrayList<>();
    final List<MayThu> cacMay = new ArrayList<>();
    final String matKhau = "TuongTacTCP2026!";

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

    static JsonObject dung(JsonObject tra) {
        assertTrue(tra.get("thanhCong").getAsBoolean(), tra.toString());
        return tra;
    }

    static void sai(JsonObject tra, String ma) {
        assertFalse(tra.get("thanhCong").getAsBoolean(), tra.toString());
        assertEquals(ma, DuLieu.chu(tra, "maLoi"));
    }

    JsonObject lenhTin(int dich, String chu, String ma, String traLoi) {
        return DuLieu.tao("nguoiNhanId", dich < 0 ? null : id.get(dich), "noiDung", chu, "maGui", ma,
                "traLoiId", traLoi);
    }

    JsonObject guiTin(int gui, int dich, String chu, String traLoi) throws Exception {
        return dung(cacMay.get(gui)
                .gui("guiTin", lenhTin(dich, chu, UUID.randomUUID().toString(), traLoi))).getAsJsonObject(
                "tinNhan");
    }

    String ma(JsonObject tin) {
        return DuLieu.chu(tin, "id");
    }

    JsonObject thaoTac(int nguoi, String loai, String tin, String cam) throws Exception {
        return cacMay.get(nguoi).gui(loai, DuLieu.tao("tinNhanId", tin, "bieuCam", cam));
    }

    JsonArray lichSu(int nguoi, int dich) throws Exception {
        return dung(cacMay.get(nguoi)
                .gui("lichSu", DuLieu.tao("nguoiNhanId", dich < 0 ? null : id.get(dich)))).getAsJsonArray(
                "tinNhan");
    }

    JsonObject tim(JsonArray cacTin, String ma) {
        for (JsonElement t : cacTin) {
            if (ma(t.getAsJsonObject()).equals(ma)) {
                return t.getAsJsonObject();
            }
        }
        throw new AssertionError("Không tìm được tin " + ma);
    }

    @BeforeAll
    void chuanBi() throws Exception {
        CauHinh cauHinh = CauHinh.doc(Path.of("../cau-hinh/may-chu.properties"));
        if (!cauHinh.urlCSDL().contains("127.0.0.1:3306/nexus_javafx")) {
            throw new IllegalStateException("Chỉ kiểm tra CSDL JavaFX cục bộ.");
        }
        kho = new CSDLDao(cauHinh);
        mayChu = new MayChuChat(kho, "127.0.0.1", 0, tep);
        luong = new Thread(() -> {
            try {
                mayChu.phucVu();
            } catch (IOException loi) {
            }
        });
        luong.start();
        String bam = BaoMat.bam(matKhau);
        for (int i = 0; i < 3; i++) {
            String e = "qa-tuong-tac-" + UUID.randomUUID() + "@example.invalid";
            email.add(e);
            id.add(Long.toString(new NguoiDungDao(kho).themNguoi("Người thử tương tác " + i, e, bam).id()));
            MayThu m = new MayThu(mayChu.cong());
            cacMay.add(m);
            dung(m.gui("dangNhap", DuLieu.tao("email", e, "matKhau", matKhau)));
        }
    }

    @AfterAll
    void don() throws Exception {
        for (MayThu m : cacMay) {
            m.close();
        }
        if (mayChu != null) {
            mayChu.close();
        }
        if (luong != null) {
            luong.join(5000);
        }
        if (kho != null) {
            try (Connection db = kho.moKetNoi();
                 PreparedStatement xoa = db.prepareStatement("DELETE FROM nguoi_dung WHERE email=?")) {
                for (String e : email) {
                    xoa.setString(1, e);
                    xoa.executeUpdate();
                }
            }
        }
    }

    @Test
    void traLoiDungHoiThoaiVaGuiLaiKhongTrung() throws Exception {
        JsonObject goc = guiTin(0, 1, "Nội dung gốc ữ 😊", null);
        String maGui = UUID.randomUUID().toString();
        JsonObject lenh = lenhTin(0, "Mình trả lời", maGui, ma(goc));
        JsonObject tra = dung(cacMay.get(1).gui("guiTin", lenh)).getAsJsonObject("tinNhan");
        assertEquals(ma(goc), DuLieu.chu(tra, "traLoiId"));
        assertEquals("Nội dung gốc ữ 😊", DuLieu.chu(tra.getAsJsonObject("trichDan"), "noiDung"));
        assertEquals(ma(tra), ma(dung(cacMay.get(1).gui("guiTin", lenh)).getAsJsonObject("tinNhan")));
        lenh.remove("traLoiId");
        sai(cacMay.get(1).gui("guiTin", lenh), "maGuiDaDung");
        sai(cacMay.get(2).gui("guiTin", lenhTin(0, "Đọc lén", UUID.randomUUID().toString(), ma(goc))),
                "traLoiSai");
        sai(cacMay.get(0)
                        .gui("guiTin", lenhTin(-1, "Lộ vào phòng chung", UUID.randomUUID().toString(), ma(goc))),
                "traLoiSai");
        JsonObject chung = guiTin(0, -1, "Phòng chung", null);
        assertTrue(guiTin(2, -1, "Trả lời chung", ma(chung)).has("trichDan"));
        sai(cacMay.get(0).gui("guiTin", lenhTin(1, "Lệch phòng", UUID.randomUUID().toString(), ma(chung))),
                "traLoiSai");
    }

    @Test
    void camXucMoiNguoiMotLuaChonVaCoTheDoiBo() throws Exception {
        String tin = ma(guiTin(0, 1, "Thử cảm xúc", null));
        dung(thaoTac(0, "datCamXuc", tin, "👍"));
        dung(thaoTac(1, "datCamXuc", tin, "❤️"));
        JsonObject a = tim(lichSu(0, 1), tin), b = tim(lichSu(1, 0), tin);
        assertEquals(2, b.getAsJsonArray("camXuc").size());
        assertEquals("👍", DuLieu.chu(a, "camXucCuaToi"));
        assertEquals("❤️", DuLieu.chu(b, "camXucCuaToi"));
        String phien = DuLieu.chu(b, "phienBan");
        dung(thaoTac(1, "datCamXuc", tin, "❤️"));
        assertEquals(phien, DuLieu.chu(tim(lichSu(1, 0), tin), "phienBan"));
        dung(thaoTac(1, "datCamXuc", tin, "👍"));
        b = tim(lichSu(1, 0), tin);
        assertEquals(1, b.getAsJsonArray("camXuc").size());
        assertEquals(2, b.getAsJsonArray("camXuc").get(0).getAsJsonObject().get("soLuong").getAsInt());
        dung(thaoTac(1, "datCamXuc", tin, ""));
        b = tim(lichSu(1, 0), tin);
        assertEquals("", DuLieu.chu(b, "camXucCuaToi"));
        assertEquals(1, b.getAsJsonArray("camXuc").get(0).getAsJsonObject().get("soLuong").getAsInt());
        sai(thaoTac(2, "datCamXuc", tin, "👍"), "tinKhongHopLe");
        sai(thaoTac(1, "datCamXuc", tin, "💣"), "camXucSai");
    }

    @Test
    void thuHoiChiChuTinVaKhongLoNoiDungTrongTrichDan() throws Exception {
        String maGui = UUID.randomUUID().toString();
        JsonObject lenh = lenhTin(1, "NỘI DUNG CẦN THU HỒI", maGui, null);
        JsonObject goc = dung(cacMay.get(0).gui("guiTin", lenh)).getAsJsonObject("tinNhan");
        JsonObject dap = guiTin(1, 0, "Đã nhận", ma(goc));
        sai(thaoTac(1, "thuHoiTin", ma(goc), ""), "khongDuocThuHoi");
        sai(thaoTac(2, "thuHoiTin", ma(goc), ""), "khongDuocThuHoi");
        dung(thaoTac(1, "datCamXuc", ma(goc), "😂"));
        JsonObject thu = dung(thaoTac(0, "thuHoiTin", ma(goc), "")).getAsJsonObject("tinNhan");
        assertTrue(thu.get("daThuHoi").getAsBoolean());
        assertFalse(thu.toString().contains("NỘI DUNG CẦN THU HỒI"));
        assertTrue(thu.getAsJsonArray("camXuc").isEmpty());
        assertEquals(DuLieu.chu(thu, "phienBan"),
                DuLieu.chu(dung(thaoTac(0, "thuHoiTin", ma(goc), "")).getAsJsonObject("tinNhan"),
                        "phienBan"));
        JsonObject lap = dung(cacMay.get(0).gui("guiTin", lenh)).getAsJsonObject("tinNhan");
        assertEquals(ma(goc), ma(lap));
        assertTrue(lap.get("daThuHoi").getAsBoolean());
        JsonObject trich = tim(lichSu(1, 0), ma(dap)).getAsJsonObject("trichDan");
        assertTrue(trich.get("daThuHoi").getAsBoolean());
        assertEquals("Tin nhắn đã được thu hồi", DuLieu.chu(trich, "noiDung"));
        sai(thaoTac(1, "datCamXuc", ma(goc), "😂"), "tinDaThuHoi");
        sai(cacMay.get(1).gui("guiTin", lenhTin(0, "Trả lời muộn", UUID.randomUUID().toString(), ma(goc))),
                "tinDaThuHoi");
        lenh.addProperty("noiDung", "Nội dung khác");
        sai(cacMay.get(0).gui("guiTin", lenh), "maGuiDaDung");
        try (Connection db = kho.moKetNoi();
             PreparedStatement tim = db.prepareStatement("SELECT noi_dung FROM tin_nhan WHERE id=?")) {
            tim.setLong(1, Long.parseLong(ma(goc)));
            try (ResultSet d = tim.executeQuery()) {
                assertTrue(d.next());
                assertEquals("", d.getString(1));
            }
        }
    }

    @Test
    void thuHoiTepChanTaiAnhThuVaGuiLaiKhongKhoiPhuc() throws Exception {
        JsonObject goc = guiTin(1, 0, "Gửi tài liệu ở đây nhé", null);
        byte[] bytes = {1, 2, 3, 4};
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        JsonObject meta =
                DuLieu.tao("nguoiNhanId", id.get(1), "tenTep", "bai-tap.zip", "kichThuoc", "4", "sha256",
                        sha, "maGui", UUID.randomUUID().toString(), "noiDung", "Tài liệu", "traLoiId",
                        ma(goc));
        dung(cacMay.get(0).gui("tepBatDau", meta));
        dung(cacMay.get(0).gui("tepGuiKhoi",
                DuLieu.tao("viTri", "0", "duLieu", Base64.getEncoder().encodeToString(bytes))));
        JsonObject tin =
                dung(cacMay.get(0).gui("tepKetThuc", new JsonObject())).getAsJsonObject("tinNhan");
        assertEquals(ma(goc), DuLieu.chu(tin.getAsJsonObject("trichDan"), "id"));
        dung(cacMay.get(1).gui("tepThongTin", DuLieu.tao("tinNhanId", ma(tin))));
        dung(thaoTac(0, "thuHoiTin", ma(tin), ""));
        for (String loai : List.of("tepThongTin", "tepDocKhoi", "tepAnhThu")) {
            sai(cacMay.get(1).gui(loai, DuLieu.tao("tinNhanId", ma(tin), "viTri", "0")),
                    "khongCoQuyenTai");
        }
        JsonObject lap = dung(cacMay.get(0).gui("tepBatDau", meta));
        assertTrue(lap.get("daCo").getAsBoolean());
        assertTrue(lap.getAsJsonObject("tinNhan").get("daThuHoi").getAsBoolean());
        assertFalse(lap.getAsJsonObject("tinNhan").has("tep"));
        assertFalse(tim(lichSu(1, 0), ma(tin)).has("tep"));
    }

    @Test
    void suKienDenDungHaiNguoiVaCamXucRiengTheoNguoiXem() throws Exception {
        String tin = ma(guiTin(0, 1, "Thử sự kiện riêng tư", null));
        dung(thaoTac(1, "datCamXuc", tin, "😮"));
        for (MayThu m : cacMay) {
            dung(m.gui("nhipTim", new JsonObject()));
        }
        for (int i = 0; i < 2; i++) {
            int nguoi = i;
            List<ThongDiep> cacSuKien = cacMay.get(i).suKien.stream()
                    .filter(t -> t.loai().equals("tinNhanDoi") &&
                            ma(t.duLieu().getAsJsonObject("tinNhan")).equals(tin)).toList();
            assertFalse(cacSuKien.isEmpty());
            assertEquals(nguoi == 1 ? "😮" : "",
                    DuLieu.chu(cacSuKien.get(cacSuKien.size() - 1).duLieu().getAsJsonObject("tinNhan"),
                            "camXucCuaToi"));
        }
        assertTrue(cacMay.get(2).suKien.stream().noneMatch(
                t -> (t.loai().equals("tinNhanMoi") || t.loai().equals("tinNhanDoi")) &&
                        ma(t.duLieu().getAsJsonObject("tinNhan")).equals(tin)));
        try (MayThu moi = new MayThu(mayChu.cong())) {
            sai(moi.gui("thuHoiTin", DuLieu.tao("tinNhanId", tin)), "chuaDangNhap");
            sai(moi.gui("datCamXuc", DuLieu.tao("tinNhanId", tin, "bieuCam", "👍")), "chuaDangNhap");
            dung(moi.gui("dangNhap", DuLieu.tao("email", email.get(1), "matKhau", matKhau)));
            JsonObject sau = dung(moi.gui("lichSu", DuLieu.tao("nguoiNhanId", id.get(0))));
            assertEquals("😮", DuLieu.chu(tim(sau.getAsJsonArray("tinNhan"), tin), "camXucCuaToi"));
        }
    }

    @Test
    void thuHoiDongThoiCamXucKhongDeLaiCamXucTrenTinDaThu() throws Exception {
        String tin = ma(guiTin(0, 1, "Đồng thời", null));
        ExecutorService luongThu = Executors.newFixedThreadPool(2);
        try {
            Future<JsonObject> a = luongThu.submit(() -> thaoTac(0, "thuHoiTin", tin, "")), b =
                    luongThu.submit(() -> thaoTac(1, "datCamXuc", tin, "😡"));
            dung(a.get(10, TimeUnit.SECONDS));
            JsonObject cam = b.get(10, TimeUnit.SECONDS);
            if (!cam.get("thanhCong").getAsBoolean()) {
                assertEquals("tinDaThuHoi", DuLieu.chu(cam, "maLoi"));
            }
            JsonObject sau = tim(lichSu(1, 0), tin);
            assertTrue(sau.get("daThuHoi").getAsBoolean());
            assertTrue(sau.getAsJsonArray("camXuc").isEmpty());
        } finally {
            luongThu.shutdownNow();
        }
    }

    @Test
    void lichSuCoTrichDanDaiVanPhanTrangTrongGioiHanKhung() throws Exception {
        String goc = ma(guiTin(0, 1, "ữ".repeat(4000), null));
        for (int i = 0; i < 7; i++) {
            guiTin(1, 0, "ữ".repeat(4000), goc);
        }
        JsonObject tra = dung(cacMay.get(1).gui("lichSu", DuLieu.tao("nguoiNhanId", id.get(0))));
        assertTrue(tra.get("coTinCu").getAsBoolean());
        assertTrue(new Gson().toJson(tra).getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 65536);
        JsonObject cu = dung(cacMay.get(1).gui("lichSu", DuLieu.tao("nguoiNhanId", id.get(0), "truocId",
                ma(tra.getAsJsonArray("tinNhan").get(0).getAsJsonObject()))));
        assertFalse(cu.getAsJsonArray("tinNhan").isEmpty());
    }

    @Test
    void xoaRiengLuuCSDLVaKhongAnhHuongNguoiKhac() throws Exception {
        String tin = ma(guiTin(0, 1, "Tin chỉ xóa phía B", null));
        sai(thaoTac(2, "xoaTin", tin, ""), "tinKhongHopLe");
        dung(thaoTac(1, "xoaTin", tin, ""));
        dung(thaoTac(1, "xoaTin", tin, ""));
        assertTrue(lichSu(1, 0).asList().stream().noneMatch(t -> ma(t.getAsJsonObject()).equals(tin)));
        assertNotNull(tim(lichSu(0, 1), tin));
        try (MayThu moi = new MayThu(mayChu.cong())) {
            dung(moi.gui("dangNhap", DuLieu.tao("email", email.get(1), "matKhau", matKhau)));
            JsonArray sau = dung(moi.gui("lichSu", DuLieu.tao("nguoiNhanId", id.get(0)))).getAsJsonArray(
                    "tinNhan");
            assertTrue(sau.asList().stream().noneMatch(t -> ma(t.getAsJsonObject()).equals(tin)));
        }
    }

    @Test
    void chuyenTiepVanBanDungDichKhongLoTrichDanVaKhongTrung() throws Exception {
        String goc = ma(guiTin(0, 1, "Nội dung để chuyển tiếp", null));
        String maGui = UUID.randomUUID().toString();
        JsonObject lenh = DuLieu.tao("tinNhanId", goc, "nguoiNhanId", id.get(2), "maGui", maGui);
        sai(cacMay.get(2).gui("chuyenTiepTin",
                DuLieu.tao("tinNhanId", goc, "nguoiNhanId", id.get(0), "maGui",
                        UUID.randomUUID().toString())), "tinKhongHopLe");
        JsonObject tin = dung(cacMay.get(1).gui("chuyenTiepTin", lenh)).getAsJsonObject("tinNhan");
        assertEquals(id.get(1), DuLieu.chu(tin, "nguoiGuiId"));
        assertEquals(id.get(2), DuLieu.chu(tin, "nguoiNhanId"));
        assertFalse(tin.has("trichDan"));
        assertEquals("Nội dung để chuyển tiếp", DuLieu.chu(tin, "noiDung"));
        assertEquals(ma(tin),
                ma(dung(cacMay.get(1).gui("chuyenTiepTin", lenh)).getAsJsonObject("tinNhan")));
        assertNotNull(tim(lichSu(2, 1), ma(tin)));
        dung(thaoTac(0, "thuHoiTin", goc, ""));
        assertEquals("Nội dung để chuyển tiếp", DuLieu.chu(tim(lichSu(2, 1), ma(tin)), "noiDung"));
        sai(cacMay.get(1).gui("chuyenTiepTin",
                DuLieu.tao("tinNhanId", goc, "nguoiNhanId", id.get(2), "maGui",
                        UUID.randomUUID().toString())), "tinDaThuHoi");
    }

    @Test
    void chuyenTiepTepTaoBanSaoDocLapCoQuyenTaiDung() throws Exception {
        byte[] cacByte = {6, 7, 8};
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cacByte));
        JsonObject meta =
                DuLieu.tao("nguoiNhanId", id.get(1), "tenTep", "chuyen-tiep.txt", "kichThuoc", "3",
                        "sha256", sha, "maGui", UUID.randomUUID().toString(), "noiDung", "");
        dung(cacMay.get(0).gui("tepBatDau", meta));
        dung(cacMay.get(0).gui("tepGuiKhoi",
                DuLieu.tao("viTri", "0", "duLieu", Base64.getEncoder().encodeToString(cacByte))));
        String goc =
                ma(dung(cacMay.get(0).gui("tepKetThuc", new JsonObject())).getAsJsonObject("tinNhan"));
        String maGui = UUID.randomUUID().toString();
        JsonObject lenh = DuLieu.tao("tinNhanId", goc, "nguoiNhanId", id.get(2), "maGui", maGui);
        JsonObject tin = dung(cacMay.get(1).gui("chuyenTiepTin", lenh)).getAsJsonObject("tinNhan");
        assertEquals(ma(tin),
                ma(dung(cacMay.get(1).gui("chuyenTiepTin", lenh)).getAsJsonObject("tinNhan")));
        dung(thaoTac(0, "thuHoiTin", goc, ""));
        JsonObject khoi =
                dung(cacMay.get(2).gui("tepDocKhoi", DuLieu.tao("tinNhanId", ma(tin), "viTri", "0")));
        assertArrayEquals(cacByte, Base64.getDecoder().decode(DuLieu.chu(khoi, "duLieu")));
        sai(cacMay.get(0).gui("tepThongTin", DuLieu.tao("tinNhanId", ma(tin))), "khongCoQuyenTai");
    }

    @Test
    void thongTinChatGhimHoSoVaMocDoc() throws Exception {
        String tin = ma(guiTin(0, 1, "Tin cần ghim", null));
        sai(thaoTac(2, "ghimTin", tin, ""), "tinKhongHopLe");
        dung(thaoTac(1, "ghimTin", tin, ""));
        JsonObject thongTin = dung(cacMay.get(0)
                .gui("thongTinChat", DuLieu.tao("nguoiNhanId", id.get(1), "loai", "ghim")));
        assertNotNull(tim(thongTin.getAsJsonArray("tinNhan"), tin));
        dung(cacMay.get(1).gui("danhDauDoc", DuLieu.tao("nguoiNhanId", id.get(0), "tinCuoiId", tin)));
        thongTin = dung(cacMay.get(0)
                .gui("thongTinChat", DuLieu.tao("nguoiNhanId", id.get(1), "loai", "ghim")));
        assertTrue(Long.parseLong(DuLieu.chu(thongTin, "daDocId")) >= Long.parseLong(tin));
        JsonObject ho = dung(cacMay.get(1)
                .gui("hoSoCongKhai", DuLieu.tao("nguoiDungId", id.get(0)))).getAsJsonObject("hoSo");
        assertFalse(ho.has("email"));
        assertFalse(ho.has("soDienThoai"));
        assertTrue(ho.has("tenHienThi"));
        dung(thaoTac(0, "boGhimTin", tin, ""));
        thongTin = dung(cacMay.get(0)
                .gui("thongTinChat", DuLieu.tao("nguoiNhanId", id.get(1), "loai", "ghim")));
        assertTrue(thongTin.getAsJsonArray("tinNhan").asList().stream()
                .noneMatch(t -> ma(t.getAsJsonObject()).equals(tin)));
    }

    @Test
    void trangThaiChanDongBoHaiChieuQuaNhieuPhienVaKhiKetNoiLai() throws Exception {
        MayThu a = cacMay.get(0);
        MayThu b = cacMay.get(1);
        MayThu nguoiKhac = cacMay.get(2);
        String idA = id.get(0);
        String idB = id.get(1);
        JsonObject dichA = DuLieu.tao("nguoiNhanId", idA);
        JsonObject dichB = DuLieu.tao("nguoiNhanId", idB);

        try (MayThu phienB = new MayThu(mayChu.cong())) {
            dung(phienB.gui("dangNhap", DuLieu.tao("email", email.get(1), "matKhau", matKhau)));
            dung(a.gui("chanNguoiDung", dichB));
            kiemTraTrangThaiChan(a, idB, true, false);
            kiemTraTrangThaiChan(b, idA, false, true);
            kiemTraTrangThaiChan(phienB, idA, false, true);
            kiemTraSuKienChan(a, idB, true, false);
            kiemTraSuKienChan(b, idA, false, true);
            kiemTraSuKienChan(phienB, idA, false, true);
            kiemTraTrangThaiChan(nguoiKhac, idA, false, false);
            assertTrue(nguoiKhac.suKien.stream().noneMatch(suKien -> suKien.loai().equals("chanNguoiDungDoi")));

            try (MayThu ketNoiLai = new MayThu(mayChu.cong())) {
                dung(ketNoiLai.gui("dangNhap", DuLieu.tao("email", email.get(0), "matKhau", matKhau)));
                kiemTraTrangThaiChan(ketNoiLai, idB, true, false);
            }

            dung(b.gui("chanNguoiDung", dichA));
            kiemTraTrangThaiChan(a, idB, true, true);
            kiemTraTrangThaiChan(b, idA, true, true);
            kiemTraSuKienChan(a, idB, true, true);
            kiemTraSuKienChan(b, idA, true, true);

            dung(a.gui("boChanNguoiDung", dichB));
            kiemTraTrangThaiChan(a, idB, false, true);
            kiemTraTrangThaiChan(b, idA, true, false);
            kiemTraSuKienChan(a, idB, false, true);
            kiemTraSuKienChan(b, idA, true, false);
            sai(a.gui("guiTin", lenhTin(1, "Chưa thể gửi", UUID.randomUUID().toString(), null)),
                    "khongTheNhanTin");

            dung(b.gui("boChanNguoiDung", dichA));
            kiemTraTrangThaiChan(a, idB, false, false);
            kiemTraTrangThaiChan(b, idA, false, false);
            kiemTraSuKienChan(a, idB, false, false);
            kiemTraSuKienChan(b, idA, false, false);
            guiTin(0, 1, "Đã gửi sau khi bỏ chặn", null);
        } finally {
            dung(a.gui("boChanNguoiDung", dichB));
            dung(b.gui("boChanNguoiDung", dichA));
        }
    }

    private static void kiemTraTrangThaiChan(MayThu may, String idDich, boolean daChan, boolean biChan)
            throws IOException {
        JsonObject ho = dung(may.gui("hoSoCongKhai", DuLieu.tao("nguoiDungId", idDich))).getAsJsonObject("hoSo");
        assertEquals(daChan, ho.get("daChan").getAsBoolean());
        assertEquals(biChan, ho.get("biChan").getAsBoolean());
    }

    private static void kiemTraSuKienChan(MayThu may, String idDich, boolean daChan, boolean biChan) {
        List<ThongDiep> suKienChan = may.suKien.stream()
                .filter(suKien -> suKien.loai().equals("chanNguoiDungDoi"))
                .toList();
        assertEquals(1, suKienChan.size());
        JsonObject trangThai = suKienChan.get(0).duLieu();
        assertEquals(new JsonPrimitive(idDich), trangThai.get("nguoiNhanId"));
        assertEquals(new JsonPrimitive(daChan), trangThai.get("daChan"));
        assertEquals(new JsonPrimitive(biChan), trangThai.get("biChan"));
        may.suKien.remove(suKienChan.get(0));
    }

    @Test
    void quyenRiengTuDuocLocTaiMayChu() throws Exception {
        long chu = Long.parseLong(id.get(0)), ban = Long.parseLong(id.get(1));
        HoSoModel ho = new HoSoModel(kho);
        BanBeDao banBe = new BanBeDao(kho);
        try {
            dung(cacMay.get(0).gui("luuQuyenRiengTu",
                    DuLieu.tao("quyenEmail", "moiNguoi", "quyenDienThoai", "banBe", "nguoiDungId",
                            id.get(2))));
            JsonObject khach = dung(cacMay.get(1)
                    .gui("hoSoCongKhai", DuLieu.tao("nguoiDungId", id.get(0)))).getAsJsonObject("hoSo");
            assertTrue(khach.has("email"));
            assertFalse(khach.has("soDienThoai"));
            assertEquals("chiMinhToi", DuLieu.chu(ho.doc(Long.parseLong(id.get(2))), "quyenEmail"));
            banBe.thaoTac(chu, ban, "gui");
            banBe.thaoTac(ban, chu, "chapNhan");
            assertTrue(ho.docChoNguoiXem(chu, ban).has("soDienThoai"));
            banBe.thaoTac(chu, ban, "huyBan");
            assertFalse(ho.docChoNguoiXem(chu, ban).has("soDienThoai"));
            sai(cacMay.get(0).gui("luuQuyenRiengTu",
                    DuLieu.tao("quyenEmail", "sai", "quyenDienThoai", "moiNguoi")), "quyenSai");
            assertEquals("moiNguoi", DuLieu.chu(ho.doc(chu), "quyenEmail"));
            dung(cacMay.get(0).gui("luuQuyenRiengTu",
                    DuLieu.tao("quyenEmail", "chiMinhToi", "quyenDienThoai", "chiMinhToi")));
            assertFalse(ho.docChoNguoiXem(chu, ban).has("email"));
            assertTrue(ho.docChoNguoiXem(chu, chu).has("email"));
        } finally {
            ho.luuQuyen(chu, DuLieu.tao("quyenEmail", "chiMinhToi", "quyenDienThoai", "chiMinhToi"));
        }
    }

    @Test
    void noiDungChiaSePhanLoaiVaAnTinDaXoa() throws Exception {
        String lienKet = ma(guiTin(0, 1, "Tài liệu https://example.com/tai-lieu", null));
        JsonObject lenh = DuLieu.tao("nguoiNhanId", id.get(0), "loai", "lienKet");
        assertNotNull(
                tim(dung(cacMay.get(1).gui("thongTinChat", lenh)).getAsJsonArray("tinNhan"), lienKet));
        JsonArray anh = dung(cacMay.get(1)
                .gui("thongTinChat", DuLieu.tao("nguoiNhanId", id.get(0), "loai", "anh"))).getAsJsonArray(
                "tinNhan");
        assertTrue(anh.asList().stream()
                .allMatch(e -> DuLieu.chu(e.getAsJsonObject(), "loaiTin").equals("anh")));
        dung(thaoTac(1, "xoaTin", lienKet, ""));
        assertTrue(
                dung(cacMay.get(1).gui("thongTinChat", lenh)).getAsJsonArray("tinNhan").asList().stream()
                        .noneMatch(e -> ma(e.getAsJsonObject()).equals(lienKet)));
        assertNotNull(tim(dung(cacMay.get(0).gui("thongTinChat",
                        DuLieu.tao("nguoiNhanId", id.get(1), "loai", "lienKet"))).getAsJsonArray("tinNhan"),
                lienKet));
    }

}
