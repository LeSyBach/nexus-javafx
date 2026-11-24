package server.model;

import java.util.Locale;
import java.util.UUID;

public record CuocGoiModel(UUID id, long nguoiGoi, long nguoiNhan, KetQua ketQua, long soGiay, boolean video) {
    public CuocGoiModel(UUID id, long nguoiGoi, long nguoiNhan, KetQua ketQua, long soGiay) {
        this(id, nguoiGoi, nguoiNhan, ketQua, soGiay, false);
    }

    public enum KetQua {
        hoanTat, nho, tuChoi, huy, gianDoan
    }

    public String noiDung() {
        String loai = video ? "Cuộc gọi video" : "Cuộc gọi";
        return switch (ketQua) {
            case hoanTat -> (video ? loai : "Cuộc gọi thoại") + " · " + dinhDangThoiLuong();
            case nho -> loai + " nhỡ";
            case tuChoi -> loai + " đã bị từ chối";
            case huy -> loai + " đã hủy";
            case gianDoan -> loai + " bị gián đoạn · " + dinhDangThoiLuong();
        };
    }

    private String dinhDangThoiLuong() {
        long gio = soGiay / 3600;
        long phut = soGiay / 60 % 60;
        long giay = soGiay % 60;
        return gio == 0 ? String.format(Locale.ROOT, "%02d:%02d", phut, giay)
                : String.format(Locale.ROOT, "%d:%02d:%02d", gio, phut, giay);
    }
}
