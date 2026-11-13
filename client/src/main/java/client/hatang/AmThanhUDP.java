package client.hatang;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** PCM 16 kHz, 16 bit, mono. Mỗi datagram mang 20 ms và số thứ tự. */
public final class AmThanhUDP implements AutoCloseable {
    private static final AudioFormat dinhDang = new AudioFormat(16000, 16, 1, true, false);
    static final int kichThuocPCM = 640;
    static final int kichThuocHeader = Long.BYTES * 2 + Integer.BYTES;
    private static final long hanNhan = TimeUnit.SECONDS.toNanos(8);
    private final DatagramSocket socket;
    private final AtomicBoolean daDong = new AtomicBoolean();
    private volatile boolean tatMic;
    private TargetDataLine mic;
    private SourceDataLine loa;

    public AmThanhUDP() throws SocketException {
        socket = new DatagramSocket(0);
    }

    public int port() {
        return socket.getLocalPort();
    }

    public void tatMic(boolean tat) {
        tatMic = tat;
    }

    public void batDau(String host, int port, UUID mediaToken, Runnable khiCoAmThanh,
                       Consumer<String> khiLoi) {
        Thread luongNhan = new Thread(() -> nhan(host, port, mediaToken, khiCoAmThanh, khiLoi),
                "nexus-nhan-am-thanh");
        luongNhan.setDaemon(true);
        luongNhan.start();
    }

    private void nhan(String host, int port, UUID token, Runnable khiCoAmThanh, Consumer<String> khiLoi) {
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
            socket.setSoTimeout(1000);
            socket.setReceiveBufferSize(kichThuocPCM * 16);
            if (!moThietBi()) {
                return;
            }
            Thread luongGui = new Thread(() -> gui(token, khiLoi), "nexus-gui-am-thanh");
            luongGui.setDaemon(true);
            luongGui.start();

            // Thêm một byte để phát hiện gói dài hơn kích thước cho phép.
            byte[] duLieu = new byte[kichThuocHeader + kichThuocPCM + 1];
            DatagramPacket packet = new DatagramPacket(duLieu, duLieu.length);
            long lanCuoi = System.nanoTime();
            int soGoiCuoi = -1;
            while (!daDong.get()) {
                try {
                    packet.setLength(duLieu.length);
                    socket.receive(packet);
                    int soGoi = docSoGoi(packet, token);
                    if (soGoi > soGoiCuoi) {
                        if (soGoiCuoi < 0) {
                            khiCoAmThanh.run();
                        }
                        soGoiCuoi = soGoi;
                        lanCuoi = System.nanoTime();
                        loa.write(duLieu, kichThuocHeader, kichThuocPCM);
                    }
                } catch (SocketTimeoutException choTiep) {
                    // Kiểm tra thời gian từ gói âm thanh hợp lệ cuối cùng.
                }
                if (System.nanoTime() - lanCuoi > hanNhan) {
                    baoLoi("Không nhận được âm thanh UDP. Kiểm tra mạng LAN và tường lửa.", khiLoi);
                    return;
                }
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException loi) {
            baoLoi("Không mở được mic hoặc loa. Kiểm tra thiết bị âm thanh và quyền dùng mic.", khiLoi);
        } catch (IOException | IllegalStateException loi) {
            baoLoi("Kết nối âm thanh UDP bị gián đoạn. Kiểm tra mạng LAN và tường lửa.", khiLoi);
        }
    }

    private synchronized boolean moThietBi() throws LineUnavailableException {
        if (daDong.get()) {
            return false;
        }
        mic = AudioSystem.getTargetDataLine(dinhDang);
        mic.open(dinhDang, kichThuocPCM * 8);
        loa = AudioSystem.getSourceDataLine(dinhDang);
        loa.open(dinhDang, kichThuocPCM * 8);
        loa.start();
        mic.start();
        return true;
    }

    private void gui(UUID token, Consumer<String> khiLoi) {
        byte[] duLieu = new byte[kichThuocHeader + kichThuocPCM];
        ByteBuffer header = ByteBuffer.wrap(duLieu);
        DatagramPacket packet = new DatagramPacket(duLieu, duLieu.length);
        int soGoi = 0;
        try {
            while (!daDong.get()) {
                int soByte = mic.read(duLieu, kichThuocHeader, kichThuocPCM);
                if (soByte != kichThuocPCM || daDong.get()) {
                    break;
                }
                if (tatMic) {
                    Arrays.fill(duLieu, kichThuocHeader, duLieu.length, (byte) 0);
                }
                ghiHeader(header, token, soGoi++);
                socket.send(packet);
            }
            baoLoi("Mic đã ngừng thu âm.", khiLoi);
        } catch (IOException | RuntimeException loi) {
            baoLoi("Đường gửi âm thanh bị gián đoạn.", khiLoi);
        }
    }

    static void ghiHeader(ByteBuffer header, UUID token, int soGoi) {
        header.putLong(0, token.getMostSignificantBits());
        header.putLong(Long.BYTES, token.getLeastSignificantBits());
        header.putInt(Long.BYTES * 2, soGoi);
    }

    static int docSoGoi(DatagramPacket packet, UUID token) {
        if (packet.getLength() != kichThuocHeader + kichThuocPCM) {
            return -1;
        }
        ByteBuffer header = ByteBuffer.wrap(packet.getData(), packet.getOffset(), kichThuocHeader);
        if (header.getLong() != token.getMostSignificantBits()
                || header.getLong() != token.getLeastSignificantBits()) {
            return -1;
        }
        return header.getInt();
    }

    private void baoLoi(String thongBao, Consumer<String> khiLoi) {
        if (dongTaiNguyen()) {
            khiLoi.accept(thongBao);
        }
    }

    private synchronized boolean dongTaiNguyen() {
        if (!daDong.compareAndSet(false, true)) {
            return false;
        }
        socket.close();
        if (mic != null) {
            mic.close();
        }
        if (loa != null) {
            loa.close();
        }
        return true;
    }

    @Override
    public void close() {
        dongTaiNguyen();
    }
}
