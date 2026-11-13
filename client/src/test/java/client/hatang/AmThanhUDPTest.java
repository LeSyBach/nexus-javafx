package client.hatang;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AmThanhUDPTest {
    @Test
    void truyenMauPcmTrucTiepQuaSocket() throws Exception {
        InetAddress localhost = InetAddress.getLoopbackAddress();
        try (DatagramSocket a = new DatagramSocket(0, localhost);
             DatagramSocket b = new DatagramSocket(0, localhost)) {
            a.connect(new InetSocketAddress(localhost, b.getLocalPort()));
            b.connect(new InetSocketAddress(localhost, a.getLocalPort()));
            b.setSoTimeout(2000);

            UUID token = UUID.randomUUID();
            byte[] goi = new byte[AmThanhUDP.kichThuocHeader + AmThanhUDP.kichThuocPCM];
            Arrays.fill(goi, AmThanhUDP.kichThuocHeader, goi.length, (byte) 42);
            AmThanhUDP.ghiHeader(ByteBuffer.wrap(goi), token, 7);
            a.send(new DatagramPacket(goi, goi.length));

            var nhan = new DatagramPacket(new byte[goi.length + 1], goi.length + 1);
            b.receive(nhan);
            assertEquals(7, AmThanhUDP.docSoGoi(nhan, token));
            assertArrayEquals(goi, Arrays.copyOf(nhan.getData(), nhan.getLength()));
            assertEquals(-1, AmThanhUDP.docSoGoi(nhan, UUID.randomUUID()));
            nhan.setLength(goi.length - 1);
            assertEquals(-1, AmThanhUDP.docSoGoi(nhan, token));
        }
    }
}
