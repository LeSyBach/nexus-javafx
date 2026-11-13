package client.view;

import client.controller.GoiThoaiController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Thanh trạng thái cuộc gọi, hiển thị cả khi đổi sang màn hình khác. */
final class GoiThoaiView extends HBox {
    private final Label trangThai = DoHoaView.nhan("", "tenHoiThoai");
    private final Button batMay = DoHoaView.nut("Bắt máy", "nutHoSoChinh");
    private final Button mic = DoHoaView.nut("Tắt mic", "nutHoSoPhu");
    private final Button ketThuc = DoHoaView.nut("Cúp máy", "nutThaoTacNguyHiem");
    private final Button dong = DoHoaView.nut("Đóng", "nutHoSoPhu");

    GoiThoaiView() {
        setId("thanhGoiThoai");
        getStyleClass().add("thanhGhimTin");
        setPadding(new Insets(8, 16, 8, 16));
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        trangThai.setWrapText(true);
        trangThai.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(trangThai, Priority.ALWAYS);
        managedProperty().bind(visibleProperty());
        for (Button nut : new Button[]{batMay, mic, ketThuc, dong}) {
            nut.setMinHeight(36);
            nut.setMinWidth(USE_PREF_SIZE);
            nut.managedProperty().bind(nut.visibleProperty());
        }
        getChildren().addAll(trangThai, batMay, mic, ketThuc, dong);
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
        trangThai.setText(duLieu.thongBao());
        batMay.setVisible(cuocGoiDen);
        mic.setVisible(buoc == GoiThoaiController.TrangThai.dangNoiChuyen);
        mic.setText(duLieu.tatMic() ? "Bật mic" : "Tắt mic");
        ketThuc.setVisible(!ranh);
        ketThuc.setText(cuocGoiDen ? "Từ chối"
                : buoc == GoiThoaiController.TrangThai.dangGoi ? "Hủy gọi" : "Cúp máy");
        dong.setVisible(ranh);
        setVisible(!duLieu.thongBao().isEmpty());
    }
}
