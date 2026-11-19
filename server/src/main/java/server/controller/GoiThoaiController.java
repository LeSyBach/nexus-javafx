package server.controller;

import giaothuc.DuLieu;
import giaothuc.ThongDiep;
import server.model.CuocGoiModel;
import server.model.CuocGoiModel.KetQua;
import server.model.LoiNghiepVuModel;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Điều phối lời mời và phiên gọi qua TCP. Âm thanh đi trực tiếp giữa hai app. */
final class GoiThoaiController {
    record DauGoi(Object phien, long id, String ten, InetAddress ip, Consumer<ThongDiep> gui) {
        void bao(String loai, Object... duLieu) {
            gui.accept(ThongDiep.suKien(loai, DuLieu.tao(duLieu)));
        }
    }

    private record CuocGoi(UUID id, DauGoi nguoiGoi, DauGoi nguoiNhan, int portNguoiGoi, Long lucBatMay) {
        boolean daBatMay() {
            return lucBatMay != null;
        }

        DauGoi benKia(Object phien) {
            return nguoiGoi.phien() == phien ? nguoiNhan : nguoiGoi;
        }
    }

    private final Map<UUID, CuocGoi> cacCuocGoi = new HashMap<>();
    private final Object khoaChat;
    private final Consumer<CuocGoiModel> luuLichSu;

    GoiThoaiController(Object khoaChat, Consumer<CuocGoiModel> luuLichSu) {
        this.khoaChat = khoaChat;
        this.luuLichSu = luuLichSu;
    }

