package client.controller;

import client.hatang.KenhTCP;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Các thao tác bạn bè, hồ sơ công khai và chặn người dùng.
 */
public final class BanBeController {
    private final KenhTCP kenh;
    private boolean dangTaiDanhSach;
    private boolean canTaiLaiDanhSach;
    private boolean daDongDanhSach;

    public BanBeController(KenhTCP kenh) {
        this.kenh = kenh;
    }

    public void taiDanhSach(Executor luongGiaoDien, Consumer<Map<String, JsonObject>> khiTaiXong,
                            Consumer<Throwable> khiLoi) {
        if (daDongDanhSach) {
            return;
        }
        if (dangTaiDanhSach) {
            canTaiLaiDanhSach = true;
            return;
        }
        dangTaiDanhSach = true;
        taiTrang("", new LinkedHashMap<>(), luongGiaoDien, khiTaiXong, khiLoi);
    }

    private void taiTrang(String sauId, Map<String, JsonObject> ketQua, Executor luongGiaoDien,
                         Consumer<Map<String, JsonObject>> khiTaiXong, Consumer<Throwable> khiLoi) {
        kenh.yeuCau("layBanBe", DuLieu.tao("sauId", sauId))
                .whenCompleteAsync((duLieu, sai) -> {
                    if (daDongDanhSach) {
                        return;
                    }
                    if (sai == null) {
                        JsonArray trang = duLieu.getAsJsonArray("banBe");
                        for (JsonElement phanTu : trang) {
                            JsonObject nguoi = phanTu.getAsJsonObject();
                            ketQua.put(DuLieu.chu(nguoi, "id"), nguoi);
                        }
                        if (trang.size() == 50) {
                            String idCuoi = DuLieu.chu(trang.get(49).getAsJsonObject(), "id");
                            taiTrang(idCuoi, ketQua, luongGiaoDien, khiTaiXong, khiLoi);
                            return;
                        }
                        khiTaiXong.accept(ketQua);
                    } else {
                        khiLoi.accept(sai);
                    }

                    dangTaiDanhSach = false;
                    if (canTaiLaiDanhSach) {
                        canTaiLaiDanhSach = false;
                        taiDanhSach(luongGiaoDien, khiTaiXong, khiLoi);
                    }
                }, luongGiaoDien);
    }

    public void dongDanhSach() {
        daDongDanhSach = true;
    }

    public CompletableFuture<JsonObject> timTaiKhoan(String tuKhoa) {
        return kenh.yeuCau("timTaiKhoan", DuLieu.tao("tuKhoa", tuKhoa.strip()));
    }

    public CompletableFuture<JsonObject> thayDoiQuanHe(String id, String thaoTac) {
        return kenh.yeuCau("thaoTacBanBe", DuLieu.tao("doiPhuongId", id, "thaoTac", thaoTac));
    }

    public CompletableFuture<JsonObject> hoSoCongKhai(String id) {
        return kenh.yeuCau("hoSoCongKhai", DuLieu.tao("nguoiDungId", id));
    }

    public CompletableFuture<JsonObject> datChan(String id, boolean chan) {
        return kenh.yeuCau(chan ? "chanNguoiDung" : "boChanNguoiDung", DuLieu.tao("nguoiNhanId", id));
    }
}
