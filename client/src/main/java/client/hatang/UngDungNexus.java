package client.hatang;

import client.controller.DangNhapController;
import client.controller.TaiKhoanController;
import client.view.DoHoaView;
import client.model.TaiKhoanModel;
import client.view.DangNhapView;
import client.view.HoiThoaiView;
import client.view.TaiKhoanView;
import client.view.ThongBaoNoiView;

import javafx.application.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.util.*;
import java.util.concurrent.*;

import static client.model.TaiKhoanModel.TaiKhoan;

public final class UngDungNexus extends Application {
    private Stage cuaSo;
    private KenhTCP kenh;
    private DangNhapView dangNhap;
    private HoiThoaiView hoiThoai;
    private TaiKhoanView manHinhTaiKhoan;
    private javafx.scene.Parent trangTruocTaiKhoan;
    private final TaiKhoanModel khoTaiKhoan;

    public UngDungNexus() {
        this(TaiKhoanModel.macDinh());
    }

    public UngDungNexus(TaiKhoanModel khoTaiKhoan) {
        this.khoTaiKhoan = khoTaiKhoan;
    }

    private final List<TaiKhoan> cacTaiKhoan = new ArrayList<>();
    private TaiKhoan dangDung;
    private CompletableFuture<Void> viecLuu = CompletableFuture.completedFuture(null);
    private boolean dangChuyen, daDung;
    private String loiLuu = "";

    @Override
    public void start(Stage cuaSo) {
        this.cuaSo = cuaSo;
        cuaSo.setTitle("NEXUS · Kết nối gần hơn");
        cuaSo.setMinWidth(960);
        cuaSo.setMinHeight(680);
        hienDangNhap("Đang đọc tài khoản đã lưu…");
        cuaSo.getScene().getRoot().setDisable(true);
        cuaSo.show();
        CompletableFuture.supplyAsync(() -> {
                    try {
                        return khoTaiKhoan.doc();
                    } catch (java.io.IOException loi) {
                        throw new CompletionException(loi);
                    }
                })
                .whenComplete((daLuu, sai) -> Platform.runLater(() -> {
                    if (daDung) {
                        return;
                    }
                    cuaSo.getScene().getRoot().setDisable(false);
                    if (sai != null) {
                        hienDangNhap(DoHoaView.thongBao(sai));
                        return;
                    }
                    cacTaiKhoan.addAll(daLuu);
                    if (cacTaiKhoan.isEmpty()) {
                        hienDangNhap("");
                    } else {
                        chuyenTaiKhoan(cacTaiKhoan.get(0));
                    }
                }));
    }

    private void dongManHinh() {
        manHinhTaiKhoan = null;
        trangTruocTaiKhoan = null;
        if (hoiThoai != null) {
            hoiThoai.dong();
            hoiThoai = null;
        }
        if (kenh != null) {
            kenh.close();
            kenh = null;
        }
        if (dangNhap != null) {
            dangNhap.dong();
            dangNhap = null;
        }
    }

    private void hienDangNhap(String thongBao) {
        dongManHinh();
        dangDung = null;
        dangNhap = new DangNhapView(new DangNhapController(Platform::runLater),
                ketQua -> dangNhapThanhCong(ketQua.kenh(), ketQua.phanHoi()), thongBao);
        BorderPane trang = new BorderPane(dangNhap);
        if (!cacTaiKhoan.isEmpty()) {
            Button daLuu =
                    DoHoaView.nut("Tài khoản đã đăng nhập (" + cacTaiKhoan.size() + ")", "nutHoSoPhu");
            daLuu.setOnAction(suKien -> moTaiKhoan());
            HBox dau = new HBox(daLuu);
            dau.getStyleClass().add("thanhTaiKhoanLuu");
            trang.setTop(dau);
        }
        datNoiDung(trang);
    }

    private void dangNhapThanhCong(KenhTCP moi, JsonObject phanHoi) {
        JsonObject nguoi = phanHoi.getAsJsonObject("nguoiDung");
        TaiKhoan taiKhoan = new TaiKhoan(moi.mayChu(), moi.cong(), DuLieu.chu(nguoi, "id"),
                DuLieu.chu(nguoi, "tenHienThi"), DuLieu.chu(nguoi, "email"),
                DuLieu.chu(phanHoi, "maPhien"));
        ghiNho(taiKhoan);
        hienHoiThoai(moi, nguoi, taiKhoan);
    }