    // Các thao tác dùng cùng khóa với lịch sử và tin nhắn riêng ở MayChuController.
    void moi(UUID id, DauGoi nguoiGoi, DauGoi nguoiNhan, int portNguoiGoi) {
        if (nguoiGoi.id() == nguoiNhan.id() || !trongLan(nguoiGoi.ip()) || !trongLan(nguoiNhan.ip())
                || nguoiGoi.ip().isLoopbackAddress() != nguoiNhan.ip().isLoopbackAddress()
                || nguoiGoi.ip().getClass() != nguoiNhan.ip().getClass()) {
            throw new LoiNghiepVuModel("mangGoiThoaiSai",
                    "Hai máy cần dùng địa chỉ cùng mạng LAN để gọi thoại.");
        }
        if (cacCuocGoi.containsKey(id) || dangGoi(nguoiGoi.id()) || dangGoi(nguoiNhan.id())) {
            throw new LoiNghiepVuModel("cuocGoiBan", "Một trong hai người đang có cuộc gọi.");
        }

        CuocGoi cuocGoi = new CuocGoi(id, nguoiGoi, nguoiNhan, portNguoiGoi, null);
        cacCuocGoi.put(id, cuocGoi);
        CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> hetHan(cuocGoi));
        nguoiNhan.bao("cuocGoiDen", "cuocGoiId", id.toString(), "ten", nguoiGoi.ten());
    }

    void batMay(UUID id, Object phien, int portNguoiNhan) {
        CuocGoi cuocGoi = cacCuocGoi.get(id);
        if (cuocGoi == null || cuocGoi.nguoiNhan().phien() != phien || cuocGoi.daBatMay()) {
            throw new LoiNghiepVuModel("cuocGoiHetHan", "Lời mời gọi đã kết thúc.");
        }
        DauGoi nguoiGoi = cuocGoi.nguoiGoi();
        DauGoi nguoiNhan = cuocGoi.nguoiNhan();
        UUID mediaToken = UUID.randomUUID();
        CuocGoi dangGoi = new CuocGoi(id, nguoiGoi, nguoiNhan, cuocGoi.portNguoiGoi(), System.nanoTime());
        cacCuocGoi.put(id, dangGoi);
        baoBatDau(id, mediaToken, nguoiGoi, nguoiNhan.ip(), portNguoiNhan);
        if (cacCuocGoi.get(id) == dangGoi) {
            baoBatDau(id, mediaToken, nguoiNhan, nguoiGoi.ip(), cuocGoi.portNguoiGoi());
        }
    }

    void ketThuc(UUID id, Object phien) {
        CuocGoi cuocGoi = cacCuocGoi.get(id);
        if (cuocGoi == null || !thuocPhien(cuocGoi, phien)) {
            return;
        }
        if (cuocGoi.daBatMay()) {
            ketThuc(cuocGoi, KetQua.hoanTat, "Cuộc gọi đã kết thúc.");
        } else if (cuocGoi.nguoiNhan().phien() == phien) {
            ketThuc(cuocGoi, KetQua.tuChoi, "Người nhận đã từ chối cuộc gọi.");
        } else {
            ketThuc(cuocGoi, KetQua.huy, "Người gọi đã hủy lời mời.");
        }
    }

    void roi(Object phien) {
        cacCuocGoi.values().stream()
                .filter(cuocGoi -> thuocPhien(cuocGoi, phien))
                .findFirst()
                .ifPresent(cuocGoi -> {
                    KetQua ketQua = cuocGoi.daBatMay() ? KetQua.gianDoan
                            : cuocGoi.nguoiNhan().phien() == phien ? KetQua.nho : KetQua.huy;
                    CuocGoiModel lichSu = xoaKhoiPhienDangGoi(cuocGoi, ketQua);
                    baoKetThuc(cuocGoi.benKia(phien), cuocGoi.id(), "Đối phương đã ngắt kết nối.");
                    luuLichSu.accept(lichSu);
                });
    }

    void khiChan(long nguoi, long doiPhuong) {
        cacCuocGoi.values().stream()
                .filter(cuocGoi -> (cuocGoi.nguoiGoi().id() == nguoi && cuocGoi.nguoiNhan().id() == doiPhuong)
                        || (cuocGoi.nguoiNhan().id() == nguoi && cuocGoi.nguoiGoi().id() == doiPhuong))
                .findFirst()
                .ifPresent(cuocGoi -> ketThuc(cuocGoi,
                        cuocGoi.daBatMay() ? KetQua.gianDoan : KetQua.huy, "Cuộc gọi đã kết thúc."));
    }

    private void hetHan(CuocGoi cuocGoi) {
        synchronized (khoaChat) {
            if (cacCuocGoi.get(cuocGoi.id()) == cuocGoi) {
                ketThuc(cuocGoi, KetQua.nho, "Hết thời gian chờ bắt máy.");
            }
        }
    }

    private void ketThuc(CuocGoi cuocGoi, KetQua ketQua, String thongBao) {
        CuocGoiModel lichSu = xoaKhoiPhienDangGoi(cuocGoi, ketQua);
        baoKetThuc(cuocGoi.nguoiGoi(), cuocGoi.id(), thongBao);
        baoKetThuc(cuocGoi.nguoiNhan(), cuocGoi.id(), thongBao);
        luuLichSu.accept(lichSu);
    }

    private CuocGoiModel xoaKhoiPhienDangGoi(CuocGoi cuocGoi, KetQua ketQua) {
        cacCuocGoi.remove(cuocGoi.id());
        long soGiay = cuocGoi.daBatMay()
                ? TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - cuocGoi.lucBatMay()) : 0;
        return new CuocGoiModel(cuocGoi.id(), cuocGoi.nguoiGoi().id(), cuocGoi.nguoiNhan().id(), ketQua, soGiay);
    }

    boolean dangGoi(long id) {
        return cacCuocGoi.values().stream()
                .anyMatch(cuocGoi -> cuocGoi.nguoiGoi().id() == id || cuocGoi.nguoiNhan().id() == id);
    }

    private static boolean thuocPhien(CuocGoi cuocGoi, Object phien) {
        return cuocGoi.nguoiGoi().phien() == phien || cuocGoi.nguoiNhan().phien() == phien;
    }

    private static boolean trongLan(InetAddress ip) {
        return Objects.nonNull(ip) && (ip.isLoopbackAddress() || ip.isSiteLocalAddress());
    }

    private static void baoBatDau(UUID id, UUID mediaToken, DauGoi dich, InetAddress ip, int port) {
        dich.bao("cuocGoiBatDau", "cuocGoiId", id.toString(), "host", ip.getHostAddress(),
                "port", port, "mediaToken", mediaToken.toString());
    }

    private static void baoKetThuc(DauGoi dich, UUID id, String thongBao) {
        dich.bao("cuocGoiKetThuc", "cuocGoiId", id.toString(), "thongBao", thongBao);
    }
}
