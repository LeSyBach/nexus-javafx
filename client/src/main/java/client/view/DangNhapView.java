package client.view;

import client.controller.DangNhapController;
import client.controller.DangNhapController.KetQuaXacThuc;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public final class DangNhapView extends HBox {
    private final TextField email = new TextField();
    private final PasswordField matKhau = new PasswordField();
    private final TextField ten = new TextField();
    private final PasswordField xacNhan = new PasswordField();
    private final TextField diaChi = new TextField("127.0.0.1");
    private final TextField cong = new TextField("4040");
    private final Label thongBao = DoHoaView.nhan("", "thongBao");
    private final Label tieuDe = DoHoaView.nhan("Chào mừng\ntrở lại.", "tieuDeDangNhap");
    private final Button gui = DoHoaView.nut("Đăng nhập   →", "nutChinh");
    private final VBox cacTruong = new VBox(10);
    private boolean laDangKy;
    private final DangNhapController dieuKhien;
    private boolean daHuy;

    public DangNhapView(DangNhapController dieuKhien, Consumer<KetQuaXacThuc> khiThanhCong, String loiBanDau) {
        this.dieuKhien = dieuKhien;
        getStyleClass().add("trangDangNhap");
        getChildren().addAll(taoCotMinhHoa(), taoCotBieuMau());

        gui.setMaxWidth(Double.MAX_VALUE);
        gui.setDefaultButton(true);
        gui.setOnAction(suKien -> thucHien(khiThanhCong));

        doiCheDo(false);
        thongBao.setText(loiBanDau);
        thongBao.setWrapText(true);
    }

    private VBox taoCotMinhHoa() {
        VBox minhHoa = new VBox(22);
        minhHoa.getStyleClass().add("cotMinhHoa");
        minhHoa.setPrefWidth(430);

        Label thuongHieu = DoHoaView.nhan("nexus.", "thuongHieuSang");
        Region khoangTren = new Region();
        VBox.setVgrow(khoangTren, Priority.ALWAYS);
        Label nhanGioiThieu = DoHoaView.nhan("MỘT KHÔNG GIAN. NHIỀU KẾT NỐI.", "nhanSang");
        Label tieuDeMinhHoa = DoHoaView.nhan("Chậm một chút.\nGần nhau hơn.", "chuMinhHoa");
        Label moTa = DoHoaView.nhan("Những câu chuyện nhỏ\nlàm nên những kết nối lớn.", "moTaSang");
        minhHoa.getChildren().addAll(thuongHieu, khoangTren, nhanGioiThieu, tieuDeMinhHoa, moTa);

        Region khoangDuoi = new Region();
        VBox.setVgrow(khoangDuoi, Priority.ALWAYS);
        minhHoa.getChildren()
                .addAll(khoangDuoi, DoHoaView.nhan("NEXUS · Ứng dụng trò chuyện", "chanMinhHoa"));
        return minhHoa;
    }

    private VBox taoCotBieuMau() {
        VBox cotForm = new VBox(16);
        cotForm.getStyleClass().add("cotBieuMau");
        HBox.setHgrow(cotForm, Priority.ALWAYS);
        cotForm.setAlignment(Pos.CENTER);
        VBox bieuMau = new VBox(13);
        bieuMau.setMaxWidth(360);

        HBox chon = new HBox(8);
        Button dangNhap = DoHoaView.nut("Đăng nhập", "nutTheDangNhap");
        Button dangKy = DoHoaView.nut("Tạo tài khoản", "nutTheDangNhap");
        chon.getChildren().addAll(dangNhap, dangKy);
        dangNhap.setOnAction(suKien -> doiCheDo(false));
        dangKy.setOnAction(suKien -> doiCheDo(true));

        email.setPromptText("ban@example.com");
        matKhau.setPromptText("Nhập mật khẩu");
        ten.setPromptText("Tên bạn muốn hiển thị");
        xacNhan.setPromptText("Nhập lại mật khẩu");

        Label nhanBieuMau = DoHoaView.nhan("KẾT NỐI BẮT ĐẦU TỪ ĐÂY", "nhanNho");
        bieuMau.getChildren().addAll(nhanBieuMau, tieuDe, chon, cacTruong, gui, thongBao);
        bieuMau.getChildren().add(taoCauHinhKetNoi());
        cotForm.getChildren().add(bieuMau);

        return cotForm;
    }

    private TitledPane taoCauHinhKetNoi() {
        TitledPane cauHinh = new TitledPane();
        cauHinh.setText("Máy chủ kết nối");
        cauHinh.setExpanded(false);
        cauHinh.getStyleClass().add("cauHinhKetNoi");

        HBox diaChiCong = new HBox(8, diaChi, cong);
        HBox.setHgrow(diaChi, Priority.ALWAYS);
        cong.setPrefWidth(85);
        cauHinh.setContent(diaChiCong);
        return cauHinh;
    }

    private void doiCheDo(boolean dangKy) {
        laDangKy = dangKy;
        thongBao.setText("");
        cacTruong.getChildren().clear();
        tieuDe.setText(dangKy ? "Bắt đầu một\nkết nối mới." : "Chào mừng\ntrở lại.");
        if (dangKy) {
            cacTruong.getChildren().addAll(DoHoaView.nhan("Tên hiển thị", "nhanTruong"), ten);
        }
        cacTruong.getChildren().addAll(DoHoaView.nhan("Địa chỉ email", "nhanTruong"), email,
                DoHoaView.nhan("Mật khẩu", "nhanTruong"), matKhau);
        if (dangKy) {
            cacTruong.getChildren().addAll(DoHoaView.nhan("Xác nhận mật khẩu", "nhanTruong"), xacNhan);
        }
        gui.setText(dangKy ? "Tạo tài khoản   →" : "Đăng nhập   →");
    }

    private void thucHien(Consumer<KetQuaXacThuc> khiThanhCong) {
        if (gui.isDisabled()) {
            return;
        }

        boolean dangKy = laDangKy;
        gui.setDisable(true);
        cacTruong.setDisable(true);
        thongBao.setText("Đang kết nối…");

        dieuKhien.xacThuc(diaChi.getText(), cong.getText(), dangKy, email.getText(), matKhau.getText(),
                        xacNhan.getText(), ten.getText())
                .whenComplete((ketQua, loi) -> {
                    if (daHuy) {
                        return;
                    }
                    gui.setDisable(false);
                    cacTruong.setDisable(false);
                    if (loi != null) {
                        thongBao.setText(DoHoaView.thongBao(loi));
                        return;
                    }

                    matKhau.clear();
                    xacNhan.clear();
                    if (dangKy) {
                        doiCheDo(false);
                        thongBao.setText("Tạo tài khoản thành công. Bạn đăng nhập nhé.");
                    } else {
                        khiThanhCong.accept(ketQua);
                    }
                });
    }

    public void dong() {
        daHuy = true;
        dieuKhien.dong();
    }
}
