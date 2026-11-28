package client.hatang;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Phát tuần tự từng đoạn thoại và giải phóng thiết bị khi dừng. */
public final class PhatTinNhanThoai implements AutoCloseable {
    private static final long GIOI_HAN_TEP = 2 * 1024 * 1024L;
    private final ExecutorService luongPhat = Executors.newSingleThreadExecutor(viec -> {
        Thread luong = new Thread(viec, "nexus-phat-tin-nhan-thoai");
        luong.setDaemon(true);
        return luong;
    });

    private static final class LuotPhat {
        final CompletableFuture<Void> ketQua = new CompletableFuture<>();
        volatile boolean daDung;
    }

    private LuotPhat dangPhat;

    /** Kết quả chỉ hoàn tất sau khi tệp và thiết bị phát đã đóng. */
    public synchronized CompletableFuture<Void> phat(Path tep) {
        if (luongPhat.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Trình phát đã đóng."));
        }
        dung();
        LuotPhat moi = new LuotPhat();
        dangPhat = moi;
        luongPhat.execute(() -> phatTrongNen(tep, moi));
        return moi.ketQua;
    }

    public synchronized void dung() {
        if (dangPhat != null) {
            dangPhat.daDung = true;
            dangPhat = null;
        }
    }

    @Override
    public synchronized void close() {
        dung();
        luongPhat.shutdown();
    }

    private void phatTrongNen(Path tep, LuotPhat luot) {
        Exception sai = null;
        try {
            if (!luot.daDung) {
                phatWav(tep, luot);
            }
        } catch (InterruptedException loi) {
            Thread.currentThread().interrupt();
            luot.daDung = true;
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | RuntimeException loi) {
            sai = loi;
        }
        synchronized (this) {
            if (dangPhat == luot) {
                dangPhat = null;
            }
        }
        if (luot.daDung) {
            luot.ketQua.cancel(false);
        } else if (sai != null) {
            luot.ketQua.completeExceptionally(new IOException(
                    "Chưa phát được tin nhắn thoại. Kiểm tra tệp hoặc thiết bị âm thanh.", sai));
        } else {
            luot.ketQua.complete(null);
        }
    }

    private void phatWav(Path tep, LuotPhat luot)
            throws IOException, LineUnavailableException, UnsupportedAudioFileException, InterruptedException {
        if (Files.size(tep) > GIOI_HAN_TEP
                || !AudioFileFormat.Type.WAVE.equals(AudioSystem.getAudioFileFormat(tep.toFile()).getType())) {
            throw new IOException("Tệp tin thoại không hợp lệ.");
        }
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(tep.toFile())) {
            AudioFormat dinhDang = audio.getFormat();
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(dinhDang.getEncoding())
                    || dinhDang.getSampleRate() != 16000 || dinhDang.getSampleSizeInBits() != 16
                    || dinhDang.getChannels() != 1 || dinhDang.getFrameSize() != 2 || dinhDang.isBigEndian()
                    || audio.getFrameLength() < 1 || audio.getFrameLength() > 16000L * GhiAmTinNhan.GIOI_HAN_GIAY) {
                throw new IOException("Định dạng WAV không được hỗ trợ.");
            }
            try (Clip clip = AudioSystem.getClip()) {
                clip.open(audio);
                if (luot.daDung) {
                    return;
                }
                CountDownLatch daDung = new CountDownLatch(1);
                clip.addLineListener(suKien -> {
                    if (suKien.getType() == LineEvent.Type.STOP) {
                        daDung.countDown();
                    }
                });
                long han = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(clip.getMicrosecondLength())
                        + TimeUnit.SECONDS.toNanos(2);
                clip.start();
                while (!luot.daDung && !daDung.await(25, TimeUnit.MILLISECONDS)) {
                    if (clip.getFramePosition() >= clip.getFrameLength()) {
                        break;
                    }
                    if (System.nanoTime() > han) {
                        throw new IOException("Thiết bị phát không hoàn tất đoạn thoại.");
                    }
                }
            }
        }
    }
}
