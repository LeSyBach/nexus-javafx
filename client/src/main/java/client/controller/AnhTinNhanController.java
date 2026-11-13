package client.controller;

import client.hatang.KenhTCP;

import giaothuc.DuLieu;
import client.model.AnhTinNhanModel;
import java.util.concurrent.Executor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Điều phối ảnh xem trước của một tài khoản. Hàng đợi và cache chạy trên luồng giao diện.
 */
public final class AnhTinNhanController {
    private final KenhTCP kenh;
    private final AnhTinNhanModel boNho = new AnhTinNhanModel();
    private final Executor luongGiaoDien;
    private final Map<String, CompletableFuture<byte[]>> dangCho = new HashMap<>();
    private final Deque<String> hangDoi = new ArrayDeque<>();
    private int soDangTai;
    private boolean daDong;

    public AnhTinNhanController(KenhTCP kenh, Executor luongGiaoDien) {
        this.kenh = kenh;
        this.luongGiaoDien = luongGiaoDien;
    }

    // Gọi trên luồng JavaFX; việc nhận dữ liệu vẫn chạy trên luồng TCP.
    public CompletableFuture<byte[]> tai(String id) {
        if (daDong) {
            return CompletableFuture.failedFuture(new IOException("Đã đóng cuộc trò chuyện."));
        }
        byte[] anh = boNho.layAnh(id);
        if (anh != null) {
            return CompletableFuture.completedFuture(anh);
        }
        if (dangCho.containsKey(id)) {
            return dangCho.get(id);
        }
        CompletableFuture<byte[]> ketQua = new CompletableFuture<>();
        dangCho.put(id, ketQua);
        hangDoi.addLast(id);
        taiTiep();
        return ketQua;
    }

    private void taiTiep() {
        while (!daDong && soDangTai < 3 && !hangDoi.isEmpty()) {
            String id = hangDoi.removeFirst();
            CompletableFuture<byte[]> ketQua = dangCho.get(id);
            if (ketQua == null) {
                continue;
            }
            soDangTai++;
            kenh.yeuCau("tepAnhThu", DuLieu.tao("tinNhanId", id))
                    .whenCompleteAsync((tra, loi) -> {
                        soDangTai--;
                        // Thu hồi hoặc đóng tài khoản đã hủy lượt tải: không đưa ảnh trở lại bộ nhớ.
                        if (dangCho.remove(id, ketQua) && !daDong) {
                            try {
                                if (loi != null) {
                                    throw new java.util.concurrent.CompletionException(loi);
                                }
                                byte[] anh = Base64.getDecoder().decode(DuLieu.chu(tra, "anh"));
                                if (anh.length == 0 || anh.length > 24576) {
                                    throw new IOException("Ảnh xem trước không hợp lệ.");
                                }
                                boNho.luuAnh(id, anh);
                                ketQua.complete(anh);
                            } catch (Exception sai) {
                                ketQua.completeExceptionally(sai);
                            }
                        }
                        taiTiep();
                    }, luongGiaoDien);
        }
    }

    public void xoa(String id) {
        boNho.xoaAnh(id);
        hangDoi.remove(id);
        CompletableFuture<byte[]> cho = dangCho.remove(id);
        if (cho != null) {
            cho.completeExceptionally(new IOException("Tin nhắn đã được thu hồi."));
        }
    }

    public void dong() {
        daDong = true;
        boNho.xoaTatCa();
        hangDoi.clear();
        var cacCho = new ArrayList<>(dangCho.values());
        dangCho.clear();
        cacCho.forEach(cho -> cho.completeExceptionally(new IOException("Đã đóng cuộc trò chuyện.")));
    }
}
