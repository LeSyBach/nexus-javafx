package client.controller;

import client.hatang.KenhTCP;
import client.model.TaiKhoanModel;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.util.concurrent.CompletableFuture;

/**
 * Quản lý yêu cầu đăng nhập, phiên và hồ sơ.
 */
public final class TaiKhoanController {
    private final KenhTCP kenh;

    public TaiKhoanController(KenhTCP kenh) {
        this.kenh = kenh;
    }

    public static int docCong(String chu) {
        try {
            int cong = Integer.parseInt(chu);
            if (cong < 1 || cong > 65535) {
                throw new NumberFormatException();
            }
            return cong;
        } catch (NumberFormatException sai) {
            throw new IllegalArgumentException("Cổng kết nối cần từ 1 đến 65535.");
        }
    }

    public CompletableFuture<JsonObject> xacThuc(String diaChi, int cong, boolean dangKy, String email,
                                                 String matKhau, String ten) {
        String loaiYeuCau = dangKy ? "dangKy" : "dangNhap";
        JsonObject duLieu = DuLieu.tao("email", email, "matKhau", matKhau, "tenHienThi", ten);

        return kenh.ketNoi(diaChi.trim(), cong)
                .thenCompose(ketNoi -> ketNoi.yeuCau(loaiYeuCau, duLieu));
    }

    public CompletableFuture<JsonObject> khoiPhuc(TaiKhoanModel.TaiKhoan taiKhoan) {
        return kenh.ketNoi(taiKhoan.mayChu(), taiKhoan.cong())
                .thenCompose(
                        mo -> kenh.yeuCau("khoiPhucPhien", DuLieu.tao("maPhien", taiKhoan.maPhien())));
    }

    public CompletableFuture<JsonObject> dangXuat() {
        return kenh.yeuCau("dangXuat", new JsonObject());
    }

    public CompletableFuture<JsonObject> hoSo() {
        return kenh.yeuCau("layHoSo", new JsonObject());
    }

    public CompletableFuture<JsonObject> luuHoSo(String ten, String dienThoai, String gioiThieu, String anh,
                                                 String phienBan) {
        int doDai = ten.strip().length();
        if (doDai < 2 || doDai > 100) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Tên hiển thị cần 2–100 ký tự."));
        }
        if (gioiThieu.strip().length() > 240) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Giới thiệu tối đa 240 ký tự."));
        }
        return kenh.yeuCau("suaHoSo", DuLieu.tao("tenHienThi", ten, "soDienThoai", dienThoai,
                "gioiThieu", gioiThieu, "anhDaiDien", anh, "phienBan", phienBan));
    }

    public CompletableFuture<JsonObject> luuQuyen(String email, String dienThoai) {
        return kenh.yeuCau("luuQuyenRiengTu",
                DuLieu.tao("quyenEmail", email, "quyenDienThoai", dienThoai));
    }

    public CompletableFuture<JsonObject> doiMatKhau(String cu, String moi, String xacNhan) {
        if (!moi.equals(xacNhan)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Mật khẩu nhập lại chưa trùng khớp."));
        }
        if (moi.length() < 8 || moi.length() > 128) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Mật khẩu mới cần 8–128 ký tự."));
        }
        return kenh.yeuCau("doiMatKhau", DuLieu.tao("matKhauCu", cu, "matKhauMoi", moi));
    }
}
