package client.controller;

import client.hatang.AmThanhUDP;
import client.hatang.ChuongGoiDen;
import client.hatang.KenhTCP;
import client.hatang.VideoUDP;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class GoiThoaiController {
    public enum TrangThai { ranh, dangGoi, cuocGoiDen, dangMoAmThanh, dangNoiChuyen }

    public record HienThi(TrangThai trangThai, String thongBao, boolean tatMic, String tenDoiPhuong,
                          boolean goiVideo, boolean tatCamera, String thongBaoVideo) {
    }

    public record HinhAnh(byte[] jpeg, boolean cuaMinh) {
    }

    private final KenhTCP kenh;
    private final Executor luongGiaoDien;
    private final Consumer<HienThi> capNhat;
    private final ChuongGoiDen chuongGoiDen = new ChuongGoiDen();
    private UUID cuocGoiId;
    private String ten = "";
    private TrangThai trangThai = TrangThai.ranh;
    private AmThanhUDP amThanh;
    private VideoUDP video;
    private boolean goiVideo;
    private boolean tatCamera;
    private String thongBaoVideo = "";
    private Consumer<HinhAnh> capNhatHinh = anh -> { };
    private final AtomicBoolean choAnhCuaMinh = new AtomicBoolean();
    private final AtomicBoolean choAnhDoiPhuong = new AtomicBoolean();
    private boolean daMoAmThanh;
    private boolean tatMic;

    public GoiThoaiController(KenhTCP kenh, Executor luongGiaoDien, Consumer<HienThi> capNhat) {
        this.kenh = kenh;
        this.luongGiaoDien = luongGiaoDien;
        this.capNhat = capNhat;
    }

    public void goi(String nguoiNhanId, String tenNguoiNhan) {
        goi(nguoiNhanId, tenNguoiNhan, false);
    }

    public void goiVideo(String nguoiNhanId, String tenNguoiNhan) {
        goi(nguoiNhanId, tenNguoiNhan, true);
    }

    public void khiCoHinh(Consumer<HinhAnh> capNhatHinh) {
        this.capNhatHinh = capNhatHinh;
    }

    private void goi(String nguoiNhanId, String tenNguoiNhan, boolean dungVideo) {
        if (trangThai != TrangThai.ranh) {
            return;
        }
        goiVideo = dungVideo;
        if (!moSocket()) {
            goiVideo = false;
            return;
        }
        cuocGoiId = UUID.randomUUID();
        ten = tenNguoiNhan;
        trangThai = TrangThai.dangGoi;
        hien("Đang gọi " + ten + "…");
        yeuCau("moiGoiThoai", DuLieu.tao("cuocGoiId", cuocGoiId.toString(), "nguoiNhanId", nguoiNhanId,
                "port", Integer.toString(amThanh.port()), "videoPort", video == null ? 0 : video.port()));
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
                "port", Integer.toString(amThanh.port()), "videoPort", video == null ? 0 : video.port()));
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
                goiVideo = duLieu.has("goiVideo") && duLieu.get("goiVideo").getAsBoolean();
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
        if (goiVideo) {
            int videoPort = duLieu.has("videoPort") ? duLieu.get("videoPort").getAsInt() : 0;
            if (videoPort < 1 || videoPort > 65535) {
                guiKetThuc(id);
                datLai("Máy chủ hoặc client chưa hỗ trợ video. Hãy cập nhật cả hai máy.");
                return;
            }
            try {
                video.tatCamera(tatCamera);
                video.batDau(host, videoPort, token, anh -> nhanHinh(id, anh, true),
                        anh -> nhanHinh(id, anh, false),
                        loi -> luongGiaoDien.execute(() -> baoLoiVideo(id, loi)));
            } catch (IOException loi) {
                baoLoiVideo(id, "Không mở được đường truyền video.");
            }
        }
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

    public void doiCamera() {
        if (goiVideo && trangThai != TrangThai.ranh) {
            tatCamera = !tatCamera;
            thongBaoVideo = "";
            if (video != null) {
                video.tatCamera(tatCamera);
            }
            if (tatCamera) {
                capNhatHinh.accept(new HinhAnh(new byte[0], true));
            }
            hien("Cuộc gọi video với " + ten);
        }
    }

    private void baoLoiVideo(UUID id, String loi) {
        if (id.equals(cuocGoiId)) {
            thongBaoVideo = loi;
            tatCamera = true;
            video.tatCamera(true);
            capNhatHinh.accept(new HinhAnh(new byte[0], true));
            hien("Cuộc gọi video với " + ten);
        }
    }

    private void nhanHinh(UUID id, byte[] jpeg, boolean cuaMinh) {
        AtomicBoolean cho = cuaMinh ? choAnhCuaMinh : choAnhDoiPhuong;
        if (!cho.compareAndSet(false, true)) {
            return;
        }
        luongGiaoDien.execute(() -> {
            try {
                if (id.equals(cuocGoiId) && !(cuaMinh && tatCamera && jpeg.length > 0)) {
                    capNhatHinh.accept(new HinhAnh(jpeg, cuaMinh));
                }
            } finally {
                cho.set(false);
            }
        });
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
            if (goiVideo) {
                video = new VideoUDP();
            }
            return true;
        } catch (IOException loi) {
            if (amThanh != null) {
                amThanh.close();
                amThanh = null;
            }
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
        if (video != null) {
            video.close();
            video = null;
        }
        cuocGoiId = null;
        daMoAmThanh = false;
        tatMic = false;
        trangThai = TrangThai.ranh;
        hien(thongBao);
        ten = "";
        goiVideo = false;
        tatCamera = false;
        thongBaoVideo = "";
    }

    private void hien(String thongBao) {
        if (trangThai == TrangThai.cuocGoiDen) {
            chuongGoiDen.bat();
        } else {
            chuongGoiDen.tat();
        }
        capNhat.accept(new HienThi(trangThai, thongBao == null ? "Cuộc gọi bị gián đoạn." : thongBao,
                tatMic, ten, goiVideo, tatCamera, thongBaoVideo));
    }
}
