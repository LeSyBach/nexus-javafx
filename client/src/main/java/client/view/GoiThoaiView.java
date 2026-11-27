package client.view;

import client.controller.GoiThoaiController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Locale;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/** Cửa sổ gọi thoại riêng, dùng lại cùng controller và phiên âm thanh. */
final class GoiThoaiView extends BorderPane {
    private final Node manHinhChat;
    private final Label ten = DoHoaView.nhan("", "tenNguoiGoi");
    private final Label trangThai = DoHoaView.nhan("", "trangThaiCuocGoi");
    private final Label thoiLuong = DoHoaView.nhan("00:00", "dongHoCuocGoi");
    private final StackPane anhDaiDien = new StackPane();
    private final Button batMay = nutGoi("Bắt máy", BieuTuongView.dienThoai(), "nutBatMay");
    private final Button mic = nutGoi("Tắt mic", BieuTuongView.micro(false), "nutMicGoi");
    private final Button ketThuc = nutGoi("Cúp máy", BieuTuongView.cupMay(), "nutCupMay");
    private final Button camera = nutGoi("Tắt camera", BieuTuongView.video(false), "nutMicGoi");
    private final Label nhanCamera = DoHoaView.nhan("Tắt camera", "nhanNutGoi");
    private final Label loiVideo = DoHoaView.nhan("", "loiVideo");
    private final VBox vungCamera = cumThaoTac(camera, nhanCamera);
    private final VBox giua = new VBox(14);
    private final ImageView anhDoiPhuong = new ImageView();
    private final ImageView anhCuaMinh = new ImageView();
    private final Label choAnhCuaMinh = DoHoaView.nhan("Camera chưa bật", "nhanNutGoi");
    private final VBox xemTruoc = new VBox(6);
    private final StackPane vungHinh = new StackPane();
    private final Label nhanMic = DoHoaView.nhan("Tắt mic", "nhanNutGoi");
    private final Label nhanKetThuc = DoHoaView.nhan("Cúp máy", "nhanNutGoi");
    private final VBox vungBatMay = cumThaoTac(batMay, DoHoaView.nhan("Bắt máy", "nhanNutGoi"));
    private final VBox vungMic = cumThaoTac(mic, nhanMic);
    private final VBox vungKetThuc = cumThaoTac(ketThuc, nhanKetThuc);
    private final Timeline dongHo = new Timeline(
            new KeyFrame(Duration.millis(250), e -> capNhatThoiLuong()));
    private GoiThoaiController dieuKhien;
    private Stage cuaSo;
    private boolean dangGoi;
    private String tenDaVe = "";
    private long lucBatDau;

    GoiThoaiView(Node manHinhChat) {
        this.manHinhChat = manHinhChat;
        setId("cuaSoGoiThoai");
        getStyleClass().add("cuaSoGoiThoai");
        Label thuongHieu = DoHoaView.nhan("nexus.", "thuongHieuCuocGoi");
        Region khoang = new Region();
        HBox.setHgrow(khoang, Priority.ALWAYS);
        HBox dau = new HBox(12, thuongHieu, khoang, thoiLuong);
        dau.setAlignment(Pos.CENTER_LEFT);
        dau.setPadding(new Insets(24, 28, 0, 28));
        setTop(dau);
        anhDaiDien.getStyleClass().add("vongAnhCuocGoi");
        anhDaiDien.setMinSize(164, 164);
        anhDaiDien.setPrefSize(164, 164);
        anhDaiDien.setMaxSize(164, 164);
        ten.setWrapText(true);
        ten.setMaxWidth(360);
        ten.setAlignment(Pos.CENTER);
        trangThai.setWrapText(true);
        trangThai.setMaxWidth(330);
        trangThai.setAlignment(Pos.CENTER);
        giua.getChildren().addAll(anhDaiDien, ten, trangThai);
        giua.setAlignment(Pos.CENTER);
        giua.setPadding(new Insets(32));
        VBox.setMargin(ten, new Insets(18, 0, 0, 0));
        anhDoiPhuong.setPreserveRatio(true);
        anhDoiPhuong.fitWidthProperty().bind(vungHinh.widthProperty().subtract(16));
        anhDoiPhuong.fitHeightProperty().bind(vungHinh.heightProperty().subtract(16));
        anhCuaMinh.setPreserveRatio(true);
        anhCuaMinh.setFitWidth(96);
        anhCuaMinh.setFitHeight(72);
        StackPane oXemTruoc = new StackPane(anhCuaMinh, choAnhCuaMinh);
        oXemTruoc.setMinSize(96, 72);
        xemTruoc.getChildren().addAll(oXemTruoc, DoHoaView.nhan("Bạn", "nhanNutGoi"));
        xemTruoc.setMaxSize(114, 108);
        xemTruoc.setAlignment(Pos.CENTER);
        xemTruoc.getStyleClass().add("xemTruocVideo");
        StackPane.setAlignment(xemTruoc, Pos.TOP_RIGHT);
        StackPane.setMargin(xemTruoc, new Insets(16));
        vungHinh.getChildren().addAll(anhDoiPhuong, giua, xemTruoc);
        setCenter(vungHinh);
        HBox thaoTac = new HBox(24, vungMic, vungCamera, vungKetThuc, vungBatMay);
        thaoTac.setAlignment(Pos.CENTER);
        thaoTac.getStyleClass().add("thaoTacCuocGoi");
        loiVideo.setWrapText(true);
        loiVideo.setMaxWidth(390);
        loiVideo.managedProperty().bind(loiVideo.visibleProperty());
        VBox chan = new VBox(8, loiVideo, thaoTac);
        chan.setAlignment(Pos.CENTER);
        setBottom(chan);
        thoiLuong.setId("thoiLuongGoiThoai");
        thoiLuong.managedProperty().bind(thoiLuong.visibleProperty());
        thoiLuong.setVisible(false);
        dongHo.setCycleCount(Animation.INDEFINITE);
    }

