package server.controller;

import server.model.KetQuaLuuModel;
import server.hatang.BaoMat;
import server.hatang.BoNhanTep;
import server.dao.BanBeDao;
import server.dao.CSDLDao;
import server.dao.NguoiDungDao;
import server.dao.TinNhanDao;
import server.model.HoSoModel;
import server.dao.NhomChatDao;
import server.dao.PhienDao;
import server.model.TepModel;
import server.dao.ThongTinChatDao;
import server.dao.TuongTacTinNhanDao;
import server.model.LoiNghiepVuModel;
import server.model.NguoiDungModel;

import com.google.gson.*;
import giaothuc.*;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Điều phối yêu cầu và sự kiện, độc lập với cách đọc/ghi socket.
 */
public final class MayChuController {
    private final NguoiDungDao nguoiDungDao;
    private final TinNhanDao tinNhanDao;
    private final ThongTinChatDao thongTinChatDao;
    private final BanBeDao khoBanBe;
    private final NhomChatDao khoNhom;
    private final HoSoModel khoHoSo;
    private final PhienDao khoPhien;
    private final TepModel khoTep;
    private final TuongTacTinNhanDao khoTuongTac;
    private final GoiThoaiController goiThoai = new GoiThoaiController();
    private final Set<Phien> cacClient = ConcurrentHashMap.newKeySet();
    private final Map<String, long[]> luotDangNhapTheoIP = new HashMap<>();

    public MayChuController(CSDLDao csdl, Path thuMucTep) throws IOException {
        nguoiDungDao = new NguoiDungDao(csdl);
        tinNhanDao = new TinNhanDao(csdl);
        thongTinChatDao = new ThongTinChatDao(csdl);
        khoTep = new TepModel(csdl, thuMucTep);
        khoTuongTac = new TuongTacTinNhanDao(csdl);
        khoNhom = new NhomChatDao(csdl);
        khoBanBe = new BanBeDao(csdl);
        khoHoSo = new HoSoModel(csdl);
        khoPhien = new PhienDao(csdl);
    }

    public void them(Phien phien) {
        cacClient.add(phien);
    }

    public int soKetNoi() {
        return cacClient.size();
    }

    public void dongTatCa() {
        for (Phien phien : cacClient) {
            phien.close();
        }
    }

    public void roi(Phien phien, boolean mayChuDangChay) {
        boolean daVao = phien.nguoiDung != null;
        phien.nguoiDung = null;
        cacClient.remove(phien);
        goiThoai.roi(phien);
        phien.boNhanTep.close();
        if (daVao && mayChuDangChay) {
            phatDanhSach();
        }
    }

    /**
     * Tin chung gửi cho mọi client đã xác thực; tin riêng chỉ gửi đúng hai người.
     */
    private void phatTin(JsonObject tin, Long nguoiNhan, long nguoiGui) {
        ThongDiep suKien = ThongDiep.suKien("tinNhanMoi", DuLieu.tao("tinNhan", tin));
        for (Phien client : cacClient) {
            NguoiDungModel nguoi = client.nguoiDung;
            if (nguoi != null && (nguoiNhan == null || nguoi.id() == nguoiGui || nguoi.id() == nguoiNhan)) {
                client.gui(suKien);
            }
        }
    }

    // Cùng khóa với lịch sử/gửi tin để sự kiện có thứ tự và không khôi phục dữ liệu cũ.
    private void phatTinDoi(JsonObject tin) throws SQLException {
        Long nhan = DuLieu.id(tin, "nguoiNhanId", true);
        long gui = Long.parseLong(DuLieu.chu(tin, "nguoiGuiId"));
        Map<Long, JsonObject> theoNguoi = new HashMap<>();
        Set<Long> daAn = new HashSet<>();
        for (Phien client : cacClient) {
            NguoiDungModel nguoi = client.nguoiDung;
            if (nguoi != null && (nhan == null || nguoi.id() == gui || nguoi.id() == nhan)) {
                JsonObject rieng = theoNguoi.get(nguoi.id());
                if (daAn.contains(nguoi.id())) {
                    continue;
                }
                if (rieng == null) {
                    long id = Long.parseLong(DuLieu.chu(tin, "id"));
                    if (tinNhanDao.daAnTin(nguoi.id(), id)) {
                        daAn.add(nguoi.id());
                        continue;
                    }
                    rieng = khoTuongTac.doc(id, nguoi.id());
                    theoNguoi.put(nguoi.id(), rieng);
                }
                client.gui(ThongDiep.suKien("tinNhanDoi", DuLieu.tao("tinNhan", rieng)));
            }
        }
    }

