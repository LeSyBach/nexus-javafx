package client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HoiThoaiModelTest {
    @Test
    void lichSuCuKhongGhiDeTinDaCapNhat() {
        LichSuTinNhanModel model = new LichSuTinNhanModel();
        model.ghiNhanTin(tin("2", "1", "Cũ", false));
        assertTrue(model.nhanTinDoi(tin("2", "3", "Đã thu hồi", true)));
        assertFalse(model.nhanTinDoi(tin("2", "2", "Cập nhật cũ", false)));
        model.ghiNhanTin(tin("2", "1", "Cũ", false));

        assertTrue(LichSuTinNhanModel.daThuHoi(model.timTin("2")));
        assertEquals("3", DuLieu.chu(model.timTin("2"), "phienBan"));
        model.xoaTin("2");
        assertTrue(model.rong());
    }

    @Test
    void capNhatTrichDanKhiTinGocBiThuHoi() {
        LichSuTinNhanModel model = new LichSuTinNhanModel();
        JsonObject goc = tin("5", "1", "Nội dung ban đầu", false);
        JsonObject traLoi = tin("8", "1", "Đã đọc", false);
        traLoi.addProperty("traLoiId", "5");
        traLoi.add("trichDan", goc.deepCopy());
        model.ghiNhanTin(goc);
        model.ghiNhanTin(traLoi);
        model.nhanTinDoi(tin("5", "2", "Đã thu hồi", true));

        JsonObject trichDan = model.timTin("8").getAsJsonObject("trichDan");
        assertTrue(LichSuTinNhanModel.daThuHoi(trichDan));
        assertEquals("Đã thu hồi", DuLieu.chu(trichDan, "noiDung"));
        assertEquals("5", model.maTinCuNhat());
        assertEquals(8, model.maTinMoiNhat());
    }

    @Test
    void mocDocChiTangVaBoDungLanGuiLoi() {
        LichSuTinNhanModel model = new LichSuTinNhanModel();
        assertTrue(model.ghiMocDaGui(null, 3));
        assertFalse(model.ghiMocDaGui(null, 3));
        assertTrue(model.ghiMocDaGui(null, 4));
        model.boMocDaGui(null, 3);
        assertFalse(model.ghiMocDaGui(null, 4));
        model.boMocDaGui(null, 4);
        assertTrue(model.ghiMocDaGui(null, 4));

        model.capNhatMocDoiPhuong(7);
        model.capNhatMocDoiPhuong(6);
        assertTrue(model.doiPhuongDaXem("7"));
        model.xoaTrangDangMo();
        assertFalse(model.doiPhuongDaXem("7"));
    }

    @Test
    void locDanhSachTachHoiThoaiLuuTruKhoiTinNhan() {
        DanhSachHoiThoaiModel model = new DanhSachHoiThoaiModel();
        JsonArray nguoi = new JsonArray();
        nguoi.add(DuLieu.tao("id", "1", "tenHienThi", "Tôi", "dangOnline", true));
        nguoi.add(DuLieu.tao("id", "2", "tenHienThi", "Bạn Hai", "dangOnline", true));
        model.capNhatNguoi(nguoi, "1");
        model.capNhatHoiThoai(Map.of(
                "2", hoiThoai("2", "Bạn Hai", false),
                "3", hoiThoai("3", "Bạn Ba", true)
        ));

        assertTrue(model.timNguoi("1").isEmpty());
        assertEquals("2", DuLieu.chu(model.loc("hai", false, null).get(0), "id"));
        assertEquals("3", DuLieu.chu(model.loc("", true, null).get(0), "id"));
        assertTrue(model.loc("ba", false, null).isEmpty());
    }

    @Test
    void cacheAnhUuTienAnhVuaTruyCap() {
        AnhTinNhanModel model = new AnhTinNhanModel();
        byte[] anh = {1};
        for (int i = 0; i < 128; i++) {
            model.luuAnh(Integer.toString(i), anh);
        }
        assertSame(anh, model.layAnh("0"));
        model.luuAnh("moi", anh);
        assertNull(model.layAnh("1"));
        assertSame(anh, model.layAnh("0"));
        model.xoaAnh("0");
        assertNull(model.layAnh("0"));
    }

    private JsonObject tin(String id, String phienBan, String chu, boolean daThuHoi) {
        return DuLieu.tao("id", id, "phienBan", phienBan, "noiDung", chu,
                "daThuHoi", daThuHoi, "nguoiGuiId", "1", "tenNguoiGui", "Bạn");
    }

    private JsonObject hoiThoai(String id, String ten, boolean luuTru) {
        return DuLieu.tao("id", id, "tenHienThi", ten, "daLuuTru", luuTru,
                "tinCuoi", DuLieu.tao("taoLuc", "2026-11-07T00:00:00Z"));
    }
}