    void ganHanhDong(GoiThoaiController dieuKhien) {
        this.dieuKhien = dieuKhien;
        batMay.setOnAction(e -> dieuKhien.batMay());
        mic.setOnAction(e -> dieuKhien.doiMic());
        ketThuc.setOnAction(e -> dieuKhien.ketThuc());
        camera.setOnAction(e -> dieuKhien.doiCamera());
        dieuKhien.khiCoHinh(this::capNhatHinh);
    }

    void capNhat(GoiThoaiController.HienThi duLieu) {
        boolean cuocGoiMoi = !dangGoi && duLieu.trangThai() != GoiThoaiController.TrangThai.ranh;
        dangGoi = duLieu.trangThai() != GoiThoaiController.TrangThai.ranh;
        if (!dangGoi || duLieu.thongBao().isEmpty()) {
            xoaHinh();
            hienDongHo(false);
            if (cuaSo != null) {
                cuaSo.close();
            }
            return;
        }
        taoCuaSo();
        if (cuocGoiMoi) {
            xoaHinh();
        }
        veNguoiGoi(duLieu.tenDoiPhuong());
        loiVideo.setText(duLieu.thongBaoVideo());
        loiVideo.setVisible(!duLieu.thongBaoVideo().isEmpty() && dangGoi);
        xemTruoc.setVisible(duLieu.goiVideo() && dangGoi && anhCuaMinh.getImage() != null);
        vungCamera.setVisible(duLieu.goiVideo() && dangGoi);
        camera.setGraphic(BieuTuongView.video(duLieu.tatCamera()));
        camera.pseudoClassStateChanged(PseudoClass.getPseudoClass("micTat"), duLieu.tatCamera());
        String chuCamera = duLieu.tatCamera() ? "Bật camera" : "Tắt camera";
        nhanCamera.setText(chuCamera);
        camera.setAccessibleText(chuCamera);
        camera.setTooltip(new Tooltip(chuCamera));
        boolean cuocGoiDen = duLieu.trangThai() == GoiThoaiController.TrangThai.cuocGoiDen;
        boolean dangNoiChuyen = duLieu.trangThai() == GoiThoaiController.TrangThai.dangNoiChuyen;
        trangThai.setText(switch (duLieu.trangThai()) {
            case dangGoi -> "Đang đổ chuông…";
            case cuocGoiDen -> duLieu.goiVideo() ? "Cuộc gọi video đến" : "Cuộc gọi thoại đến";
            case dangMoAmThanh -> "Đang kết nối âm thanh…";
            case dangNoiChuyen -> "Đang trò chuyện";
            case ranh -> duLieu.thongBao();
        });
        vungBatMay.setVisible(cuocGoiDen);
        vungMic.setVisible(dangGoi && !cuocGoiDen);
        mic.setDisable(!dangNoiChuyen);
        mic.setGraphic(BieuTuongView.micro(duLieu.tatMic()));
        mic.pseudoClassStateChanged(PseudoClass.getPseudoClass("micTat"), duLieu.tatMic());
        String chuMic = duLieu.tatMic() ? "Bật mic" : "Tắt mic";
        nhanMic.setText(chuMic);
        mic.setAccessibleText(chuMic);
        mic.setTooltip(new Tooltip(chuMic));
        String chuKetThuc = cuocGoiDen ? "Từ chối"
                : duLieu.trangThai() == GoiThoaiController.TrangThai.dangGoi ? "Hủy gọi" : "Cúp máy";
        nhanKetThuc.setText(chuKetThuc);
        ketThuc.setAccessibleText(chuKetThuc);
        ketThuc.setTooltip(new Tooltip(chuKetThuc));
        vungKetThuc.setVisible(dangGoi);
        hienDongHo(dangNoiChuyen);
        if (cuocGoiMoi) {
            hienLai();
        } else if (!cuaSo.isShowing()) {
            cuaSo.show();
        }
    }

