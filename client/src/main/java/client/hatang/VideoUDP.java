package client.hatang;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Thu camera, nén JPEG 320×240 và truyền từng khung qua UDP trong mạng LAN. */
public final class VideoUDP implements AutoCloseable {
    // Nhường thiết bị sau khi đóng camera của cuộc gọi cũ.
    private static final Semaphore khoaCamera = new Semaphore(1);
    private final DatagramSocket socket = new DatagramSocket(0);
    private final AtomicBoolean daDong = new AtomicBoolean();
    private final AtomicBoolean daBatDau = new AtomicBoolean();
    private volatile boolean tatCamera;
    private boolean giuCamera;

    public VideoUDP() throws IOException {
    }

    public int port() {
        return socket.getLocalPort();
    }

    public void tatCamera(boolean tat) {
        tatCamera = tat;
    }

    public void batDau(String host, int port, UUID token, Consumer<byte[]> anhCuaMinh,
                       Consumer<byte[]> anhDoiPhuong, Consumer<String> khiLoi) throws IOException {
        if (!daBatDau.compareAndSet(false, true)) {
            return;
        }
        socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));
        socket.setSoTimeout(1000);
        socket.setReceiveBufferSize(262144);
        Thread nhan = new Thread(() -> nhan(token, anhDoiPhuong, khiLoi), "nexus-nhan-video");
        nhan.setDaemon(true);
        nhan.start();
        Thread gui = new Thread(() -> gui(token, anhCuaMinh, khiLoi), "nexus-gui-video");
        gui.setDaemon(true);
        gui.start();
    }

    private void nhan(UUID token, Consumer<byte[]> khiCoAnh, Consumer<String> khiLoi) {
        KhungHinhUDP ghep = new KhungHinhUDP();
        byte[] boDem = new byte[KhungHinhUDP.kichThuocHeader + KhungHinhUDP.kichThuocManh + 1];
        DatagramPacket packet = new DatagramPacket(boDem, boDem.length);
        long lanCoAnh = System.nanoTime();
        boolean daBaoTat = false;
        try {
            while (!daDong.get()) {
                try {
                    packet.setLength(boDem.length);
                    socket.receive(packet);
                    byte[] anh = ghep.nhan(packet, token);
                    if (anh != null) {
                        lanCoAnh = System.nanoTime();
                        daBaoTat = anh.length == 0;
                        khiCoAnh.accept(anh);
                    }
                } catch (SocketTimeoutException choTiep) {
                    // Hiện lại đại diện nếu hình ngừng đến, âm thanh vẫn chạy độc lập.
                }
                if (!daBaoTat && System.nanoTime() - lanCoAnh > TimeUnit.SECONDS.toNanos(3)) {
                    khiCoAnh.accept(new byte[0]);
                    daBaoTat = true;
                }
            }
        } catch (IOException loi) {
            if (!daDong.get()) {
                khiLoi.accept("Đường nhận video bị gián đoạn.");
            }
        }
    }

    private void gui(UUID token, Consumer<byte[]> khiCoAnh, Consumer<String> khiLoi) {
        Webcam camera = null;
        long maKhung = 0;
        long lanCoAnh = System.nanoTime();
        try {
            while (!daDong.get()) {
                long batDau = System.nanoTime();
                byte[] anh = new byte[0];
                if (tatCamera) {
                    if (camera != null) {
                        dongCamera(camera);
                        camera = null;
                    }
                } else {
                    try {
                        if (camera == null) {
                            camera = moCamera();
                            lanCoAnh = System.nanoTime();
                        }
                        if (daDong.get() || tatCamera) {
                            continue;
                        }
                        BufferedImage hinh = camera.getImage();
                        if (hinh != null) {
                            anh = nenAnh(hinh);
                            lanCoAnh = System.nanoTime();
                        } else if (System.nanoTime() - lanCoAnh > TimeUnit.SECONDS.toNanos(8)) {
                            throw new IllegalStateException("Camera không trả hình.");
                        }
                    } catch (IOException | RuntimeException | LinkageError | TimeoutException loi) {
                        tatCamera = true;
                        khiLoi.accept("Chưa mở được camera. Kiểm tra quyền camera hoặc ứng dụng đang sử dụng thiết bị.");
                    }
                }
                if (daDong.get()) {
                    break;
                }
                if (tatCamera) {
                    anh = new byte[0];
                }
                for (byte[] goi : KhungHinhUDP.tach(anh, token, maKhung++)) {
                    socket.send(new DatagramPacket(goi, goi.length));
                }
                khiCoAnh.accept(anh);
                long chuKy = tatCamera ? 500 : 80;
                long conLai = TimeUnit.MILLISECONDS.toNanos(chuKy) - (System.nanoTime() - batDau);
                if (conLai > 0) {
                    TimeUnit.NANOSECONDS.sleep(conLai);
                }
            }
        } catch (IOException | RuntimeException loi) {
            if (!daDong.get()) {
                khiLoi.accept("Đường gửi video bị gián đoạn.");
            }
        } catch (InterruptedException loi) {
            Thread.currentThread().interrupt();
        } finally {
            if (camera != null) {
                dongCamera(camera);
            }
        }
    }

    private Webcam moCamera() throws TimeoutException {
        if (!khoaCamera.tryAcquire()) {
            throw new IllegalStateException("Camera đang được cuộc gọi khác sử dụng.");
        }
        giuCamera = true;
        Webcam camera = null;
        try {
            camera = Webcam.getDefault(8, TimeUnit.SECONDS);
            if (camera == null) {
                throw new IllegalStateException("Không tìm thấy camera.");
            }
            Dimension kichThuoc = Arrays.stream(camera.getViewSizes())
                    .min(Comparator.comparingLong(co -> Math.abs((long) co.width * co.height - 320 * 240)))
                    .orElseThrow(() -> new IllegalStateException("Camera không có kích thước phù hợp."));
            camera.setViewSize(kichThuoc);
            if (!camera.open(true)) {
                throw new IllegalStateException("Camera đang bận.");
            }
            return camera;
        } catch (RuntimeException | LinkageError | TimeoutException loi) {
            dongCamera(camera);
            throw loi;
        }
    }

    private void dongCamera(Webcam camera) {
        if (!giuCamera) {
            return;
        }
        try {
            if (camera != null) {
                camera.close();
            }
        } finally {
            if (giuCamera) {
                giuCamera = false;
                khoaCamera.release();
            }
        }
    }

    private byte[] nenAnh(BufferedImage hinh) throws IOException {
        BufferedImage nho = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D ve = nho.createGraphics();
        try {
            ve.drawImage(hinh, 0, 0, 320, 240, null);
        } finally {
            ve.dispose();
        }
        ByteArrayOutputStream noiDung = new ByteArrayOutputStream();
        ImageIO.write(nho, "jpg", noiDung);
        return noiDung.toByteArray();
    }

    @Override
    public void close() {
        if (daDong.compareAndSet(false, true)) {
            socket.close();
        }
    }
}