    private void hienHoiThoai(KenhTCP moi, JsonObject nguoi, TaiKhoan taiKhoan) {
        dongManHinh();
        dangDung = taiKhoan;
        kenh = moi;
        hoiThoai = new HoiThoaiView(moi, nguoi, () -> dangXuat(dangDung), this::moTaiKhoan,
                this::capNhatTaiKhoan);
        moi.khiMatKetNoi(lyDo -> Platform.runLater(() -> {
            if (kenh == moi) {
                hienDangNhap(lyDo);
            }
        }));
        moi.khiPhienBiHuy(lyDo -> Platform.runLater(() -> {
            if (kenh != moi) {
                return;
            }
            xoaDaLuu(taiKhoan);
            hienDangNhap(lyDo);
        }));
        datNoiDung(hoiThoai);
        moi.startHeartbeat();
        hoiThoai.batDau();
    }

    private void ghiNho(TaiKhoan taiKhoan) {
        cacTaiKhoan.removeIf(cu -> cu.khoa().equals(taiKhoan.khoa()));
        cacTaiKhoan.add(0, taiKhoan);
        luuDanhSach();
    }

    private void capNhatTaiKhoan(JsonObject hoSo) {
        if (dangDung == null) {
            return;
        }
        TaiKhoan moi = new TaiKhoan(dangDung.mayChu(), dangDung.cong(), dangDung.id(),
                DuLieu.chu(hoSo, "tenHienThi"), DuLieu.chu(hoSo, "email"), dangDung.maPhien());
        if (!moi.equals(dangDung)) {
            dangDung = moi;
            ghiNho(moi);
        }
    }

    private void xoaDaLuu(TaiKhoan taiKhoan) {
        cacTaiKhoan.removeIf(cu -> cu.khoa().equals(taiKhoan.khoa()));
        luuDanhSach();
    }

    private void luuDanhSach() {
        List<TaiKhoan> banSao = List.copyOf(cacTaiKhoan);
        viecLuu = viecLuu.handle((x, sai) -> null).thenRunAsync(() -> {
            try {
                khoTaiKhoan.ghi(banSao);
            } catch (java.io.IOException loi) {
                throw new CompletionException(loi);
            }
        });
        viecLuu.whenComplete((x, sai) -> Platform.runLater(() -> {
            if (daDung) {
                return;
            }
            loiLuu = sai == null ? "" : "Chưa lưu được phiên trên máy: " + DoHoaView.thongBao(sai);
            if (sai != null && dangMoTaiKhoan()) {
                manHinhTaiKhoan.datTrangThai(false, loiLuu);
            }
        }));
    }

    private boolean dangMoTaiKhoan() {
        return manHinhTaiKhoan != null && cuaSo.getScene().getRoot() == manHinhTaiKhoan;
    }

    private void moTaiKhoan() {
        if (dangMoTaiKhoan()) {
            return;
        }
        trangTruocTaiKhoan = cuaSo.getScene().getRoot();
        taoTrangTaiKhoan();
    }

    private void taoTrangTaiKhoan() {
        manHinhTaiKhoan = new TaiKhoanView(List.copyOf(cacTaiKhoan), dangDung == null ? "" : dangDung.khoa(),
                this::chuyenTaiKhoan, this::dangXuat,
                () -> hienDangNhap("Đăng nhập thêm tài khoản. Các tài khoản trước vẫn được giữ."),
                this::quayLaiTuTaiKhoan);
        manHinhTaiKhoan.datTrangThai(false, loiLuu);
        datNoiDung(manHinhTaiKhoan);
    }

    private void quayLaiTuTaiKhoan() {
        if (dangChuyen) {
            return;
        }
        javafx.scene.Parent truoc = trangTruocTaiKhoan;
        manHinhTaiKhoan = null;
        trangTruocTaiKhoan = null;
        if (truoc == null) {
            hienDangNhap("");
        } else {
            datNoiDung(truoc);
            if (hoiThoai != null) {
                hoiThoai.khiHienLai();
            }
        }
    }

