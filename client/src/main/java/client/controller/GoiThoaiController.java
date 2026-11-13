package client.controller;

import client.hatang.AmThanhUDP;
import client.hatang.KenhTCP;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class GoiThoaiController {
    public enum TrangThai { ranh, dangGoi, cuocGoiDen, dangMoAmThanh, dangNoiChuyen }

    public record HienThi(TrangThai trangThai, String thongBao, boolean tatMic) {
    }

    private final KenhTCP kenh;
    private final Executor luongGiaoDien;
    private final Consumer<HienThi> capNhat;
    private UUID cuocGoiId;
    private String ten = "";
    private TrangThai trangThai = TrangThai.ranh;
    private AmThanhUDP amThanh;
    private boolean daMoAmThanh;
    private boolean tatMic;

    public GoiThoaiController(KenhTCP kenh, Executor luongGiaoDien, Consumer<HienThi> capNhat) {
        this.kenh = kenh;
        this.luongGiaoDien = luongGiaoDien;
        this.capNhat = capNhat;
    }

    public void goi(String nguoiNhanId, String tenNguoiNhan) {
        if (trangThai != TrangThai.ranh) {
            return;
        }
        if (!moSocket()) {
            return;
        }
        cuocGoiId = UUID.randomUUID();
        ten = tenNguoiNhan;
        trangThai = TrangThai.dangGoi;
        hien("Đang gọi " + ten + "…");
        yeuCau("moiGoiThoai", DuLieu.tao("cuocGoiId", cuocGoiId.toString(), "nguoiNhanId", nguoiNhanId,
                "port", Integer.toString(amThanh.port())));
    }

    public void batMay() {
        if (trangThai != TrangThai.cuocGoiDen) {
            return;
        }
        if (!moSocket()) {
            guiKetThuc(cuocGoiId);
            datLai("Không mở được cổng âm thanh UDP.");
            return;
        }
        trangThai = TrangThai.dangMoAmThanh;
        hien("Đang kết nối âm thanh với " + ten + "…");
        yeuCau("batMayGoiThoai", DuLieu.tao("cuocGoiId", cuocGoiId.toString(),
                "port", Integer.toString(amThanh.port())));
    }

    public void nhanSuKien(String loai, JsonObject duLieu) {
        UUID id = UUID.fromString(DuLieu.chu(duLieu, "cuocGoiId"));
        switch (loai) {
            case "cuocGoiDen" -> {
                if (trangThai != TrangThai.ranh) {
                    guiKetThuc(id);
                    return;
                }
                cuocGoiId = id;
                ten = DuLieu.chu(duLieu, "ten");
                trangThai = TrangThai.cuocGoiDen;
                hien(ten + " đang gọi cho bạn.");
            }
            case "cuocGoiBatDau" -> {
                if (id.equals(cuocGoiId) && amThanh != null && !daMoAmThanh
                        && (trangThai == TrangThai.dangGoi || trangThai == TrangThai.dangMoAmThanh)) {
                    batDauAmThanh(id, duLieu);
                }
            }
            case "cuocGoiKetThuc" -> {
                if (id.equals(cuocGoiId)) {
                    datLai(DuLieu.chu(duLieu, "thongBao"));
                }
            }
            default -> { }
        }
    }

    private void batDauAmThanh(UUID id, JsonObject duLieu) {
        daMoAmThanh = true;
        trangThai = TrangThai.dangMoAmThanh;
        hien("Đang kết nối âm thanh với " + ten + "…");
        String host = DuLieu.chu(duLieu, "host");
        int port = duLieu.get("port").getAsInt();
        UUID token = UUID.fromString(DuLieu.chu(duLieu, "mediaToken"));
        amThanh.batDau(host, port, token, () -> luongGiaoDien.execute(() -> {
            if (id.equals(cuocGoiId)) {
                trangThai = TrangThai.dangNoiChuyen;
                hien("Đang nói chuyện với " + ten);
            }
        }), loi -> luongGiaoDien.execute(() -> {
            if (id.equals(cuocGoiId)) {
                guiKetThuc(id);
                datLai(loi);
            }
        }));
    }

    public void doiMic() {
        if (trangThai == TrangThai.dangNoiChuyen) {
            tatMic = !tatMic;
            amThanh.tatMic(tatMic);
            hien("Đang nói chuyện với " + ten);
        }
    }

    public void ketThuc() {
        if (cuocGoiId != null) {
            guiKetThuc(cuocGoiId);
            datLai("Cuộc gọi đã kết thúc.");
        }
    }

    public void dong() {
        datLai("");
    }

    private boolean moSocket() {
        try {
            amThanh = new AmThanhUDP();
            return true;
        } catch (IOException loi) {
            hien("Không mở được cổng âm thanh UDP.");
            return false;
        }
    }

    private void yeuCau(String lenh, JsonObject duLieu) {
        UUID id = cuocGoiId;
        kenh.yeuCau(lenh, duLieu).whenCompleteAsync((phanHoi, loi) -> {
            if (loi == null || !id.equals(cuocGoiId)) {
                return;
            }
            guiKetThuc(id);
            Throwable nguyenNhan = loi instanceof CompletionException && loi.getCause() != null
                    ? loi.getCause() : loi;
            datLai(nguyenNhan.getMessage());
        }, luongGiaoDien);
    }

    private void guiKetThuc(UUID id) {
        kenh.yeuCau("ketThucGoiThoai", DuLieu.tao("cuocGoiId", id.toString()));
    }

    private void datLai(String thongBao) {
        if (amThanh != null) {
            amThanh.close();
            amThanh = null;
        }
        cuocGoiId = null;
        daMoAmThanh = false;
        tatMic = false;
        ten = "";
        trangThai = TrangThai.ranh;
        hien(thongBao);
    }

    private void hien(String thongBao) {
        capNhat.accept(new HienThi(trangThai, thongBao == null ? "Cuộc gọi bị gián đoạn." : thongBao, tatMic));
    }
}
