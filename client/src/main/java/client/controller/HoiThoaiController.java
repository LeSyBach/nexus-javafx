package client.controller;

import client.hatang.KenhTCP;
import client.hatang.TruyenTepTCP;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.io.IOException;
import java.nio.file.Files;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Điều phối tin riêng, phòng chung, nội dung chia sẻ và truyền tệp.
 */
public final class HoiThoaiController {
    private final KenhTCP kenh;
    private final TruyenTepTCP truyenTep;
    private final Executor luongGiaoDien;
    private boolean dangTaiDanhSach;
    private boolean canTaiLaiDanhSach;
    private boolean daDong;
    private final Map<String, LanGui> cacLanGui = new HashMap<>();

    private record LanGui(String dau, String ma) {
    }

    public HoiThoaiController(KenhTCP kenh, Executor luongGiaoDien) {
        this.kenh = kenh;
        this.luongGiaoDien = luongGiaoDien;
        truyenTep = new TruyenTepTCP(kenh);
    }

    public enum LoaiSuKien {
        danhSachNguoi, nhomCapNhat, tinMoi, tinCapNhat, tinBiXoa, ghimCapNhat, doiPhuongDaDoc,
        danhSachThayDoi, hoiThoaiBiXoa, banBeCapNhat, hoSoCapNhat, trangThaiChanDoi,
        cuocGoiDen, cuocGoiBatDau, cuocGoiKetThuc
    }

    public record SuKien(LoaiSuKien loai, JsonObject duLieu) {
    }

    public void khiCoSuKien(Consumer<SuKien> nhan) {
        kenh.khiCoSuKien(thongDiep -> {
            LoaiSuKien loai = phanLoaiSuKien(thongDiep.loai());
            if (loai != null) {
                SuKien suKien = new SuKien(loai, thongDiep.duLieu());
                luongGiaoDien.execute(() -> {
                    if (!daDong) {
                        nhan.accept(suKien);
                    }
                });
            }
        });
    }

    private LoaiSuKien phanLoaiSuKien(String loai) {
        return switch (loai) {
            case "danhSachNguoiDung" -> LoaiSuKien.danhSachNguoi;
            case "nhomDoi", "tinNhanNhomMoi" -> LoaiSuKien.nhomCapNhat;
            case "tinNhanMoi" -> LoaiSuKien.tinMoi;
            case "tinNhanDoi" -> LoaiSuKien.tinCapNhat;
            case "tinBiXoa" -> LoaiSuKien.tinBiXoa;
            case "ghimDoi" -> LoaiSuKien.ghimCapNhat;
            case "doiPhuongDaDoc" -> LoaiSuKien.doiPhuongDaDoc;
            case "mocDocDoi", "luuTruDoi" -> LoaiSuKien.danhSachThayDoi;
            case "hoiThoaiBiXoa" -> LoaiSuKien.hoiThoaiBiXoa;
            case "banBeDoi" -> LoaiSuKien.banBeCapNhat;
            case "hoSoDoi" -> LoaiSuKien.hoSoCapNhat;
            case "chanNguoiDungDoi" -> LoaiSuKien.trangThaiChanDoi;
            case "cuocGoiDen" -> LoaiSuKien.cuocGoiDen;
            case "cuocGoiBatDau" -> LoaiSuKien.cuocGoiBatDau;
            case "cuocGoiKetThuc" -> LoaiSuKien.cuocGoiKetThuc;
            default -> null;
        };
    }

    public CompletableFuture<JsonObject> danhSachNguoi() {
        return kenh.yeuCau("layDanhSach", new JsonObject());
    }

    public void taiDanhSachHoiThoai(Consumer<Map<String, JsonObject>> khiTaiXong,
                                     Consumer<Throwable> khiLoi) {
        if (daDong) {
            return;
        }
        if (dangTaiDanhSach) {
            canTaiLaiDanhSach = true;
            return;
        }
        dangTaiDanhSach = true;
        taiTrangHoiThoai(0, new HashMap<>(), khiTaiXong, khiLoi);
    }

    private void taiTrangHoiThoai(int boQua, Map<String, JsonObject> ketQua,
                                 Consumer<Map<String, JsonObject>> khiTaiXong, Consumer<Throwable> khiLoi) {
        kenh.yeuCau("layHoiThoai", DuLieu.tao("boQua", Integer.toString(boQua)))
                .whenCompleteAsync((duLieu, sai) -> {
                    if (daDong) {
                        return;
                    }
                    if (sai == null) {
                        JsonArray trang = duLieu.getAsJsonArray("hoiThoai");
                        for (JsonElement phanTu : trang) {
                            JsonObject hoiThoai = phanTu.getAsJsonObject();
                            ketQua.put(DuLieu.chu(hoiThoai, "id"), hoiThoai);
                        }
                        if (trang.size() == 50) {
                            taiTrangHoiThoai(boQua + 50, ketQua, khiTaiXong, khiLoi);
                            return;
                        }
                    }

                    dangTaiDanhSach = false;
                    if (sai == null) {
                        khiTaiXong.accept(ketQua);
                    } else {
                        khiLoi.accept(sai);
                    }
                    if (canTaiLaiDanhSach) {
                        canTaiLaiDanhSach = false;
                        taiDanhSachHoiThoai(khiTaiXong, khiLoi);
                    }
                }, luongGiaoDien);
    }

