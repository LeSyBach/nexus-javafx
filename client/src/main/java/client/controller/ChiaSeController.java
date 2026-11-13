package client.controller;

import client.hatang.KenhTCP;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.util.concurrent.CompletableFuture;

import java.util.*;

/**
 * Gửi lần lượt và giữ mã cho các đích cần thử lại.
 */
public final class ChiaSeController {
    private final KenhTCP kenh;
    private final Map<String, String> maGui = new HashMap<>(), maLoiNhan = new HashMap<>();

    public record KetQua(List<String> thanhCong, List<Throwable> thatBai) {
    }

    public ChiaSeController(KenhTCP kenh) {
        this.kenh = kenh;
    }

    public CompletableFuture<KetQua> gui(String tinId, Collection<String> cacDich, String ghiChu) {
        List<String> thanhCong = new ArrayList<>();
        List<Throwable> thatBai = new ArrayList<>();
        CompletableFuture<Void> cho = CompletableFuture.completedFuture(null);
        for (String id : List.copyOf(cacDich)) {
            String dich = id.equals("0") ? null : id;
            String ma = maGui.computeIfAbsent(id, khoa -> UUID.randomUUID().toString());
            cho = cho.thenCompose(x -> kenh.yeuCau("chuyenTiepTin",
                            DuLieu.tao("tinNhanId", tinId, "nguoiNhanId", dich, "maGui", ma))
                    .thenCompose(tra -> ghiChu.isEmpty() ? CompletableFuture.completedFuture(tra) :
                            guiLoiNhan(id, dich, ghiChu))
                    .handle((tra, sai) -> {
                        if (sai == null) {
                            thanhCong.add(id);
                        } else {
                            thatBai.add(sai);
                        }
                        return null;
                    }));
        }
        return cho.thenApply(x -> new KetQua(List.copyOf(thanhCong), List.copyOf(thatBai)));
    }

    private CompletableFuture<JsonObject> guiLoiNhan(String id, String dich, String chu) {
        String ma = maLoiNhan.computeIfAbsent(id + "\n" + chu, khoa -> UUID.randomUUID().toString());
        return kenh.yeuCau("guiTin", DuLieu.tao("nguoiNhanId", dich, "noiDung", chu, "maGui", ma));
    }
}