    private void phatSuKienNhom(long id, String loai, JsonObject duLieu) throws SQLException {
        Set<Long> thanhVien = khoNhom.thanhVien(id);
        ThongDiep suKien = ThongDiep.suKien(loai, duLieu);
        for (Phien client : cacClient) {
            NguoiDungModel nguoi = client.nguoiDung;
            if (nguoi != null && thanhVien.contains(nguoi.id())) {
                client.gui(suKien);
            }
        }
    }

    private static String maNhomHopLe(JsonObject duLieu, String khoa) {
        String ma = DuLieu.chu(duLieu, khoa);
        if (!ma.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) {
            throw new IllegalArgumentException();
        }
        return ma;
    }

    private Set<Long> cacIdOnline() {
        Set<Long> cacId = new HashSet<>();
        for (Phien client : cacClient) {
            NguoiDungModel nguoi = client.nguoiDung;
            if (nguoi != null) {
                cacId.add(nguoi.id());
            }
        }
        return cacId;
    }

    private void baoQuanHeDoi(long nguoiA, long nguoiB) {
        ThongDiep suKien = ThongDiep.suKien("banBeDoi", new JsonObject());
        for (Phien client : cacClient) {
            NguoiDungModel nguoi = client.nguoiDung;
            if (nguoi != null && (nguoi.id() == nguoiA || nguoi.id() == nguoiB)) {
                client.gui(suKien);
            }
        }
    }

    private void phatDanhSach() {
        try {
            Set<Long> dangOnline = new HashSet<>();
            for (Phien client : cacClient) {
                if (client.nguoiDung != null) {
                    dangOnline.add(client.nguoiDung.id());
                }
            }
            JsonArray cacNguoi = new JsonArray();
            for (NguoiDungModel nguoi : nguoiDungDao.danhSachNguoi()) {
                cacNguoi.add(nguoi.congKhai(dangOnline.contains(nguoi.id())));
            }
            ThongDiep suKien = ThongDiep.suKien("danhSachNguoiDung", DuLieu.tao("nguoiDung", cacNguoi));
            for (Phien client : cacClient) {
                if (client.nguoiDung != null) {
                    client.gui(suKien);
                }
            }
        } catch (SQLException loi) {
            System.err.println("Chưa đọc được danh sách người dùng: " + loi.getSQLState());
        }
    }

    private synchronized boolean choPhepDangNhap(String ip) {
        long bayGio = System.currentTimeMillis();
        luotDangNhapTheoIP.entrySet().removeIf(dong -> bayGio - dong.getValue()[0] > 60000);
        if (!luotDangNhapTheoIP.containsKey(ip) && luotDangNhapTheoIP.size() >= 256) {
            return false;
        }
        long[] moc = luotDangNhapTheoIP.computeIfAbsent(ip, diaChi -> new long[]{bayGio, 0});
        return ++moc[1] <= 40;
    }

    public abstract class Phien {
        private volatile NguoiDungModel nguoiDung;
        private volatile String bamPhien;
        private final BoNhanTep boNhanTep = new BoNhanTep(khoTep);

        protected abstract void gui(ThongDiep thongDiep);

        protected abstract String diaChi();

        protected abstract void datHanDoc(int miliGiay) throws IOException;

        public abstract void close();