    public CompletableFuture<JsonObject> lichSu(String dich, String truoc) {
        return kenh.yeuCau("lichSu", DuLieu.tao("nguoiNhanId", dich, "truocId", truoc));
    }

    public CompletableFuture<JsonObject> danhDauDoc(String dich, long id) {
        return kenh.yeuCau("danhDauDoc",
                DuLieu.tao("nguoiNhanId", dich, "tinCuoiId", Long.toString(id)));
    }

    public CompletableFuture<JsonObject> thongTin(String dich, String loai, String truoc) {
        return kenh.yeuCau("thongTinChat",
                DuLieu.tao("nguoiNhanId", dich, "loai", loai, "truocId", truoc));
    }

    public CompletableFuture<JsonObject> thaoTacTin(String loai, JsonObject duLieu) {
        return switch (loai) {
            case "ghimTin", "boGhimTin", "thuHoiTin", "datCamXuc", "xoaTin" ->
                    kenh.yeuCau(loai, duLieu);
            default -> CompletableFuture.failedFuture(
                    new IllegalArgumentException("Thao tác tin nhắn chưa hợp lệ."));
        };
    }

    public CompletableFuture<JsonObject> boGhim(String id) {
        return thaoTacTin("boGhimTin", DuLieu.tao("tinNhanId", id));
    }

    public CompletableFuture<JsonObject> luuTru(String dich, boolean daLuu) {
        return kenh.yeuCau(daLuu ? "boLuuTruHoiThoai" : "luuTruHoiThoai",
                DuLieu.tao("nguoiNhanId", dich));
    }

    public CompletableFuture<JsonObject> xoa(String dich) {
        return kenh.yeuCau("xoaHoiThoai", DuLieu.tao("nguoiNhanId", dich));
    }

    private String khoaGui(String dich, boolean tep) {
        return tep ? "tep:" + (dich == null ? "chung" : dich) : dich == null ? "phongChung" : dich;
    }

    private String maGui(String khoa, String dau) {
        LanGui lan = cacLanGui.get(khoa);
        if (lan == null || !lan.dau().equals(dau)) {
            lan = new LanGui(dau, UUID.randomUUID().toString());
            cacLanGui.put(khoa, lan);
        }
        return lan.ma();
    }

    public CompletableFuture<JsonObject> guiTin(String dich, String chu, String traLoi) {
        String banNhap = DuLieu.tao("chu", chu, "traLoi", traLoi).toString();
        String ma = maGui(khoaGui(dich, false), banNhap);

        JsonObject duLieu = DuLieu.tao(
                "nguoiNhanId", dich,
                "noiDung", chu,
                "maGui", ma,
                "traLoiId", traLoi
        );
        return kenh.yeuCau("guiTin", duLieu);
    }

    public void kiemTraTepGui(Path tep) throws IOException {
        if (!Files.isRegularFile(tep) || Files.size(tep) > TruyenTepTCP.gioiHan) {
            throw new IOException("Chọn tệp thường tối đa 10 MB.");
        }
    }

    public CompletableFuture<JsonObject> guiTep(Path tep, String dich, String chu, String traLoi,
                                                DoubleConsumer tienTrinh) {
        return guiTep(tep, dich, chu, traLoi, false, tienTrinh);
    }

    public CompletableFuture<JsonObject> guiTinThoai(Path tep, String dich, String traLoi,
                                                     DoubleConsumer tienTrinh) {
        return guiTep(tep, dich, "", traLoi, true, tienTrinh);
    }

    private CompletableFuture<JsonObject> guiTep(Path tep, String dich, String chu, String traLoi,
                                                  boolean tinThoai, DoubleConsumer tienTrinh) {
        String dau =
                tep.toAbsolutePath() + ":" + tep.toFile().length() + ":" + tep.toFile().lastModified() + ":" +
                        DuLieu.tao("chu", chu, "traLoi", traLoi, "tinThoai", tinThoai);
        return truyenTep.gui(tep, dich, chu, maGui(khoaGui(dich, true), dau), traLoi, tinThoai, tienTrinh);
    }

    public void daGui(String dich, boolean tep) {
        cacLanGui.remove(khoaGui(dich, tep));
    }

    public CompletableFuture<Path> taiTep(String id, Path dich, DoubleConsumer tienTrinh) {
        return truyenTep.tai(id, dich, tienTrinh);
    }

    public CompletableFuture<Path> taiTinThoai(String id) {
        return truyenTep.taiTam(id);
    }

    public boolean dangTruyen() {
        return truyenTep.dangTruyen();
    }

    public void huyTruyen() {
        truyenTep.huy();
    }

    public void dong() {
        daDong = true;
        truyenTep.close();
        kenh.khiCoSuKien(tin -> {
        });
        kenh.close();
    }
}
