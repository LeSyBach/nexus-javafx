package client.hatang;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.CountDownLatch;

/** Phát chuông trên luồng riêng để JavaFX vẫn phản hồi khi thiết bị âm thanh mở chậm. */
public final class ChuongGoiDen {
    private static final int TAN_SO_LAY_MAU = 16000;
    private static final AudioFormat DINH_DANG = new AudioFormat(TAN_SO_LAY_MAU, 16, 1, true, false);

    private Thread luongPhat;

    // Hai thao tác này được gọi từ luồng giao diện.
    public void bat() {
        if (luongPhat != null) {
            return;
        }
        luongPhat = new Thread(this::phat, "chuong-goi-den");
        luongPhat.setDaemon(true);
        luongPhat.start();
    }

    public void tat() {
        if (luongPhat != null) {
            luongPhat.interrupt();
            luongPhat = null;
        }
    }

    private void phat() {
        try (Clip clip = AudioSystem.getClip()) {
            byte[] mau = taoMauChuong();
            clip.open(DINH_DANG, mau, 0, mau.length);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            new CountDownLatch(1).await();
        } catch (InterruptedException loi) {
            Thread.currentThread().interrupt();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException loi) {
            // Thanh cuộc gọi vẫn hiển thị nếu hệ thống không phát được chuông.
        }
    }

    private byte[] taoMauChuong() {
        byte[] mau = new byte[TAN_SO_LAY_MAU * 2 * 2];
        ghiTiengBeep(mau, 0);
        ghiTiengBeep(mau, TAN_SO_LAY_MAU * 23 / 100);
        return mau;
    }

    private void ghiTiengBeep(byte[] mau, int batDau) {
        int doDai = TAN_SO_LAY_MAU * 15 / 100;
        int doDaiChuyenAm = TAN_SO_LAY_MAU / 100;
        for (int i = 0; i < doDai; i++) {
            int cachBien = Math.min(i, doDai - 1 - i);
            double mucAm = Math.min(1.0, (double) cachBien / doDaiChuyenAm);
            double pha = 2 * Math.PI * 660 * i / TAN_SO_LAY_MAU;
            short giaTri = (short) (4200 * mucAm * Math.sin(pha));
            int viTri = (batDau + i) * 2;
            mau[viTri] = (byte) giaTri;
            mau[viTri + 1] = (byte) (giaTri >> 8);
        }
    }
}
