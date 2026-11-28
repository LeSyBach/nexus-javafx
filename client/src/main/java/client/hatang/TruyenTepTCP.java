package client.hatang;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

/**
 * Truyền theo khối 24 KB qua cùng socket; mọi đọc/ghi tệp chạy ngoài luồng JavaFX.
 */
public final class TruyenTepTCP implements AutoCloseable {
    public static final long gioiHan = 10L * 1024 * 1024;
    private static final int KHOI = 24576;
    private final KenhTCP kenh;
    private final ExecutorService luong = Executors.newSingleThreadExecutor(viec -> {
        Thread t = new Thread(viec, "nexus-truyen-tep");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean dangTruyen = new AtomicBoolean(), daHuy = new AtomicBoolean();

    public TruyenTepTCP(KenhTCP kenh) {
        this.kenh = kenh;
    }

    public void huy() {
        daHuy.set(true);
    }

    public boolean dangTruyen() {
        return dangTruyen.get();
    }

    private void kiemTraHuy() throws IOException {
        if (daHuy.get() || Thread.currentThread().isInterrupted()) {
            throw new IOException("Đã hủy truyền tệp.");
        }
    }

    private JsonObject guiLenh(String loai, JsonObject duLieu) throws IOException {
        try {
            return kenh.yeuCau(loai, duLieu).get(15, TimeUnit.SECONDS);
        } catch (InterruptedException loi) {
            Thread.currentThread().interrupt();
            throw new IOException("Đã dừng truyền tệp.");
        } catch (ExecutionException loi) {
            Throwable goc = loi.getCause();
            if (goc instanceof IOException io) {
                throw io;
            }
            throw new IOException("Chưa truyền được tệp.", goc);
        } catch (TimeoutException loi) {
            throw new IOException("Máy chủ chưa xác nhận khối tệp.");
        }
    }

    private <T> CompletableFuture<T> thucHien(Callable<T> viec) {
        if (!dangTruyen.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IOException("Đang truyền một tệp khác. Bạn chờ hoặc hủy tệp đó trước."));
        }
        daHuy.set(false);
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return viec.call();
                } catch (Exception loi) {
                    throw new CompletionException(loi);
                } finally {
                    dangTruyen.set(false);
                }
            }, luong);
        } catch (RejectedExecutionException loi) {
            dangTruyen.set(false);
            return CompletableFuture.failedFuture(new IOException("Kết nối truyền tệp đã đóng."));
        }
    }

    private static MessageDigest boBam() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    public CompletableFuture<JsonObject> gui(Path tep, String nguoiNhan, String noiDung, String maGui,
                                             DoubleConsumer tienTrinh) {
        return gui(tep, nguoiNhan, noiDung, maGui, null, tienTrinh);
    }

    public CompletableFuture<JsonObject> gui(Path tep, String nguoiNhan, String noiDung, String maGui,
                                             String traLoi, DoubleConsumer tienTrinh) {
        return gui(tep, nguoiNhan, noiDung, maGui, traLoi, false, tienTrinh);
    }

    public CompletableFuture<JsonObject> gui(Path tep, String nguoiNhan, String noiDung, String maGui,
                                             String traLoi, boolean tinThoai, DoubleConsumer tienTrinh) {
        return thucHien(() -> {
            boolean daBatDau = false;
            try {
                long kichThuoc = Files.size(tep);
                if (!Files.isRegularFile(tep) || kichThuoc > gioiHan) {
                    throw new IOException("Chọn tệp thường tối đa 10 MB.");
                }
                tienTrinh.accept(-1);
                MessageDigest bam = boBam();
                byte[] khoi = new byte[KHOI];
                try (InputStream doc = Files.newInputStream(tep)) {
                    int so;
                    while ((so = doc.read(khoi)) != -1) {
                        kiemTraHuy();
                        bam.update(khoi, 0, so);
                    }
                }
                kiemTraHuy();
                String sha = HexFormat.of().formatHex(bam.digest());
                String ten = tinThoai ? "Tin-nhan-thoai.wav" : tep.getFileName().toString();
                JsonObject khai = DuLieu.tao("nguoiNhanId", nguoiNhan, "tenTep", ten,
                        "kichThuoc", Long.toString(kichThuoc), "sha256", sha, "noiDung", noiDung,
                        "maGui", maGui, "traLoiId", traLoi);
                if (tinThoai) {
                    khai.addProperty("tinThoai", true);
                }
                JsonObject tra = guiLenh("tepBatDau", khai);
                if (tra.get("daCo").getAsBoolean()) {
                    tienTrinh.accept(1);
                    return tra.getAsJsonObject("tinNhan");
                }
                daBatDau = true;
                long viTri = 0;
                tienTrinh.accept(0);
                try (InputStream doc = Files.newInputStream(tep)) {
                    int so;
                    while ((so = doc.read(khoi)) != -1) {
                        kiemTraHuy();
                        JsonObject daNhan = guiLenh("tepGuiKhoi",
                                DuLieu.tao("viTri", Long.toString(viTri), "duLieu",
                                        Base64.getEncoder().encodeToString(Arrays.copyOf(khoi, so))));
                        viTri += so;
                        if (!DuLieu.chu(daNhan, "daNhan").equals(Long.toString(viTri))) {
                            throw new IOException("Máy chủ xác nhận sai vị trí khối.");
                        }
                        tienTrinh.accept(kichThuoc == 0 ? 1 : (double) viTri / kichThuoc);
                    }
                }
                kiemTraHuy();
                if (viTri != kichThuoc) {
                    throw new IOException("Tệp đã thay đổi trong lúc gửi.");
                }
                // Sau khi gửi lệnh hoàn tất, chờ kết quả; không báo hủy nhầm một tệp đã lưu.
                JsonObject ketQua = guiLenh("tepKetThuc", new JsonObject());
                daBatDau = false;
                tienTrinh.accept(1);
                return ketQua.getAsJsonObject("tinNhan");
            } finally {
                if (daBatDau) {
                    try {
                        guiLenh("tepHuy", new JsonObject());
                    } catch (IOException loi) {/* Đóng socket cũng sẽ dọn tệp dở. */}
                }
            }
        });
    }

    public CompletableFuture<Path> tai(String tinId, Path dich, DoubleConsumer tienTrinh) {
        return thucHien(() -> taiVao(tinId, dich, tienTrinh));
    }

    public CompletableFuture<Path> taiTam(String tinId) {
        return thucHien(() -> {
            Path dich = Files.createTempFile("nexus-nghe-thoai-", ".wav");
            try {
                return taiVao(tinId, dich, x -> { });
            } catch (Exception loi) {
                Files.deleteIfExists(dich);
                throw loi;
            }
        });
    }

    private Path taiVao(String tinId, Path dich, DoubleConsumer tienTrinh) throws IOException, NoSuchAlgorithmException {
        Path tam = null;
        try {
            kiemTraHuy();
            JsonObject thongTin = guiLenh("tepThongTin", DuLieu.tao("tinNhanId", tinId));
            long kichThuoc = Long.parseLong(DuLieu.chu(thongTin, "kichThuoc"));
            if (kichThuoc < 0 || kichThuoc > gioiHan) {
                throw new IOException("Dung lượng tệp không hợp lệ.");
            }
            String sha = DuLieu.chu(thongTin, "sha256");
            if (!sha.matches("[a-f0-9]{64}")) {
                throw new IOException("Mã kiểm tra tệp không hợp lệ.");
            }
            Path thuMuc = dich.toAbsolutePath().getParent();
            tam = Files.createTempFile(thuMuc, "nexus-", ".part");
            MessageDigest bam = boBam();
            long viTri = 0;
            tienTrinh.accept(0);
            try (OutputStream ghi = new BufferedOutputStream(Files.newOutputStream(tam))) {
                while (viTri < kichThuoc) {
                    kiemTraHuy();
                    JsonObject tra = guiLenh("tepDocKhoi",
                            DuLieu.tao("tinNhanId", tinId, "viTri", Long.toString(viTri)));
                    if (!DuLieu.chu(tra, "viTri").equals(Long.toString(viTri))) {
                        throw new IOException("Khối tải về sai thứ tự.");
                    }
                    String chu = DuLieu.chu(tra, "duLieu");
                    if (chu.length() > 32768) {
                        throw new IOException("Khối tải về vượt giới hạn.");
                    }
                    byte[] khoi = Base64.getDecoder().decode(chu);
                    if (khoi.length < 1 || khoi.length > KHOI || viTri + khoi.length > kichThuoc) {
                        throw new IOException("Khối tải về không đầy đủ.");
                    }
                    ghi.write(khoi);
                    bam.update(khoi);
                    viTri += khoi.length;
                    tienTrinh.accept((double) viTri / kichThuoc);
                }
            }
            kiemTraHuy();
            if (!HexFormat.of().formatHex(bam.digest()).equals(sha)) {
                throw new IOException("Tệp tải về không khớp SHA-256. Chưa ghi đè tệp đích.");
            }
            try {
                Files.move(tam, dich.toAbsolutePath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException loi) {
                Files.move(tam, dich.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
            }
            tam = null;
            tienTrinh.accept(1);
            return dich;
        } finally {
            if (tam != null) {
                Files.deleteIfExists(tam);
            }
        }
    }

    public static String tenLuuAnToan(String ten) {
        String chu = ten.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip().replaceAll("[. ]+$", "");
        if (chu.isBlank() || chu.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) {
            chu = "tep-" + chu;
        }
        return chu;
    }

    public static String dungLuong(long soByte) {
        return soByte >= 1024 * 1024 ? String.format(Locale.ROOT, "%.1f MB", soByte / (1024.0 * 1024)) :
                soByte >= 1024 ? String.format(Locale.ROOT, "%.1f KB", soByte / 1024.0) : soByte + " B";
    }

    @Override
    public void close() {
        huy();
        luong.shutdown();
    }
}
