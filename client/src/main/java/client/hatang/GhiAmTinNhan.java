package client.hatang;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Thu một tin thoại vào tệp WAV trên luồng nền. */
public final class GhiAmTinNhan implements AutoCloseable {
    public static final int GIOI_HAN_GIAY = 60;
    private static final int TAN_SO = 16000;
    private static final int BYTE_MOI_MAU = 2;
    private static final int BYTE_MOI_GIAY = TAN_SO * BYTE_MOI_MAU;
    private static final AudioFormat DINH_DANG = new AudioFormat(TAN_SO, 16, 1, true, false);

    public record BanGhi(Path tep, long miliGiay) {
    }

    private final CompletableFuture<Void> sanSang = new CompletableFuture<>();
    private final CompletableFuture<BanGhi> ketQua = new CompletableFuture<>();
    private volatile boolean daDung;

    private GhiAmTinNhan() {
    }

    public static GhiAmTinNhan batDau() {
        GhiAmTinNhan ghiAm = new GhiAmTinNhan();
        Thread luong = new Thread(ghiAm::thu, "nexus-ghi-am-tin-nhan");
        luong.setDaemon(true);
        luong.start();
        return ghiAm;
    }

    public CompletableFuture<Void> khiSanSang() {
        return sanSang;
    }

    /** Bên nhận kết quả sở hữu và có trách nhiệm dọn tệp tạm. */
    public CompletableFuture<BanGhi> ketQua() {
        return ketQua;
    }

    public void dung() {
        daDung = true;
    }

    @Override
    public void close() {
        daDung = true;
        sanSang.cancel(false);
        ketQua.cancel(false);
    }

    private void thu() {
        Path tep = null;
        try {
            if (ketQua.isCancelled()) {
                return;
            }
            byte[] pcm = docMic();
            if (ketQua.isCancelled()) {
                return;
            }
            if (pcm.length < BYTE_MOI_GIAY / 5) {
                throw new IOException("Đoạn ghi âm quá ngắn. Hãy thử ghi lại.");
            }
            tep = Files.createTempFile("nexus-ghi-am-", ".wav");
            try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(pcm),
                    DINH_DANG, pcm.length / BYTE_MOI_MAU)) {
                AudioSystem.write(audio, AudioFileFormat.Type.WAVE, tep.toFile());
            }
            if (ketQua.complete(new BanGhi(tep, pcm.length * 1000L / BYTE_MOI_GIAY))) {
                tep = null;
            }
        } catch (IOException | RuntimeException loi) {
            sanSang.completeExceptionally(loi);
            ketQua.completeExceptionally(loi);
        } finally {
            if (tep != null) {
                try {
                    Files.deleteIfExists(tep);
                } catch (IOException loi) {
                    tep.toFile().deleteOnExit();
                }
            }
        }
    }

    private byte[] docMic() throws IOException {
        try (TargetDataLine mic = AudioSystem.getTargetDataLine(DINH_DANG)) {
            mic.open(DINH_DANG, BYTE_MOI_GIAY / 5);
            if (daDung) {
                return new byte[0];
            }
            mic.start();
            sanSang.complete(null);

            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            int gioiHanByte = GIOI_HAN_GIAY * BYTE_MOI_GIAY;
            long han = System.nanoTime() + TimeUnit.SECONDS.toNanos(GIOI_HAN_GIAY);
            byte[] khoi = new byte[BYTE_MOI_GIAY / 50];
            while (!daDung && pcm.size() < gioiHanByte && System.nanoTime() < han) {
                // Chỉ đọc phần đã sẵn sàng để nút dừng không phải chờ driver micro.
                int doc = Math.min(Math.min(mic.available(), khoi.length), gioiHanByte - pcm.size());
                doc -= doc % BYTE_MOI_MAU;
                if (doc > 0) {
                    int soByte = mic.read(khoi, 0, doc);
                    if (soByte > 0) {
                        pcm.write(khoi, 0, soByte);
                        continue;
                    }
                }
                Thread.sleep(10);
            }
            return pcm.toByteArray();
        } catch (LineUnavailableException | IllegalArgumentException | IllegalStateException | SecurityException loi) {
            throw new IOException("Chưa mở được micro. Kiểm tra quyền truy cập hoặc đóng ứng dụng đang dùng micro.", loi);
        } catch (InterruptedException loi) {
            Thread.currentThread().interrupt();
            throw new IOException("Ghi âm bị gián đoạn.", loi);
        }
    }
}
