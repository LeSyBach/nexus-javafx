package server.controller;

import giaothuc.DuLieu;
import giaothuc.ThongDiep;
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

    private record CuocGoi(UUID id, DauGoi nguoiGoi, DauGoi nguoiNhan, int portNguoiGoi, boolean daBatMay) {
        DauGoi benKia(Object phien) {
            return nguoiGoi.phien() == phien ? nguoiNhan : nguoiGoi;
        }
    }

    private final Map<UUID, CuocGoi> cacCuocGoi = new HashMap<>();

    synchronized void moi(UUID id, DauGoi nguoiGoi, DauGoi nguoiNhan, int portNguoiGoi) {
        if (nguoiGoi.id() == nguoiNhan.id() || !trongLan(nguoiGoi.ip()) || !trongLan(nguoiNhan.ip())
                || nguoiGoi.ip().isLoopbackAddress() != nguoiNhan.ip().isLoopbackAddress()
                || nguoiGoi.ip().getClass() != nguoiNhan.ip().getClass()) {
            throw new LoiNghiepVuModel("mangGoiThoaiSai",
                    "Hai máy cần dùng địa chỉ cùng mạng LAN để gọi thoại.");
        }
        if (cacCuocGoi.containsKey(id) || dangGoi(nguoiGoi.id()) || dangGoi(nguoiNhan.id())) {
            throw new LoiNghiepVuModel("cuocGoiBan", "Một trong hai người đang có cuộc gọi.");
        }

        CuocGoi cuocGoi = new CuocGoi(id, nguoiGoi, nguoiNhan, portNguoiGoi, false);
        cacCuocGoi.put(id, cuocGoi);
        CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> hetHan(cuocGoi));
        nguoiNhan.bao("cuocGoiDen", "cuocGoiId", id.toString(), "ten", nguoiGoi.ten());
    }

    synchronized void batMay(UUID id, Object phien, int portNguoiNhan) {
        CuocGoi cuocGoi = cacCuocGoi.get(id);
        if (cuocGoi == null || cuocGoi.nguoiNhan().phien() != phien || cuocGoi.daBatMay()) {
            throw new LoiNghiepVuModel("cuocGoiHetHan", "Lời mời gọi đã kết thúc.");
        }
        DauGoi nguoiGoi = cuocGoi.nguoiGoi();
        DauGoi nguoiNhan = cuocGoi.nguoiNhan();
        UUID mediaToken = UUID.randomUUID();
        CuocGoi dangGoi = new CuocGoi(id, nguoiGoi, nguoiNhan, cuocGoi.portNguoiGoi(), true);
        cacCuocGoi.put(id, dangGoi);
        baoBatDau(id, mediaToken, nguoiGoi, nguoiNhan.ip(), portNguoiNhan);
        if (cacCuocGoi.get(id) == dangGoi) {
            baoBatDau(id, mediaToken, nguoiNhan, nguoiGoi.ip(), cuocGoi.portNguoiGoi());
        }
    }

    synchronized void ketThuc(UUID id, Object phien) {
        CuocGoi cuocGoi = cacCuocGoi.get(id);
        if (cuocGoi == null || !thuocPhien(cuocGoi, phien)) {
            return;
        }
        String thongBao = cuocGoi.daBatMay() ? "Cuộc gọi đã kết thúc."
                : cuocGoi.nguoiNhan().phien() == phien ? "Người nhận đã từ chối cuộc gọi."
                : "Người gọi đã hủy lời mời.";
        ketThuc(cuocGoi, thongBao);
    }

    synchronized void roi(Object phien) {
        cacCuocGoi.values().stream()
                .filter(cuocGoi -> thuocPhien(cuocGoi, phien))
                .findFirst()
                .ifPresent(cuocGoi -> {
                    cacCuocGoi.remove(cuocGoi.id());
                    baoKetThuc(cuocGoi.benKia(phien), cuocGoi.id(), "Đối phương đã ngắt kết nối.");
                });
    }

    synchronized void khiChan(long nguoi, long doiPhuong) {
        cacCuocGoi.values().stream()
                .filter(cuocGoi -> (cuocGoi.nguoiGoi().id() == nguoi && cuocGoi.nguoiNhan().id() == doiPhuong)
                        || (cuocGoi.nguoiNhan().id() == nguoi && cuocGoi.nguoiGoi().id() == doiPhuong))
                .findFirst()
                .ifPresent(cuocGoi -> ketThuc(cuocGoi, "Cuộc gọi đã kết thúc."));
    }

    private synchronized void hetHan(CuocGoi cuocGoi) {
        if (cacCuocGoi.get(cuocGoi.id()) == cuocGoi) {
            ketThuc(cuocGoi, "Hết thời gian chờ bắt máy.");
        }
    }

    private void ketThuc(CuocGoi cuocGoi, String thongBao) {
        cacCuocGoi.remove(cuocGoi.id());
        baoKetThuc(cuocGoi.nguoiGoi(), cuocGoi.id(), thongBao);
        baoKetThuc(cuocGoi.nguoiNhan(), cuocGoi.id(), thongBao);
    }

    private boolean dangGoi(long id) {
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