    void hienLai() {
        if (dangGoi && cuaSo != null) {
            cuaSo.show();
            cuaSo.setIconified(false);
            cuaSo.toFront();
            cuaSo.requestFocus();
        }
    }

    private void capNhatHinh(GoiThoaiController.HinhAnh hinh) {
        if (!dangGoi) {
            return;
        }
        Image anh = null;
        if (hinh.jpeg().length > 0) {
            anh = new Image(new ByteArrayInputStream(hinh.jpeg()), 640, 480, true, false);
            if (anh.isError()) {
                return;
            }
        }
        if (hinh.cuaMinh()) {
            anhCuaMinh.setImage(anh);
            xemTruoc.setVisible(anh != null);
            choAnhCuaMinh.setVisible(anh == null);
            choAnhCuaMinh.setText(anh == null ? "Camera chưa bật" : "");
        } else {
            anhDoiPhuong.setImage(anh);
            giua.setVisible(anh == null);
        }
    }

    private void xoaHinh() {
        anhDoiPhuong.setImage(null);
        anhCuaMinh.setImage(null);
        xemTruoc.setVisible(false);
        giua.setVisible(true);
        choAnhCuaMinh.setVisible(true);
    }

    private void taoCuaSo() {
        if (cuaSo != null) {
            return;
        }
        cuaSo = new Stage();
        if (manHinhChat.getScene() != null && manHinhChat.getScene().getWindow() != null) {
            cuaSo.initOwner(manHinhChat.getScene().getWindow());
        }
        Scene canh = new Scene(this, 440, 600);
        canh.getStylesheets().add(GoiThoaiView.class.getResource("/giao-dien.css").toExternalForm());
        cuaSo.setScene(canh);
        cuaSo.setMinWidth(360);
        cuaSo.setMinHeight(520);
        cuaSo.setOnCloseRequest(e -> {
            if (dangGoi) {
                dieuKhien.ketThuc();
            }
        });
    }

    private void veNguoiGoi(String tenDoiPhuong) {
        String tenHienThi = tenDoiPhuong.isBlank() ? "Cuộc gọi thoại" : tenDoiPhuong;
        cuaSo.setTitle("NEXUS Call · " + tenHienThi);
        ten.setText(tenHienThi);
        if (!tenDaVe.equals(tenHienThi)) {
            StackPane anh = DoHoaView.anh(tenHienThi, tenHienThi, false);
            anh.getStyleClass().setAll("anhCuocGoi");
            anhDaiDien.getChildren().setAll(anh);
            tenDaVe = tenHienThi;
        }
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

    private static Button nutGoi(String nhan, Node bieuTuong, String lop) {
        Button nut = DoHoaView.nut("", "nutTronCuocGoi");
        nut.getStyleClass().add(lop);
        nut.setGraphic(bieuTuong);
        nut.setTooltip(new Tooltip(nhan));
        nut.setAccessibleText(nhan);
        return nut;
    }

    private static VBox cumThaoTac(Button nut, Label nhan) {
        VBox cum = new VBox(10, nut, nhan);
        cum.setAlignment(Pos.CENTER);
        cum.managedProperty().bind(cum.visibleProperty());
        return cum;
    }
}
