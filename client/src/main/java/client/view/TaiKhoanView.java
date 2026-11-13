package client.view;

import client.model.TaiKhoanModel;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import java.util.*;
import java.util.function.Consumer;

public final class TaiKhoanView extends BorderPane {
    private final VBox cacDong = new VBox(12);
    private final Label thongBao = DoHoaView.nhan("", "thongBaoHoSo");
    private final Button quayLai = DoHoaView.nut("← Quay lại", "nutHoSoPhu");
    private final Button them = DoHoaView.nut("Thêm tài khoản", "nutHoSoChinh");

    public TaiKhoanView(List<TaiKhoanModel.TaiKhoan> cacTaiKhoan, String khoaHienTai,
                        Consumer<TaiKhoanModel.TaiKhoan> khiChon,
                        Consumer<TaiKhoanModel.TaiKhoan> khiDangXuat, Runnable khiThem, Runnable khiQuayLai) {
        setId("manHinhTaiKhoan");
        getStyleClass().add("trangHoSo");
        quayLai.setOnAction(suKien -> khiQuayLai.run());
        HBox dau = new HBox(quayLai);
        dau.getStyleClass().add("dauTrangHoSo");
        setTop(dau);
        VBox noiDung = new VBox(16);
        noiDung.getStyleClass().add("hoSoCaNhan");
        noiDung.setMaxWidth(740);
        Label moTa = DoHoaView.nhan(
                "Chuyển tài khoản không cần nhập lại mật khẩu. Đăng xuất chỉ xóa phiên của tài khoản được chọn trên máy này.",
                "giaTriHoSo");
        moTa.setWrapText(true);
        moTa.setMinHeight(Region.USE_PREF_SIZE);
        for (var taiKhoan : cacTaiKhoan) {
            boolean hienTai = taiKhoan.khoa().equals(khoaHienTai);
            Label ten = DoHoaView.nhan(taiKhoan.ten() + (hienTai ? " · Đang dùng" : ""), "tenBanBe");
            ten.setMaxWidth(260);
            Label email = DoHoaView.nhan(taiKhoan.email(), "giaTriHoSo");
            email.setMaxWidth(260);
            Label mayChu = DoHoaView.nhan(taiKhoan.mayChu() + ":" + taiKhoan.cong(), "goiYHoSo");
            VBox chu = new VBox(5, ten, email, mayChu);
            chu.setMinWidth(0);
            HBox.setHgrow(chu, Priority.ALWAYS);
            Button chuyen = DoHoaView.nut("Chuyển", "nutHoSoChinh");
            chuyen.setDisable(hienTai);
            chuyen.setOnAction(suKien -> khiChon.accept(taiKhoan));
            Button thoat = DoHoaView.nut("Đăng xuất", "nutHoSoPhu");
            thoat.setOnAction(suKien -> khiDangXuat.accept(taiKhoan));
            HBox hang = new HBox(10, chu, chuyen, thoat);
            hang.setAlignment(Pos.CENTER_LEFT);
            hang.getStyleClass().add("dongBanBe");
            cacDong.getChildren().add(hang);
        }
        if (cacTaiKhoan.isEmpty()) {
            cacDong.getChildren().add(DoHoaView.nhan("Chưa có tài khoản đã lưu.", "giaTriHoSo"));
        }

        ScrollPane cuon = new ScrollPane(cacDong);
        cuon.setFitToWidth(true);
        cuon.setMinHeight(180);
        VBox.setVgrow(cuon, Priority.ALWAYS);
        cuon.getStyleClass().add("cuonBanBe");

        them.setOnAction(suKien -> khiThem.run());
        thongBao.setWrapText(true);

        noiDung.getChildren()
                .addAll(DoHoaView.nhan("Tài khoản của bạn", "tenHoSo"), moTa, cuon, them, thongBao);

        StackPane khung = new StackPane(noiDung);
        khung.setAlignment(Pos.TOP_CENTER);
        khung.setPadding(new javafx.geometry.Insets(24));
        setCenter(khung);
    }

    public void datTrangThai(boolean dangXuLy, String chu) {
        cacDong.setDisable(dangXuLy);
        quayLai.setDisable(dangXuLy);
        them.setDisable(dangXuLy);
        thongBao.setText(chu);
    }
}
