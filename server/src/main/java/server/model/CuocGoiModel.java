package server.model;

import java.util.Locale;
import java.util.UUID;

public record CuocGoiModel(UUID id, long nguoiGoi, long nguoiNhan, KetQua ketQua, long soGiay) {
    public enum KetQua {
        hoanTat, nho, tuChoi, huy, gianDoan
    }

    public String noiDung() {
        return switch (ketQua) {
            case hoanTat -> "Cuộc gọi thoại · " + dinhDangThoiLuong();
            case nho -> "Cuộc gọi nhỡ";
            case tuChoi -> "Cuộc gọi đã bị từ chối";
            case huy -> "Cuộc gọi đã hủy";
            case gianDoan -> "Cuộc gọi bị gián đoạn · " + dinhDangThoiLuong();
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
