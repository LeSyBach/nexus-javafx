package client.controller;

import client.hatang.KenhTCP;
import com.google.gson.JsonObject;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/** Quản lý kết nối tạm khi xác thực. Đăng nhập thành công bàn giao kết nối cho ứng dụng. */
public final class DangNhapController {
    private final Executor luongGiaoDien;
    private KenhTCP dangMo;
    private boolean daDong;

    public DangNhapController(Executor luongGiaoDien) {
        this.luongGiaoDien = luongGiaoDien;
    }

    public record KetQuaXacThuc(KenhTCP kenh, JsonObject phanHoi) {
    }

    public CompletableFuture<KetQuaXacThuc> xacThuc(String diaChi, String cong, boolean dangKy,
                                                   String email, String matKhau, String xacNhan, String ten) {
        if (daDong) {
            return CompletableFuture.failedFuture(new CancellationException());
        }
        if (dangMo != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Đang kết nối…"));
        }
        if (dangKy && !matKhau.equals(xacNhan)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Mật khẩu nhập lại chưa trùng khớp."));
        }

        int port;
        try {
            port = TaiKhoanController.docCong(cong);
        } catch (IllegalArgumentException loi) {
            return CompletableFuture.failedFuture(loi);
        }

        KenhTCP kenh = new KenhTCP();
        dangMo = kenh;
        TaiKhoanController taiKhoan = new TaiKhoanController(kenh);

        return taiKhoan.xacThuc(diaChi, port, dangKy, email, matKhau, ten)
                .handleAsync((phanHoi, loi) -> {
                    dangMo = null;
                    if (daDong || loi != null || dangKy) {
                        kenh.close();
                    }
                    if (daDong) {
                        throw new CancellationException();
                    }
                    if (loi != null) {
                        throw new CompletionException(loi);
                    }
                    return new KetQuaXacThuc(dangKy ? null : kenh, phanHoi);
                }, luongGiaoDien);
    }

    public void dong() {
        daDong = true;
        if (dangMo != null) {
            dangMo.close();
            dangMo = null;
        }
    }
}
