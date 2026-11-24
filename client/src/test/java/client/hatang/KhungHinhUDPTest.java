package client.hatang;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class KhungHinhUDPTest {
    private final UUID token = UUID.randomUUID();

    @Test
    void ghepGoiDaoThuTuVaBoQuaGoiTrung() {
        byte[] anh = new byte[5000];
        new Random(42).nextBytes(anh);
        var cacGoi = new ArrayList<>(KhungHinhUDP.tach(anh, token, 1));
        Collections.reverse(cacGoi);
        KhungHinhUDP ghep = new KhungHinhUDP();
        assertNull(ghep.nhan(packet(cacGoi.get(0)), token));
        assertNull(ghep.nhan(packet(cacGoi.get(0)), token));
        byte[] daGhep = null;
        for (int i = 1; i < cacGoi.size(); i++) {
            daGhep = ghep.nhan(packet(cacGoi.get(i)), token);
        }
        assertArrayEquals(anh, daGhep);
        assertNull(ghep.nhan(packet(cacGoi.get(0)), token));
    }

    @Test
    void khungMoiThayKhungMatGoiVaTuChoiTokenSai() {
        KhungHinhUDP ghep = new KhungHinhUDP();
        var cu = KhungHinhUDP.tach(new byte[3000], token, 1);
        assertNull(ghep.nhan(packet(cu.get(0)), token));
        byte[] moi = {1, 2, 3};
        byte[] goiMoi = KhungHinhUDP.tach(moi, token, 2).get(0);
        assertNull(ghep.nhan(packet(goiMoi), UUID.randomUUID()));
        assertArrayEquals(moi, ghep.nhan(packet(goiMoi), token));
        assertNull(ghep.nhan(packet(cu.get(1)), token));
    }

    @Test
    void tuChoiHeaderSaiVaXuLyTatCamera() {
        KhungHinhUDP ghep = new KhungHinhUDP();
        byte[] goi = KhungHinhUDP.tach(new byte[12], token, 1).get(0);
        ByteBuffer.wrap(goi).putInt(24, Integer.MAX_VALUE);
        assertNull(ghep.nhan(packet(goi), token));
        byte[] tat = KhungHinhUDP.tach(new byte[0], token, 2).get(0);
        assertArrayEquals(new byte[0], ghep.nhan(packet(tat), token));
        assertThrows(IllegalArgumentException.class,
                () -> KhungHinhUDP.tach(new byte[KhungHinhUDP.gioiHanAnh + 1], token, 3));
    }

    @Test
    void nhanAnhQuaSocketThatKhiCameraChuaBat() throws Exception {
        CountDownLatch daNhan = new CountDownLatch(1);
        byte[] jpeg = {5, 9, 12};
        try (VideoUDP kenh = new VideoUDP(); DatagramSocket doiPhuong = new DatagramSocket(0)) {
            kenh.tatCamera(true);
            kenh.batDau("127.0.0.1", doiPhuong.getLocalPort(), token, anh -> { },
                    anh -> {
                        if (java.util.Arrays.equals(jpeg, anh)) {
                            daNhan.countDown();
                        }
                    }, loi -> fail(loi));
            for (byte[] goi : KhungHinhUDP.tach(jpeg, token, 1)) {
                doiPhuong.send(new DatagramPacket(goi, goi.length, InetAddress.getLoopbackAddress(), kenh.port()));
            }
            assertTrue(daNhan.await(3, TimeUnit.SECONDS));
        }
    }

    private DatagramPacket packet(byte[] goi) {
        return new DatagramPacket(goi, goi.length);
    }
}
