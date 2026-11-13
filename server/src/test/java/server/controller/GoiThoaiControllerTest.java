package server.controller;

import giaothuc.DuLieu;
import giaothuc.ThongDiep;
import org.junit.jupiter.api.Test;
import server.model.LoiNghiepVuModel;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GoiThoaiControllerTest {
    private final GoiThoaiController dieuKhien = new GoiThoaiController();
    private final List<ThongDiep> tinA = new ArrayList<>();
    private final List<ThongDiep> tinB = new ArrayList<>();
    private final Object phienA = new Object();
    private final Object phienB = new Object();

    @Test
    void chiTraDiaChiAmThanhKhiBenNhanBatMay() throws Exception {
        UUID id = moiGoi();
        assertEquals("cuocGoiDen", tinB.get(0).loai());
        assertFalse(tinB.get(0).duLieu().has("host"));
        assertFalse(tinB.get(0).duLieu().has("mediaToken"));
        assertThrows(LoiNghiepVuModel.class, () -> dieuKhien.batMay(id, phienA, 5100));

        dieuKhien.batMay(id, phienB, 5101);
        assertEquals("cuocGoiBatDau", tinA.get(0).loai());
        assertEquals("192.168.1.11", DuLieu.chu(tinA.get(0).duLieu(), "host"));
        assertEquals(5101, tinA.get(0).duLieu().get("port").getAsInt());
        assertEquals(5100, tinB.get(1).duLieu().get("port").getAsInt());
        assertEquals(DuLieu.chu(tinA.get(0).duLieu(), "mediaToken"),
                DuLieu.chu(tinB.get(1).duLieu(), "mediaToken"));
        assertThrows(LoiNghiepVuModel.class, () -> dieuKhien.batMay(id, phienB, 5101));

        dieuKhien.ketThuc(id, new Object());
        assertEquals(1, tinA.size());
        dieuKhien.ketThuc(id, phienA);
        assertEquals("cuocGoiKetThuc", tinB.get(2).loai());
        moiGoi();
    }

    @Test
    void tuChoiVaMatKetNoiGiaiPhongCuocGoi() throws Exception {
        UUID id = moiGoi();
        assertThrows(LoiNghiepVuModel.class, this::moiGoi);
        dieuKhien.ketThuc(id, phienB);
        assertEquals("Người nhận đã từ chối cuộc gọi.", DuLieu.chu(tinA.get(0).duLieu(), "thongBao"));

        UUID lanHai = moiGoi();
        dieuKhien.roi(phienA);
        assertEquals("Đối phương đã ngắt kết nối.", DuLieu.chu(tinB.get(3).duLieu(), "thongBao"));
        assertThrows(LoiNghiepVuModel.class, () -> dieuKhien.batMay(lanHai, phienB, 5101));
        moiGoi();
    }

    @Test
    void chanTrongKhiDangGoiKetThucPhien() throws Exception {
        UUID id = moiGoi();
        dieuKhien.batMay(id, phienB, 5101);
        dieuKhien.khiChan(2, 1);
        assertEquals("cuocGoiKetThuc", tinA.get(1).loai());
        assertEquals("cuocGoiKetThuc", tinB.get(2).loai());
    }

    private UUID moiGoi() throws Exception {
        UUID id = UUID.randomUUID();
        var a = new GoiThoaiController.DauGoi(phienA, 1, "A",
                InetAddress.getByName("192.168.1.10"), tinA::add);
        var b = new GoiThoaiController.DauGoi(phienB, 2, "B",
                InetAddress.getByName("192.168.1.11"), tinB::add);
        dieuKhien.moi(id, a, b, 5100);
        return id;
    }
}
