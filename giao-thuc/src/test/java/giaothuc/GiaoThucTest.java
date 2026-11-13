package giaothuc;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class GiaoThucTest {
    private byte[] dongGoi(ThongDiep thongDiep) throws IOException {
        ByteArrayOutputStream cacByte = new ByteArrayOutputStream();
        GiaoThuc.ghi(new DataOutputStream(cacByte), thongDiep);
        return cacByte.toByteArray();
    }

    @Test
    void nhanKhungBiChiaNhoVanGiuTiengVietVaBieuCam() throws Exception {
        byte[] goi = dongGoi(ThongDiep.yeuCau("guiTin", DuLieu.tao("noiDung", "Xin chào Việt Nam 😊")));
        InputStream biChia = new ByteArrayInputStream(goi) {
            @Override
            public synchronized int read(byte[] dich, int batDau, int doDai) {
                return super.read(dich, batDau, Math.min(2, doDai));
            }
        };
        ThongDiep doc = GiaoThuc.doc(new DataInputStream(biChia));
        assertEquals("guiTin", doc.loai());
        assertEquals("Xin chào Việt Nam 😊", DuLieu.chu(doc.duLieu(), "noiDung"));
    }

    @Test
    void nhieuKhungGhepTrongMotLanNhanDuocTachDung() throws Exception {
        ByteArrayOutputStream cacByte = new ByteArrayOutputStream();
        DataOutputStream ghi = new DataOutputStream(cacByte);
        GiaoThuc.ghi(ghi, ThongDiep.yeuCau("guiTin", DuLieu.tao("noiDung", "Một")));
        GiaoThuc.ghi(ghi, ThongDiep.yeuCau("guiTin", DuLieu.tao("noiDung", "Hai")));
        DataInputStream doc = new DataInputStream(new ByteArrayInputStream(cacByte.toByteArray()));
        assertEquals("Một", DuLieu.chu(GiaoThuc.doc(doc).duLieu(), "noiDung"));
        assertEquals("Hai", DuLieu.chu(GiaoThuc.doc(doc).duLieu(), "noiDung"));
        assertEquals(0, doc.available());
    }

    @Test
    void tuChoiDoDaiAmRongVaQuaLon() throws Exception {
        for (int doDai : new int[]{-1, 0, 65537}) {
            ByteArrayOutputStream cacByte = new ByteArrayOutputStream();
            new DataOutputStream(cacByte).writeInt(doDai);
            assertThrows(IOException.class,
                    () -> GiaoThuc.doc(new DataInputStream(new ByteArrayInputStream(cacByte.toByteArray()))));
        }
    }

    @Test
    void khungThieuByteKhongBiHieuLaTinHoanChinh() throws Exception {
        byte[] goi = dongGoi(ThongDiep.yeuCau("guiTin", DuLieu.tao("noiDung", "Một tin")));
        assertThrows(EOFException.class, () -> GiaoThuc.doc(
                new DataInputStream(new ByteArrayInputStream(java.util.Arrays.copyOf(goi, goi.length - 1)))));
    }

    @Test
    void tuChoiJsonSaiVaUtf8Hong() throws Exception {
        for (byte[] duLieu : new byte[][]{"[]".getBytes(), "{\"loai\":\"guiTin\"}".getBytes(), {(byte) 0xc3, (byte) 0x28}}) {
            ByteArrayOutputStream cacByte = new ByteArrayOutputStream();
            DataOutputStream ghi = new DataOutputStream(cacByte);
            ghi.writeInt(duLieu.length);
            ghi.write(duLieu);
            assertThrows(IOException.class,
                    () -> GiaoThuc.doc(new DataInputStream(new ByteArrayInputStream(cacByte.toByteArray()))));
        }
    }
}