    private void chuyenTaiKhoan(TaiKhoan taiKhoan) {
        if (dangChuyen) {
            return;
        }
        dangChuyen = true;
        if (manHinhTaiKhoan != null) {
            manHinhTaiKhoan.datTrangThai(true, "Đang kết nối tài khoản…");
        }
        KenhTCP moi = new KenhTCP();
        new TaiKhoanController(moi).khoiPhuc(taiKhoan)
                .whenComplete((phanHoi, sai) -> Platform.runLater(() -> {
                    dangChuyen = false;
                    if (daDung) {
                        moi.close();
                        return;
                    }
                    if (sai != null) {
                        moi.close();
                        Throwable goc = sai;
                        while (goc instanceof CompletionException && goc.getCause() != null) {
                            goc = goc.getCause();
                        }
                        if (goc instanceof KenhTCP.LoiPhanHoi loi && loi.ma().equals("phienKhongHopLe")) {
                            xoaDaLuu(taiKhoan);
                        }
                        String thongBao = DoHoaView.thongBao(sai);
                        if (dangMoTaiKhoan()) {
                            manHinhTaiKhoan.datTrangThai(false, thongBao);
                        } else {
                            hienDangNhap(thongBao);
                        }
                        return;
                    }
                    ghiNho(taiKhoan);
                    hienHoiThoai(moi, phanHoi.getAsJsonObject("nguoiDung"), taiKhoan);
                }));
    }

    private void dangXuat(TaiKhoan taiKhoan) {
        if (taiKhoan == null || dangChuyen) {
            return;
        }
        ThongBaoNoiView.xacNhan(cuaSo.getScene().getRoot(), "Đăng xuất tài khoản",
                "Bạn muốn đăng xuất tài khoản này? Bạn sẽ cần đăng nhập lại để sử dụng.", "Đăng xuất", () -> {
                    thucHienDangXuat(taiKhoan);
                    return CompletableFuture.completedFuture(null);
                });
    }

    private void thucHienDangXuat(TaiKhoan taiKhoan) {
        if (taiKhoan == null || dangChuyen) {
            return;
        }
        dangChuyen = true;
        boolean hienTai = dangDung != null && dangDung.khoa().equals(taiKhoan.khoa());
        if (manHinhTaiKhoan != null) {
            manHinhTaiKhoan.datTrangThai(true, "Đang đăng xuất…");
        }
        KenhTCP ketNoi = hienTai ? kenh : new KenhTCP();
        CompletableFuture<?> sanSang = hienTai ? CompletableFuture.completedFuture(null) :
                new TaiKhoanController(ketNoi).khoiPhuc(taiKhoan);
        sanSang.thenCompose(x -> new TaiKhoanController(ketNoi).dangXuat())
                .whenComplete((x, sai) -> Platform.runLater(() -> {
                    dangChuyen = false;
                    if (!hienTai) {
                        ketNoi.close();
                    }
                    if (daDung) {
                        return;
                    }
                    xoaDaLuu(taiKhoan);
                    if (hienTai) {
                        hienDangNhap(sai == null ? "Đã đăng xuất tài khoản này." :
                                "Đã đăng xuất trên máy này; máy chủ chưa xác nhận.");
                    } else {
                        taoTrangTaiKhoan();
                        if (sai != null) {
                            manHinhTaiKhoan.datTrangThai(false,
                                    "Đã đăng xuất trên máy này; máy chủ chưa xác nhận.");
                        }
                    }
                }));
    }

    private void datNoiDung(javafx.scene.Parent noiDung) {
        if (cuaSo.getScene() == null) {
            Scene canh = new Scene(noiDung, 1120, 760);
            canh.getStylesheets().add(getClass().getResource("/giao-dien.css").toExternalForm());
            cuaSo.setScene(canh);
        } else {
            cuaSo.getScene().setRoot(noiDung);
        }
    }

    @Override
    public void stop() {
        daDung = true;
        dongManHinh();
        try {
            viecLuu.get(18, TimeUnit.SECONDS);
        } catch (Exception loi) {/* Lỗi đã được hiển thị trong mục tài khoản. */}
    }
}