        protected final void xuLy(ThongDiep yeuCau) {
            try {
                JsonObject duLieu = yeuCau.duLieu();
                switch (yeuCau.loai()) {
                    case "dangKy", "dangNhap" -> xacThuc(yeuCau, duLieu);
                    case "khoiPhucPhien" -> {
                        if (nguoiDung != null) {
                            throw new LoiNghiepVuModel("daDangNhap", "Bạn đang đăng nhập.");
                        }
                        if (!choPhepDangNhap(diaChi())) {
                            throw new LoiNghiepVuModel("quaNhieuLanThu",
                                    "Bạn thử quá nhiều lần. Chờ một phút nhé.");
                        }
                        String ma = DuLieu.chu(duLieu, "maPhien");
                        nguoiDung = khoPhien.khoiPhuc(ma);
                        bamPhien = PhienDao.bam(ma);
                        datHanDoc(65000);
                        thanhCong(yeuCau, DuLieu.tao("nguoiDung", nguoiDung.rieng(), "maPhien", ma));
                        phatDanhSach();
                    }
                    case "hoSoCongKhai" -> {
                        canDangNhap();
                        long id = DuLieu.id(duLieu, "nguoiDungId", false);
                        JsonObject ho = khoHoSo.docChoNguoiXem(id, nguoiDung.id());
                        ho.addProperty("daChan", tinNhanDao.daChan(nguoiDung.id(), id));
                        thanhCong(yeuCau, DuLieu.tao("hoSo", ho));
                    }
                    case "thongTinChat" -> {
                        canDangNhap();
                        thanhCong(yeuCau, thongTinChatDao.doc(nguoiDung.id(),
                                DuLieu.id(duLieu, "nguoiNhanId", true), DuLieu.chu(duLieu, "loai"),
                                DuLieu.id(duLieu, "truocId", true)));
                    }
                    case "ghimTin", "boGhimTin" -> {
                        canDangNhap();
                        long id = DuLieu.id(duLieu, "tinNhanId", false);
                        synchronized (khoTuongTac) {
                            thongTinChatDao.ghim(nguoiDung.id(), id,
                                    yeuCau.loai().equals("boGhimTin"));
                        }
                        JsonObject tin = khoTuongTac.doc(id, nguoiDung.id());
                        Long nhan = DuLieu.id(tin, "nguoiNhanId", true);
                        long gui = Long.parseLong(DuLieu.chu(tin, "nguoiGuiId"));
                        for (Phien client : cacClient) {
                            if (client.nguoiDung != null && (nhan == null || client.nguoiDung.id() == nhan ||
                                    client.nguoiDung.id() == gui)) {
                                client.gui(ThongDiep.suKien("ghimDoi", DuLieu.tao("tinNhan", tin)));
                            }
                        }
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "luuQuyenRiengTu" -> {
                        canDangNhap();
                        khoHoSo.luuQuyen(nguoiDung.id(), duLieu);
                        JsonObject ho = khoHoSo.doc(nguoiDung.id());
                        for (Phien client : cacClient) {
                            if (client.nguoiDung != null && client.nguoiDung.id() == nguoiDung.id()) {
                                client.gui(ThongDiep.suKien("hoSoDoi", DuLieu.tao("hoSo", ho)));
                            }
                        }
                        thanhCong(yeuCau, DuLieu.tao("hoSo", ho));
                    }
                    case "layHoSo" -> {
                        canDangNhap();
                        thanhCong(yeuCau, DuLieu.tao("hoSo", khoHoSo.doc(nguoiDung.id())));
                    }
                    case "suaHoSo" -> {
                        canDangNhap();
                        khoHoSo.capNhat(nguoiDung.id(), duLieu);
                        JsonObject hoSo = khoHoSo.doc(nguoiDung.id());
                        for (Phien client : cacClient) {
                            NguoiDungModel nguoi = client.nguoiDung;
                            if (nguoi != null && nguoi.id() == nguoiDung.id()) {
                                client.nguoiDung =
                                        new NguoiDungModel(nguoi.id(), DuLieu.chu(hoSo, "tenHienThi"),
                                                nguoi.email(), nguoi.matKhauBam());
                                client.gui(ThongDiep.suKien("hoSoDoi", DuLieu.tao("hoSo", hoSo)));
                            }
                        }
                        thanhCong(yeuCau, DuLieu.tao("hoSo", hoSo));
                        phatDanhSach();
                    }
                    case "doiMatKhau" -> {
                        canDangNhap();
                        khoHoSo.doiMatKhau(nguoiDung.id(), DuLieu.chu(duLieu, "matKhauCu"),
                                DuLieu.chu(duLieu, "matKhauMoi"), bamPhien);
                        for (Phien client : cacClient) {
                            NguoiDungModel nguoi = client.nguoiDung;
                            if (nguoi != null && nguoi.id() == nguoiDung.id() &&
                                    !Objects.equals(client.bamPhien, bamPhien)) {
                                client.nguoiDung = null;
                                client.bamPhien = null;
                                goiThoai.roi(client);
                                client.boNhanTep.close();
                                client.gui(ThongDiep.suKien("phienBiHuy", DuLieu.tao("thongBao",
                                        "Mật khẩu đã được thay đổi. Hãy đăng nhập lại.")));
                            }
                        }
                        thanhCong(yeuCau, new JsonObject());
                        phatDanhSach();
                    }
                    case "moiGoiThoai", "batMayGoiThoai", "ketThucGoiThoai" -> xuLyGoiThoai(yeuCau, duLieu);
                    case "nhipTim" -> {
                        canDangNhap();
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "layDanhSach" -> {
                        canDangNhap();
                        phatDanhSach();
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "timTaiKhoan" -> {
                        canDangNhap();
                        String tuKhoa = DuLieu.chu(duLieu, "tuKhoa").strip();
                        if (tuKhoa.isEmpty()) {
                            tuKhoa = DuLieu.chu(duLieu, "email").strip();
                        }
                        NguoiDungModel nguoi;
                        if (tuKhoa.contains("@")) {
                            String email = tuKhoa.toLowerCase(Locale.ROOT);
                            if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                                throw new LoiNghiepVuModel("timKiemSai", "Email chưa hợp lệ.");
                            }
                            nguoi = nguoiDungDao.timTheoEmail(email);
                        } else {
                            String so = tuKhoa.length() <= 64 ? tuKhoa.replaceAll("[ ()-]", "") : "";
                            if (!so.matches("[+]?[0-9]{8,15}")) {
                                throw new LoiNghiepVuModel("timKiemSai",
                                        "Nhập email hoặc số điện thoại hợp lệ.");
                            }
                            nguoi = nguoiDungDao.timTheoSoDienThoai(nguoiDung.id(), so);
                        }
                        JsonObject ketQua = null;
                        if (nguoi != null && nguoi.id() != nguoiDung.id()) {
                            ketQua = nguoi.congKhai(cacIdOnline().contains(nguoi.id()));
                            ketQua.addProperty("quanHe", khoBanBe.quanHe(nguoiDung.id(), nguoi.id()));
                        }
                        thanhCong(yeuCau, DuLieu.tao("timThay", ketQua != null, "nguoiDung", ketQua));
                    }
                    case "layBanBe" -> {
                        canDangNhap();
                        Long sauId = DuLieu.id(duLieu, "sauId", true);
                        thanhCong(yeuCau, DuLieu.tao("banBe",
                                khoBanBe.danhSach(nguoiDung.id(), sauId == null ? 0 : sauId, cacIdOnline())));
                    }
                    case "thaoTacBanBe" -> {
                        canDangNhap();
                        long doiPhuong = DuLieu.id(duLieu, "doiPhuongId", false);
                        khoBanBe.thaoTac(nguoiDung.id(), doiPhuong, DuLieu.chu(duLieu, "thaoTac"));
                        baoQuanHeDoi(nguoiDung.id(), doiPhuong);
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "chanNguoiDung", "boChanNguoiDung", "xoaHoiThoai" -> {
                        canDangNhap();
                        long dich = DuLieu.id(duLieu, "nguoiNhanId", false), nguoi = nguoiDung.id();
                        if (dich == nguoi) {
                            throw new IllegalArgumentException();
                        }
                        synchronized (khoTuongTac) {
                            if (yeuCau.loai().equals("xoaHoiThoai")) {
                                tinNhanDao.xoaHoiThoai(nguoi, dich);
                                for (Phien client : cacClient) {
                                    if (client.nguoiDung != null && client.nguoiDung.id() == nguoi) {
                                        client.gui(ThongDiep.suKien("hoiThoaiBiXoa",
                                                DuLieu.tao("nguoiNhanId", Long.toString(dich))));
                                    }
                                }
                            } else {
                                khoHoSo.doc(dich);
                                tinNhanDao.datChan(nguoi, dich, yeuCau.loai().equals("chanNguoiDung"));
                                if (yeuCau.loai().equals("chanNguoiDung")) {
                                    goiThoai.khiChan(nguoi, dich);
                                }
                            }
                            thanhCong(yeuCau, new JsonObject());
                        }
                    }
                    case "luuTruHoiThoai", "boLuuTruHoiThoai" -> {
                        canDangNhap();
                        long dich = DuLieu.id(duLieu, "nguoiNhanId", false);
                        if (dich == nguoiDung.id()) {
                            throw new LoiNghiepVuModel("khongCoNguoiNhan",
                                    "Không tìm thấy cuộc trò chuyện.");
                        }
                        khoHoSo.doc(dich);
                        tinNhanDao.datLuuTru(nguoiDung.id(), dich, yeuCau.loai().equals("luuTruHoiThoai"));
                        for (Phien client : cacClient) {
                            if (client.nguoiDung != null && client.nguoiDung.id() == nguoiDung.id()) {
                                client.gui(ThongDiep.suKien("luuTruDoi", new JsonObject()));
                            }
                        }
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "taoNhom" -> {
                        canDangNhap();
                        JsonElement thanhVien = duLieu.get("thanhVien");
                        if (thanhVien == null || !thanhVien.isJsonArray() ||
                                thanhVien.getAsJsonArray().size() > 29) {
                            throw new IllegalArgumentException();
                        }
                        Set<Long> cacBan = new LinkedHashSet<>();
                        for (JsonElement id : thanhVien.getAsJsonArray()) {
                            if (!id.isJsonPrimitive()) {
                                throw new IllegalArgumentException();
                            }
                            long ma = Long.parseLong(id.getAsString());
                            if (ma <= 0) {
                                throw new IllegalArgumentException();
                            }
                            cacBan.add(ma);
                        }
                        synchronized (khoNhom) {
                            long id = khoNhom.tao(nguoiDung.id(), DuLieu.chu(duLieu, "ten"),
                                    maNhomHopLe(duLieu, "maTao"), cacBan,
                                    khoHoSo.kiemTraAnh(DuLieu.chu(duLieu, "anhDaiDien")));
                            JsonObject nhom = khoNhom.tomTat(id);
                            phatSuKienNhom(id, "nhomDoi", DuLieu.tao("nhom", nhom));
                            thanhCong(yeuCau, DuLieu.tao("nhom", nhom));
                        }
                    }
                    case "layNhom" -> {
                        canDangNhap();
                        thanhCong(yeuCau, DuLieu.tao("cacNhom",
                                khoNhom.danhSach(nguoiDung.id(), DuLieu.id(duLieu, "sauId", true))));
                    }
                    case "layAnhNhom" -> {
                        canDangNhap();
                        thanhCong(yeuCau, DuLieu.tao("anhDaiDien",
                                khoNhom.anh(nguoiDung.id(), DuLieu.id(duLieu, "nhomId", false))));
                    }
                    case "lichSuNhom" -> {
                        canDangNhap();
                        thanhCong(yeuCau, khoNhom.lichSu(nguoiDung.id(), DuLieu.id(duLieu, "nhomId", false),
                                DuLieu.id(duLieu, "truocId", true)));
                    }
                    case "guiTinNhom" -> {
                        canDangNhap();
                        long id = DuLieu.id(duLieu, "nhomId", false);
                        synchronized (khoNhom) {
                            var ketQua =
                                    khoNhom.gui(nguoiDung.id(), id, DuLieu.chu(duLieu, "noiDung").strip(),
                                            maNhomHopLe(duLieu, "maGui"));
                            if (ketQua.taoMoi()) {
                                phatSuKienNhom(id, "tinNhanNhomMoi",
                                        DuLieu.tao("nhom", khoNhom.tomTat(id), "tinNhan", ketQua.tinNhan()));
                            }
                            thanhCong(yeuCau, DuLieu.tao("tinNhan", ketQua.tinNhan()));
                        }
                    }
                    case "layHoiThoai" -> {
                        canDangNhap();
                        String chu = DuLieu.chu(duLieu, "boQua");
                        if (!chu.isEmpty() && !chu.matches("[0-9]{1,6}")) {
                            throw new IllegalArgumentException("Mốc phân trang sai.");
                        }
                        thanhCong(yeuCau, DuLieu.tao("hoiThoai",
                                tinNhanDao.hoiThoai(nguoiDung.id(), chu.isEmpty() ? 0 : Integer.parseInt(chu))));
                    }
                    case "danhDauDoc" -> {
                        canDangNhap();
                        tinNhanDao.danhDauDoc(nguoiDung.id(), DuLieu.id(duLieu, "nguoiNhanId", true),
                                DuLieu.id(duLieu, "tinCuoiId", false));
                        for (Phien client : cacClient) {
                            if (client.nguoiDung != null && client.nguoiDung.id() == nguoiDung.id()) {
                                client.gui(ThongDiep.suKien("mocDocDoi", new JsonObject()));
                            }
                        }
                        Long doiPhuong = DuLieu.id(duLieu, "nguoiNhanId", true);
                        if (doiPhuong != null) {
                            for (Phien client : cacClient) {
                                if (client.nguoiDung != null && client.nguoiDung.id() == doiPhuong) {
                                    client.gui(ThongDiep.suKien("doiPhuongDaDoc",
                                            DuLieu.tao("nguoiDocId", Long.toString(nguoiDung.id()),
                                                    "tinCuoiId", DuLieu.chu(duLieu, "tinCuoiId"))));
                                }
                            }
                        }
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "lichSu" -> {
                        canDangNhap();
                        Long dich = DuLieu.id(duLieu, "nguoiNhanId", true);
                        Long truoc = DuLieu.id(duLieu, "truocId", true);
                        synchronized (khoTuongTac) {
                            JsonArray cacTin = tinNhanDao.lichSu(nguoiDung.id(), dich, truoc);
                            List<JsonObject> boSung = new ArrayList<>();
                            cacTin.forEach(tin -> boSung.add(tin.getAsJsonObject()));
                            khoTuongTac.boSung(boSung, nguoiDung.id());
                            while (cacTin.size() > 1 && new Gson().toJson(cacTin)
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 50000) {
                                cacTin.remove(0);
                            }
                            thanhCong(yeuCau, DuLieu.tao("tinNhan", cacTin, "coTinCu",
                                    tinNhanDao.conTinCu(nguoiDung.id(), dich, cacTin)));
                        }
                    }
                    case "tepBatDau" -> {
                        canDangNhap();
                        synchronized (khoTuongTac) {
                            JsonObject tra = boNhanTep.batDau(nguoiDung.id(), duLieu);
                            if (tra.has("tinNhan")) {
                                khoTuongTac.boSung(List.of(tra.getAsJsonObject("tinNhan")), nguoiDung.id());
                            }
                            thanhCong(yeuCau, tra);
                        }
                    }
                    case "tepGuiKhoi" -> {
                        canDangNhap();
                        thanhCong(yeuCau, boNhanTep.nhanKhoi(nguoiDung.id(), duLieu));
                    }
                    case "tepKetThuc" -> {
                        canDangNhap();
                        synchronized (khoTuongTac) {
                            KetQuaLuuModel ketQua = boNhanTep.ketThuc(nguoiDung.id());
                            JsonObject tin = ketQua.tinNhan();
                            khoTuongTac.boSung(List.of(tin), nguoiDung.id());
                            Long dich = DuLieu.id(tin, "nguoiNhanId", true);
                            if (ketQua.taoMoi()) {
                                phatTin(tin, dich, nguoiDung.id());
                            }
                            thanhCong(yeuCau, DuLieu.tao("tinNhan", tin));
                        }
                    }
                    case "tepHuy" -> {
                        canDangNhap();
                        boNhanTep.close();
                        thanhCong(yeuCau, new JsonObject());
                    }
                    case "tepThongTin" -> {
                        canDangNhap();
                        thanhCong(yeuCau,
                                khoTep.thongTinTai(nguoiDung.id(), DuLieu.id(duLieu, "tinNhanId", false)));
                    }
                    case "tepDocKhoi" -> {
                        canDangNhap();
                        thanhCong(yeuCau,
                                khoTep.docKhoi(nguoiDung.id(), DuLieu.id(duLieu, "tinNhanId", false),
                                        TepModel.so(duLieu, "viTri")));
                    }
                    case "tepAnhThu" -> {
                        canDangNhap();
                        thanhCong(yeuCau,
                                khoTep.anhThu(nguoiDung.id(), DuLieu.id(duLieu, "tinNhanId", false)));
                    }
                    case "guiTin" -> {
                        canDangNhap();
                        Long dich = DuLieu.id(duLieu, "nguoiNhanId", true);
                        if (dich != null && dich == nguoiDung.id()) {
                            throw new LoiNghiepVuModel("nguoiNhanSai",
                                    "Chọn một người khác để trò chuyện.");
                        }
                        String noiDung = DuLieu.chu(duLieu, "noiDung").trim();
                        String maGui = DuLieu.chu(duLieu, "maGui");
                        if (noiDung.isEmpty() || noiDung.length() > 4000 ||
                                !maGui.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) {
                            throw new LoiNghiepVuModel("tinSai",
                                    "Tin nhắn cần 1–4.000 ký tự và mã gửi UUID.");
                        }
                        synchronized (khoTuongTac) {
                            KetQuaLuuModel ketQua = tinNhanDao.luuTin(nguoiDung.id(), dich, maGui, noiDung,
                                    DuLieu.id(duLieu, "traLoiId", true));
                            JsonObject tin = ketQua.tinNhan();
                            khoTuongTac.boSung(List.of(tin), nguoiDung.id());
                            if (ketQua.taoMoi()) {
                                phatTin(tin, dich, nguoiDung.id());
                            }
                            thanhCong(yeuCau, DuLieu.tao("tinNhan", tin));
                        }
                    }
                    case "xoaTin" -> {
                        canDangNhap();
                        long tinId = DuLieu.id(duLieu, "tinNhanId", false);
                        synchronized (khoTuongTac) {
                            tinNhanDao.anTin(nguoiDung.id(), tinId);
                            for (Phien client : cacClient) {
                                if (client.nguoiDung != null && client.nguoiDung.id() == nguoiDung.id()) {
                                    client.gui(ThongDiep.suKien("tinBiXoa",
                                            DuLieu.tao("tinNhanId", Long.toString(tinId))));
                                }
                            }
                            thanhCong(yeuCau, new JsonObject());
                        }
                    }
                    case "chuyenTiepTin" -> {
                        canDangNhap();
                        long tinId = DuLieu.id(duLieu, "tinNhanId", false);
                        Long dich = DuLieu.id(duLieu, "nguoiNhanId", true);
                        if (dich != null && dich == nguoiDung.id()) {
                            throw new IllegalArgumentException();
                        }
                        String ma = DuLieu.chu(duLieu, "maGui");
                        if (!ma.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) {
                            throw new IllegalArgumentException();
                        }
                        synchronized (khoTuongTac) {
                            if (tinNhanDao.daAnTin(nguoiDung.id(), tinId)) {
                                throw new LoiNghiepVuModel("tinKhongCon", "Tin nhắn đã bị xóa ở phía bạn.");
                            }
                            JsonObject goc = khoTuongTac.doc(tinId, nguoiDung.id());
                            if (goc.get("daThuHoi").getAsBoolean()) {
                                throw new LoiNghiepVuModel("tinDaThuHoi", "Tin gốc đã được thu hồi.");
                            }
                            KetQuaLuuModel ketQua = goc.has("tep") ?
                                    khoTep.chuyenTiep(nguoiDung.id(), tinId, duLieu,
                                            DuLieu.chu(goc, "noiDung")) :
                                    tinNhanDao.luuTin(nguoiDung.id(), dich, ma, DuLieu.chu(goc, "noiDung"));
                            JsonObject tin = ketQua.tinNhan();
                            if (ketQua.taoMoi()) {
                                phatTin(tin, dich, nguoiDung.id());
                            }
                            thanhCong(yeuCau, DuLieu.tao("tinNhan", tin));
                        }
                    }
                    case "thuHoiTin", "datCamXuc" -> {
                        canDangNhap();
                        long tinId = DuLieu.id(duLieu, "tinNhanId", false);
                        synchronized (khoTuongTac) {
                            if (yeuCau.loai().equals("thuHoiTin")) {
                                khoTuongTac.thuHoi(nguoiDung.id(), tinId);
                                khoTep.xoaSauThuHoi(tinId);
                            } else {
                                khoTuongTac.datCamXuc(nguoiDung.id(), tinId, DuLieu.chu(duLieu, "bieuCam"));
                            }
                            JsonObject tin = khoTuongTac.doc(tinId, nguoiDung.id());
                            phatTinDoi(tin);
                            thanhCong(yeuCau, DuLieu.tao("tinNhan", tin));
                        }
                    }
                    case "dangXuat" -> {
                        canDangNhap();
                        khoPhien.huy(bamPhien);
                        boNhanTep.close();
                        bamPhien = null;
                        nguoiDung = null;
                        goiThoai.roi(this);
                        thanhCong(yeuCau, new JsonObject());
                        phatDanhSach();
                    }
                    default -> throw new LoiNghiepVuModel("lenhSai", "Không hỗ trợ yêu cầu này.");
                }
            } catch (LoiNghiepVuModel loi) {
                thatBai(yeuCau, loi.ma(), loi.getMessage());
            } catch (IllegalArgumentException loi) {
                thatBai(yeuCau, "duLieuSai", "Dữ liệu gửi lên chưa hợp lệ.");
            } catch (SQLException loi) {
                thatBai(yeuCau, "csdlGianDoan", "Chưa lưu/đọc được dữ liệu. Bạn thử lại nhé.");
            } catch (IOException loi) {
                boNhanTep.close();
                thatBai(yeuCau, "tepGianDoan", "Chưa đọc/ghi được tệp. Bạn thử lại nhé.");
            } catch (RuntimeException loi) {
                thatBai(yeuCau, "xuLyLoi", "Chưa xử lý được yêu cầu.");
            }
        }

        private void xuLyGoiThoai(ThongDiep yeuCau, JsonObject duLieu) throws SQLException, IOException {
            canDangNhap();
            UUID id = UUID.fromString(DuLieu.chu(duLieu, "cuocGoiId"));
            switch (yeuCau.loai()) {
                case "moiGoiThoai" -> {
                    long nguoiNhan = DuLieu.id(duLieu, "nguoiNhanId", false);
                    int port = docPortThoai(duLieu);
                    NguoiDungModel nguoiGoi = nguoiDung;
                    if (nguoiGoi == null || nguoiGoi.id() == nguoiNhan) {
                        throw new IllegalArgumentException();
                    }
                    synchronized (khoTuongTac) {
                        if (tinNhanDao.daChan(nguoiGoi.id(), nguoiNhan)
                                || tinNhanDao.daChan(nguoiNhan, nguoiGoi.id())) {
                            throw new LoiNghiepVuModel("khongTheGoiThoai", "Không thể gọi người này.");
                        }
                        synchronized (goiThoai) {
                            for (Phien client : cacClient) {
                                NguoiDungModel dich = client.nguoiDung;
                                if (dich != null && dich.id() == nguoiNhan
                                        && khoPhien.conHieuLuc(client.bamPhien, dich.id())
                                        && client.nguoiDung == dich && nguoiDung == nguoiGoi) {
                                    goiThoai.moi(id, dauGoi(nguoiGoi), client.dauGoi(dich), port);
                                    thanhCong(yeuCau, new JsonObject());
                                    return;
                                }
                            }
                        }
                    }
                    throw new LoiNghiepVuModel("nguoiNhanNgoaiTuyen", "Người nhận đang ngoại tuyến.");
                }
                case "batMayGoiThoai" -> goiThoai.batMay(id, this, docPortThoai(duLieu));
                case "ketThucGoiThoai" -> goiThoai.ketThuc(id, this);
                default -> throw new IllegalArgumentException();
            }
            thanhCong(yeuCau, new JsonObject());
        }

        private GoiThoaiController.DauGoi dauGoi(NguoiDungModel nguoi) throws IOException {
            return new GoiThoaiController.DauGoi(this, nguoi.id(), nguoi.ten(),
                    InetAddress.getByName(diaChi()), this::gui);
        }

        private int docPortThoai(JsonObject duLieu) {
            int port = Integer.parseInt(DuLieu.chu(duLieu, "port"));
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException();
            }
            return port;
        }

        private void xacThuc(ThongDiep yeuCau, JsonObject duLieu) throws SQLException, IOException {
            if (nguoiDung != null) {
                throw new LoiNghiepVuModel("daDangNhap", "Bạn đang đăng nhập.");
            }
            if (!choPhepDangNhap(diaChi())) {
                throw new LoiNghiepVuModel("quaNhieuLanThu",
                        "Bạn thử quá nhiều lần. Chờ một phút nhé.");
            }

            String email = DuLieu.chu(duLieu, "email").trim().toLowerCase(Locale.ROOT);
            String matKhau = DuLieu.chu(duLieu, "matKhau");
            if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") ||
                    matKhau.length() < 8 || matKhau.length() > 128) {
                throw new LoiNghiepVuModel("duLieuSai",
                        "Kiểm tra email và mật khẩu (8–128 ký tự).");
            }

            if (yeuCau.loai().equals("dangKy")) {
                String ten = DuLieu.chu(duLieu, "tenHienThi").trim();
                if (ten.length() < 2 || ten.length() > 100 ||
                        ten.chars().anyMatch(Character::isISOControl)) {
                    throw new LoiNghiepVuModel("tenSai", "Tên hiển thị cần từ 2 đến 100 ký tự.");
                }
                nguoiDungDao.themNguoi(ten, email, BaoMat.bam(matKhau));
                thanhCong(yeuCau, DuLieu.tao("thongBao",
                        "Tạo tài khoản thành công. Bạn có thể đăng nhập."));
                phatDanhSach();
            } else {
                PhienDao.PhienDaTao phien = khoPhien.dangNhap(email, matKhau);
                nguoiDung = phien.nguoi();
                bamPhien = PhienDao.bam(phien.ma());
                datHanDoc(65000);
                thanhCong(yeuCau,
                        DuLieu.tao("nguoiDung", nguoiDung.rieng(), "maPhien", phien.ma()));
                phatDanhSach();
            }
        }

        private void canDangNhap() throws SQLException {
            if (nguoiDung == null) {
                throw new LoiNghiepVuModel("chuaDangNhap", "Bạn cần đăng nhập trước.");
            }
            if (!khoPhien.conHieuLuc(bamPhien, nguoiDung.id())) {
                throw new LoiNghiepVuModel("phienKhongHopLe",
                        "Phiên đăng nhập đã bị thu hồi. Hãy đăng nhập lại.");
            }
        }

        private void thanhCong(ThongDiep yeuCau, JsonObject duLieu) {
            duLieu.addProperty("thanhCong", true);
            gui(new ThongDiep("phanHoi", yeuCau.ma(), duLieu));
        }

        protected final void thatBai(ThongDiep yeuCau, String ma, String thongBao) {
            gui(new ThongDiep("phanHoi", yeuCau.ma(),
                    DuLieu.tao("thanhCong", false, "maLoi", ma, "thongBao", thongBao)));
        }
    }
}
