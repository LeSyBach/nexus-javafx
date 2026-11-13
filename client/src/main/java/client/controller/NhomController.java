package client.controller;

import client.hatang.KenhTCP;

import com.google.gson.*;
import giaothuc.DuLieu;

import java.util.concurrent.CompletableFuture;

import java.util.*;

/**
 * Luồng tạo nhóm và gửi tin nhóm. Mỗi biểu mẫu giữ mã gửi khi thử lại.
 */
public final class NhomController {
    public static final int gioiHanBan = 29;
    private final KenhTCP kenh;
    private LanTao lanTao;
    private String maTao;
    private String tinDangThuLai, maTin;

    private record LanTao(String ten, List<String> thanhVien, String anh) {
    }

    public NhomController(KenhTCP kenh) {
        this.kenh = kenh;
    }

    public boolean coTheTao(String ten, Collection<String> thanhVien) {
        String chu = ten.strip();
        return !chu.isEmpty() && chu.length() <= 80 && chu.chars().noneMatch(Character::isISOControl)
                && !thanhVien.isEmpty() && thanhVien.size() <= gioiHanBan;
    }

    public CompletableFuture<JsonObject> tao(String ten, Collection<String> thanhVien, String anh) {
        if (!coTheTao(ten, thanhVien)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Nhập tên nhóm hợp lệ và chọn từ 1 đến 29 bạn bè."));
        }
        LanTao noiDung = new LanTao(ten.strip(), thanhVien.stream().sorted().toList(), anh);
        if (!noiDung.equals(lanTao)) {
            lanTao = noiDung;
            maTao = UUID.randomUUID().toString();
        }
        JsonArray ds = new JsonArray();
        noiDung.thanhVien().forEach(ds::add);
        return kenh.yeuCau("taoNhom", DuLieu.tao("ten", noiDung.ten(), "thanhVien", ds,
                "maTao", maTao, "anhDaiDien", noiDung.anh()));
    }

    public CompletableFuture<JsonObject> danhSach(String sau) {
        return kenh.yeuCau("layNhom", DuLieu.tao("sauId", sau));
    }

    public CompletableFuture<JsonObject> anh(String id) {
        return kenh.yeuCau("layAnhNhom", DuLieu.tao("nhomId", id));
    }

    public CompletableFuture<JsonObject> lichSu(String id, String truoc) {
        return kenh.yeuCau("lichSuNhom", DuLieu.tao("nhomId", id, "truocId", truoc));
    }

    public CompletableFuture<JsonObject> gui(String id, String chu) {
        if (!chu.equals(tinDangThuLai)) {
            tinDangThuLai = chu;
            maTin = UUID.randomUUID().toString();
        }
        return kenh.yeuCau("guiTinNhom", DuLieu.tao("nhomId", id, "noiDung", chu, "maGui", maTin));
    }

    public void daGui() {
        tinDangThuLai = null;
    }
}
