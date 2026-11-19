package client.view;

import client.controller.GoiThoaiController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Thanh trạng thái cuộc gọi, hiển thị cả khi đổi sang màn hình khác. */
final class GoiThoaiView extends HBox {
    private final Label trangThai = DoHoaView.nhan("", "tenHoiThoai");
    private final Label thoiLuong = DoHoaView.nhan("", "tenHoiThoai");
    private final Button batMay = DoHoaView.nut("Bắt máy", "nutHoSoChinh");
    private final Button mic = DoHoaView.nut("Tắt mic", "nutHoSoPhu");
    private final Button ketThuc = DoHoaView.nut("Cúp máy", "nutThaoTacNguyHiem");
    private final Button dong = DoHoaView.nut("Đóng", "nutHoSoPhu");
    private final Timeline dongHo = new Timeline(
            new KeyFrame(Duration.millis(250), e -> capNhatThoiLuong()));
    private long lucBatDau;

    GoiThoaiView() {
        setId("thanhGoiThoai");
        getStyleClass().add("thanhGhimTin");
        setPadding(new Insets(8, 16, 8, 16));
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        trangThai.setWrapText(true);
        trangThai.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(trangThai, Priority.ALWAYS);
        thoiLuong.setId("thoiLuongGoiThoai");
        thoiLuong.setMinWidth(USE_PREF_SIZE);
        thoiLuong.managedProperty().bind(thoiLuong.visibleProperty());
        thoiLuong.setVisible(false);
        dongHo.setCycleCount(Animation.INDEFINITE);
        managedProperty().bind(visibleProperty());
        for (Button nut : new Button[]{batMay, mic, ketThuc, dong}) {
            nut.setMinHeight(36);
            nut.setMinWidth(USE_PREF_SIZE);
            nut.managedProperty().bind(nut.visibleProperty());
        }
        getChildren().addAll(trangThai, thoiLuong, batMay, mic, ketThuc, dong);
        dong.setOnAction(e -> setVisible(false));
        setVisible(false);
    }

    void ganHanhDong(GoiThoaiController dieuKhien) {
        batMay.setOnAction(e -> dieuKhien.batMay());
        mic.setOnAction(e -> dieuKhien.doiMic());
        ketThuc.setOnAction(e -> dieuKhien.ketThuc());
    }

    void capNhat(GoiThoaiController.HienThi duLieu) {
        var buoc = duLieu.trangThai();
        boolean ranh = buoc == GoiThoaiController.TrangThai.ranh;
        boolean cuocGoiDen = buoc == GoiThoaiController.TrangThai.cuocGoiDen;
        boolean dangNoiChuyen = buoc == GoiThoaiController.TrangThai.dangNoiChuyen;
        trangThai.setText(duLieu.thongBao());
        batMay.setVisible(cuocGoiDen);
        mic.setVisible(dangNoiChuyen);
        mic.setText(duLieu.tatMic() ? "Bật mic" : "Tắt mic");
        ketThuc.setVisible(!ranh);
        ketThuc.setText(cuocGoiDen ? "Từ chối"
                : buoc == GoiThoaiController.TrangThai.dangGoi ? "Hủy gọi" : "Cúp máy");
        dong.setVisible(ranh);
        hienDongHo(dangNoiChuyen);
        setVisible(!duLieu.thongBao().isEmpty());
    }

    private void hienDongHo(boolean dangNoiChuyen) {
        if (!dangNoiChuyen) {
            dongHo.stop();
            thoiLuong.setVisible(false);
        } else if (dongHo.getStatus() != Animation.Status.RUNNING) {
            lucBatDau = System.nanoTime();
            capNhatThoiLuong();
            thoiLuong.setVisible(true);
            dongHo.playFromStart();
        }
    }

    private void capNhatThoiLuong() {
        long tongGiay = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lucBatDau);
        long gio = tongGiay / 3600;
        long phut = tongGiay / 60 % 60;
        long giay = tongGiay % 60;
        String moc = gio == 0
                ? String.format(Locale.ROOT, "%02d:%02d", phut, giay)
                : String.format(Locale.ROOT, "%d:%02d:%02d", gio, phut, giay);
        thoiLuong.setText(moc);
        thoiLuong.setAccessibleText("Thời lượng cuộc gọi " + moc);
    }
}
