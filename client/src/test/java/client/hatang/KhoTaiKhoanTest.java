package client.hatang;

import client.model.TaiKhoanModel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KhoTaiKhoanTest {
    @TempDir
    Path thuMuc;

    @Test
    void maHoaDPAPIVaXoaRiengMotTaiKhoan() throws Exception {
        Path tep = thuMuc.resolve("tai-khoan.dat");
        TaiKhoanModel kho = new TaiKhoanModel(tep);
        var a = new TaiKhoanModel.TaiKhoan("localhost", 4040, "1", "Bách", "bach@example.invalid",
                "A".repeat(43));
        var b = new TaiKhoanModel.TaiKhoan("localhost", 4040, "2", "Hiếu", "hieu@example.invalid",
                "B".repeat(43));
        assertTrue(kho.doc().isEmpty());
        kho.ghi(List.of(a, b));
        String tho = Files.readString(tep);
        assertFalse(tho.contains(a.maPhien()));
        assertFalse(tho.contains(a.email()));
        assertEquals(List.of(a, b), kho.doc());
        kho.ghi(List.of(b));
        assertEquals(List.of(b), kho.doc());
        assertFalse(a.toString().contains(a.maPhien()));
    }

    @Test
    void tepHongKhongTraVePhienGia() throws Exception {
        Path tep = thuMuc.resolve("hong.dat");
        Files.writeString(tep, "du lieu hong");
        assertThrows(java.io.IOException.class, () -> new TaiKhoanModel(tep).doc());
    }
}
