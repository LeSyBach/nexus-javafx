package client.hatang;

import com.google.gson.JsonObject;
import giaothuc.*;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Không phụ thuộc JavaFX: socket đọc nền, hàng gửi riêng, đối chiếu ACK bằng mã.
 */
public final class KenhTCP implements AutoCloseable {
    private final Socket socket = new Socket();
    private final Map<String, CompletableFuture<JsonObject>> dangCho = new ConcurrentHashMap<>();
    private final ScheduledExecutorService dongHo =
            Executors.newSingleThreadScheduledExecutor(this::taoLuongNen);
    private final ExecutorService luongGui =
            new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64),
                    this::taoLuongNen);
    private final AtomicBoolean daDong = new AtomicBoolean();
    private volatile Consumer<ThongDiep> khiCoSuKien = thongDiep -> {
    };
    private volatile Consumer<String> khiMatKetNoi = lyDo -> {
    };
    private DataOutputStream outputStream;
    private String host;
    private int port;
    private volatile Consumer<String> khiPhienBiHuy = lyDo -> {
    };

    public String mayChu() {
        return host;
    }

    public int cong() {
        return port;
    }

    public void khiPhienBiHuy(Consumer<String> xuLy) {
        khiPhienBiHuy = xuLy;
    }

    public static final class LoiPhanHoi extends IOException {
        private final String ma;

        public LoiPhanHoi(String ma, String thongBao) {
            super(thongBao);
            this.ma = ma;
        }

        public String ma() {
            return ma;
        }
    }

    private Thread taoLuongNen(Runnable congViec) {
        Thread luong = new Thread(congViec, "nexus-client-nen");
        luong.setDaemon(true);
        return luong;
    }

    public CompletableFuture<KenhTCP> ketNoi(String host, int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.host = host.strip();
                this.port = port;
                InetSocketAddress serverAddress = new InetSocketAddress(this.host, port);

                socket.connect(serverAddress, 5000);
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setSoTimeout(70000);

                outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                taoLuongNen(this::nhanTrongNen).start();
                return this;
            } catch (IOException loi) {
                close();
                throw new CompletionException(new IOException(
                        "Không kết nối được máy chủ. Kiểm tra địa chỉ, cổng và chương trình máy chủ.", loi));
            }
        }, luongGui);
    }

    public void khiCoSuKien(Consumer<ThongDiep> xuLy) {
        khiCoSuKien = xuLy;
    }

    public void khiMatKetNoi(Consumer<String> xuLy) {
        khiMatKetNoi = xuLy;
    }

    public void startHeartbeat() {
        dongHo.scheduleAtFixedRate(() -> yeuCau("nhipTim", new JsonObject()).exceptionally(loi -> {
            matKetNoi();
            return null;
        }), 20, 20, TimeUnit.SECONDS);
    }

    public CompletableFuture<JsonObject> yeuCau(String loai, JsonObject duLieu) {
        if (daDong.get() || outputStream == null) {
            return CompletableFuture.failedFuture(new IOException("Kết nối đã đóng."));
        }
        if (dangCho.size() >= 64) {
            return CompletableFuture.failedFuture(
                    new IOException("Đang có nhiều yêu cầu. Bạn chờ một chút nhé."));
        }

        ThongDiep thongDiep = ThongDiep.yeuCau(loai, duLieu);
        CompletableFuture<JsonObject> ketQua = new CompletableFuture<>();
        dangCho.put(thongDiep.ma(), ketQua);
        try {
            ScheduledFuture<?> hetHan = dongHo.schedule(() -> {
                CompletableFuture<JsonObject> cu = dangCho.remove(thongDiep.ma());
                if (cu != null) {
                    cu.completeExceptionally(new IOException(
                            "Chưa nhận được xác nhận. Có thể gửi lại cùng bản nháp để tránh lưu trùng."));
                }
            }, 10, TimeUnit.SECONDS);
            ketQua.whenComplete((duLieuMoi, loi) -> hetHan.cancel(false));

            luongGui.execute(() -> {
                try {
                    GiaoThuc.ghi(outputStream, thongDiep);
                } catch (IOException loi) {
                    matKetNoi();
                }
            });
        } catch (RejectedExecutionException loi) {
            dangCho.remove(thongDiep.ma());
            ketQua.completeExceptionally(new IOException("Kết nối đang đóng hoặc máy chủ chưa sẵn sàng."));
        }
        return ketQua;
    }

    private void nhanTrongNen() {
        try (DataInputStream inputStream = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()))) {
            while (!daDong.get()) {
                ThongDiep thongDiep = GiaoThuc.doc(inputStream);
                if (thongDiep.loai().equals("phanHoi")) {
                    nhanPhanHoi(thongDiep);
                } else if (thongDiep.loai().equals("phienBiHuy")) {
                    khiPhienBiHuy.accept(DuLieu.chu(thongDiep.duLieu(), "thongBao"));
                } else {
                    khiCoSuKien.accept(thongDiep);
                }
            }
        } catch (IOException | RuntimeException loi) {
            if (!daDong.get()) {
                matKetNoi();
            }
        }
    }

    private void nhanPhanHoi(ThongDiep thongDiep) {
        CompletableFuture<JsonObject> cho = dangCho.remove(thongDiep.ma());
        if (cho == null) {
            return;
        }

        JsonObject duLieu = thongDiep.duLieu();
        if (duLieu.has("thanhCong") && duLieu.get("thanhCong").getAsBoolean()) {
            cho.complete(duLieu);
        } else {
            String ma = DuLieu.chu(duLieu, "maLoi");
            cho.completeExceptionally(new LoiPhanHoi(ma, DuLieu.chu(duLieu, "thongBao")));
            if (ma.equals("phienKhongHopLe")) {
                khiPhienBiHuy.accept(DuLieu.chu(duLieu, "thongBao"));
            }
        }
    }

    private void matKetNoi() {
        if (daDong.get()) {
            return;
        }
        close();
        khiMatKetNoi.accept("Mất kết nối với máy chủ. Bạn đăng nhập lại để đồng bộ lịch sử.");
    }

    @Override
    public void close() {
        if (!daDong.compareAndSet(false, true)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException loi) { /* Đã đóng. */ }
        IOException loi = new IOException("Kết nối đã đóng. Tin đã lưu vẫn có trong lịch sử.");
        dangCho.values().forEach(cho -> cho.completeExceptionally(loi));
        dangCho.clear();
        dongHo.shutdownNow();
        luongGui.shutdownNow();
    }
}
