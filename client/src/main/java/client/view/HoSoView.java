package client.view;

import client.controller.TaiKhoanController;
import client.hatang.KenhTCP;
import client.hatang.XuLyAnh;

import com.google.gson.*;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.util.function.Consumer;

/**
 * Chỉ mở biểu mẫu sau khi người dùng chọn chỉnh sửa.
 */
public final class HoSoView extends BorderPane {
    private final TaiKhoanController dieuKhien;
    private final Consumer<JsonObject> khiCapNhat;
    private final Runnable khiChuyenTaiKhoan;
    private final VBox noiDung = new VBox(15);
    private final Label thongBao = DoHoaView.nhan("", "thongBaoHoSo");
    private JsonObject hoSo;
    private String cheDo = "xem", anhDangSua = "", phienBanSua = "0";
    private boolean dangGui, daDong, dangXuLyAnh;
    private int lanChonAnh;
    private final TextField ten = new TextField(), dienThoai = new TextField();
    private final TextArea gioiThieu = new TextArea();
    private final StackPane anhSua = new StackPane();
    private final PasswordField matKhauCu = new PasswordField(), matKhauMoi = new PasswordField(), xacNhan =
            new PasswordField();

    public HoSoView(KenhTCP kenh, JsonObject hoSo, Consumer<JsonObject> khiCapNhat,
                    Runnable khiChuyenTaiKhoan, Runnable khiQuayLai) {
        dieuKhien = new TaiKhoanController(kenh);
        this.hoSo = hoSo.deepCopy();
        this.khiCapNhat = khiCapNhat;
        this.khiChuyenTaiKhoan = khiChuyenTaiKhoan;
        setId("manHinhHoSo");
        getStyleClass().add("trangHoSo");
        Button quayLai = DoHoaView.nut("← Quay về trò chuyện", "nutHoSoPhu");
        quayLai.setOnAction(suKien -> {
            if (!dangGui) {
                khiQuayLai.run();
            }
        });
        HBox dauTrang = new HBox(quayLai);
        dauTrang.getStyleClass().add("dauTrangHoSo");
        setTop(dauTrang);
        noiDung.getStyleClass().add("hoSoCaNhan");
        noiDung.setMaxWidth(580);
        noiDung.setMinHeight(Region.USE_PREF_SIZE);
        StackPane khung = new StackPane(noiDung);
        khung.setAlignment(Pos.TOP_CENTER);
        khung.setPadding(new Insets(20, 24, 32, 24));
        ScrollPane cuon = new ScrollPane(khung);
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("cuonHoSo");
        setCenter(cuon);
        thongBao.setWrapText(true);
        thongBao.setMinHeight(26);
        thongBao.setMaxWidth(Double.MAX_VALUE);
        ten.setId("tenHoSo");
        dienThoai.setId("dienThoaiHoSo");
        gioiThieu.setId("gioiThieuHoSo");
        matKhauCu.setId("matKhauCu");
        matKhauMoi.setId("matKhauMoi");
        xacNhan.setId("xacNhanMatKhau");
        veXem();
        taiHoSo();
    }

    private void taiHoSo() {
        dieuKhien.hoSo().whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            if (sai != null) {
                thongBao.setText(DoHoaView.thongBao(sai));
                return;
            }
            capNhat(duLieu.getAsJsonObject("hoSo"));
            khiCapNhat.accept(hoSo);
        }));
    }

    public void capNhat(JsonObject moi) {
        hoSo = moi.deepCopy();
        if (cheDo.equals("xem")) {
            veXem();
        }
    }

    private void veXem() {
        cheDo = "xem";
        lanChonAnh++;
        dangXuLyAnh = false;
        xoaMatKhau();
        noiDung.getChildren().clear();
        Label emailHoSo = DoHoaView.nhan(DuLieu.chu(hoSo, "email"), "emailHoSo");
        emailHoSo.setWrapText(true);
        emailHoSo.setMaxWidth(Double.MAX_VALUE);
        emailHoSo.setAlignment(Pos.CENTER);
        VBox dau = new VBox(12, DoHoaView.anhHoSo(hoSo, 86),
                DoHoaView.nhan(DuLieu.chu(hoSo, "tenHienThi"), "tenHoSo"), emailHoSo);
        dau.setAlignment(Pos.CENTER);
        dau.setMinHeight(Region.USE_PREF_SIZE);
        Button sua = DoHoaView.nut("Chỉnh sửa hồ sơ", "nutHoSoChinh");
        sua.setMaxWidth(Double.MAX_VALUE);
        sua.setOnAction(suKien -> veSua());
        Button doi = DoHoaView.nut("Đổi mật khẩu", "nutHoSoPhu");
        doi.setMaxWidth(Double.MAX_VALUE);
        doi.setOnAction(suKien -> veDoiMatKhau());
        Button riengTu = DoHoaView.nut("Quyền riêng tư", "nutHoSoPhu");
        riengTu.setMaxWidth(Double.MAX_VALUE);
        riengTu.setOnAction(e -> veQuyenRiengTu());
        Button chuyen = DoHoaView.nut("Chuyển / thêm tài khoản", "nutHoSoPhu");
        chuyen.setMaxWidth(Double.MAX_VALUE);
        chuyen.setOnAction(suKien -> khiChuyenTaiKhoan.run());
        noiDung.getChildren().addAll(DoHoaView.nhan("HỒ SƠ CÁ NHÂN", "nhanHoSo"), dau, sua,
                muc("Số điện thoại", DuLieu.chu(hoSo, "soDienThoai")),
                muc("Giới thiệu", DuLieu.chu(hoSo, "gioiThieu")),
                muc("Ngày tham gia", DuLieu.chu(hoSo, "thamGia")), new Separator(), riengTu, doi, chuyen,
                thongBao);
    }

    private VBox muc(String ten, String giaTri) {
        Label noiDungMuc = DoHoaView.nhan(giaTri.isBlank() ? "Chưa cập nhật" : giaTri, "giaTriHoSo");
        noiDungMuc.setWrapText(true);
        noiDungMuc.setMaxWidth(Double.MAX_VALUE);
        return new VBox(5, DoHoaView.nhan(ten, "nhanHoSo"), noiDungMuc);
    }

    private void veSua() {
        cheDo = "sua";
        thongBao.setText("");
        lanChonAnh++;
        ten.setText(DuLieu.chu(hoSo, "tenHienThi"));
        dienThoai.setText(DuLieu.chu(hoSo, "soDienThoai"));
        gioiThieu.setText(DuLieu.chu(hoSo, "gioiThieu"));
        anhDangSua = DuLieu.chu(hoSo, "anhDaiDien");
        phienBanSua = DuLieu.chu(hoSo, "phienBan");
        gioiThieu.setWrapText(true);
        gioiThieu.setPrefRowCount(3);
        gioiThieu.setPromptText("Một vài dòng về bạn (tối đa 240 ký tự)");
        dienThoai.setPromptText("Ví dụ: 0912345678");
        veAnhSua();
        Button chon = DoHoaView.nut("Chọn ảnh", "nutHoSoPhu"), xoa =
                DoHoaView.nut("Xóa ảnh", "nutHoSoPhu");
        chon.setOnAction(suKien -> chonAnh());
        xoa.setOnAction(suKien -> {
            anhDangSua = "";
            lanChonAnh++;
            dangXuLyAnh = false;
            veAnhSua();
        });
        Label ghiChu = DoHoaView.nhan("Ảnh PNG/JPEG tối đa 10 MB, tự cắt vuông ở giữa.", "goiYHoSo");
        ghiChu.setWrapText(true);
        Button luu = DoHoaView.nut("Lưu thay đổi", "nutHoSoChinh"), huy =
                DoHoaView.nut("Hủy", "nutHoSoPhu");
        luu.setOnAction(suKien -> luuHoSo());
        huy.setOnAction(suKien -> {
            thongBao.setText("");
            veXem();
        });
        noiDung.getChildren()
                .setAll(DoHoaView.nhan("Chỉnh sửa hồ sơ", "tenHoSo"), anhSua, new HBox(10, chon, xoa),
                        ghiChu,
                        DoHoaView.nhan("Tên hiển thị", "nhanHoSo"), ten,
                        DoHoaView.nhan("Số điện thoại", "nhanHoSo"), dienThoai,
                        DoHoaView.nhan("Giới thiệu", "nhanHoSo"), gioiThieu, new HBox(10, luu, huy),
                        thongBao);
    }

    private void veAnhSua() {
        JsonObject duLieu = DuLieu.tao("tenHienThi", ten.getText().isBlank() ? "N" : ten.getText(), "id",
                DuLieu.chu(hoSo, "id"), "anhDaiDien", anhDangSua);
        anhSua.getChildren().setAll(DoHoaView.anhHoSo(duLieu, 86));
    }

    private void chonAnh() {
        FileChooser chon = new FileChooser();
        chon.setTitle("Chọn ảnh đại diện");
        chon.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Ảnh PNG/JPEG", "*.png", "*.jpg", "*.jpeg"));
        var tep = chon.showOpenDialog(getScene().getWindow());
        if (tep == null) {
            return;
        }
        int lan = ++lanChonAnh;
        dangXuLyAnh = true;
        thongBao.setText("Đang xử lý ảnh…");
        XuLyAnh.docTrongNen(tep.toPath())
                .whenComplete((anh, sai) -> Platform.runLater(() -> {
                    if (daDong || lan != lanChonAnh || !cheDo.equals("sua")) {
                        return;
                    }
                    dangXuLyAnh = false;
                    if (sai != null) {
                        thongBao.setText(DoHoaView.thongBao(sai));
                    } else {
                        anhDangSua = anh;
                        veAnhSua();
                        thongBao.setText("Ảnh đã sẵn sàng. Bấm Lưu thay đổi để áp dụng.");
                    }
                }));
    }

    private void luuHoSo() {
        if (dangGui) {
            return;
        }
        if (dangXuLyAnh) {
            thongBao.setText("Ảnh đang được xử lý. Bạn chờ một chút nhé.");
            return;
        }
        datDangGui(true);
        dieuKhien.luuHoSo(ten.getText(), dienThoai.getText(), gioiThieu.getText(), anhDangSua, phienBanSua)
                .whenComplete((phanHoi, sai) -> Platform.runLater(() -> {
                    if (daDong) {
                        return;
                    }
                    datDangGui(false);
                    if (sai != null) {
                        thongBao.setText(DoHoaView.thongBao(sai));
                        return;
                    }
                    hoSo = phanHoi.getAsJsonObject("hoSo");
                    khiCapNhat.accept(hoSo);
                    veXem();
                    thongBao.setText("Đã lưu hồ sơ.");
                }));
    }

    private void veQuyenRiengTu() {
        cheDo = "riengTu";
        thongBao.setText("");
        VBox email = taoLuaChonQuyen("quyenEmail", "Ai được xem email?", DuLieu.chu(hoSo, "quyenEmail"));
        VBox dienThoai = taoLuaChonQuyen("quyenDienThoai", "Ai được xem số điện thoại?",
                DuLieu.chu(hoSo, "quyenDienThoai"));
        Button luu = DoHoaView.nut("Lưu quyền riêng tư", "nutHoSoChinh"), huy =
                DoHoaView.nut("Hủy", "nutHoSoPhu");
        huy.setOnAction(e -> veXem());
        luu.setOnAction(e -> {
            if (dangGui) {
                return;
            }
            datDangGui(true);
            String quyenEmail =
                    ((ToggleGroup) email.getUserData()).getSelectedToggle().getUserData().toString();
            String quyenDienThoai =
                    ((ToggleGroup) dienThoai.getUserData()).getSelectedToggle().getUserData().toString();
            dieuKhien.luuQuyen(quyenEmail, quyenDienThoai)
                    .whenComplete((tra, sai) -> Platform.runLater(() -> {
                        if (daDong) {
                            return;
                        }
                        datDangGui(false);
                        if (sai != null) {
                            thongBao.setText(DoHoaView.thongBao(sai));
                            return;
                        }
                        hoSo = tra.getAsJsonObject("hoSo");
                        khiCapNhat.accept(hoSo);
                        veXem();
                        thongBao.setText("Đã lưu quyền riêng tư.");
                    }));
        });
        Label ghiChu = DoHoaView.nhan(
                "Chọn ai được xem từng thông tin trên trang cá nhân của bạn. Thay đổi áp dụng cho các lần xem tiếp theo.",
                "goiYHoSo");
        ghiChu.setWrapText(true);
        noiDung.getChildren().setAll(DoHoaView.nhan("Quyền riêng tư", "tenHoSo"), ghiChu, email, dienThoai,
                new HBox(10, luu, huy), thongBao);
    }

    private VBox taoLuaChonQuyen(String ma, String tieuDe, String giaTri) {
        ToggleGroup nhom = new ToggleGroup();
        HBox cacNut = new HBox(8);
        String[] cacMa = {"moiNguoi", "banBe", "chiMinhToi"};
        String[] cacTen = {"Mọi người", "Bạn bè", "Chỉ mình tôi"};
        for (int i = 0; i < cacMa.length; i++) {
            RadioButton nut = new RadioButton(cacTen[i]);
            nut.setToggleGroup(nhom);
            nut.setUserData(cacMa[i]);
            String maQuyen = cacMa[i];
            nut.setId(ma + Character.toUpperCase(maQuyen.charAt(0)) + maQuyen.substring(1));
            nut.setAccessibleText(tieuDe + " " + cacTen[i]);
            nut.getStyleClass().add("luaChonQuyen");
            nut.setMaxWidth(Double.MAX_VALUE);
            nut.setMinHeight(48);
            nut.setWrapText(false);
            nut.setMinWidth(0);
            nut.setPrefWidth(160);
            HBox.setHgrow(nut, Priority.ALWAYS);
            cacNut.getChildren().add(nut);
            if (cacMa[i].equals(giaTri)) {
                nut.setSelected(true);
            }
        }
        if (nhom.getSelectedToggle() == null) {
            nhom.selectToggle(nhom.getToggles().get(2));
        }
        VBox the = new VBox(14, DoHoaView.nhan(tieuDe, "nhanHoSo"), cacNut);
        the.setId(ma);
        the.setUserData(nhom);
        the.getStyleClass().add("theQuyenRiengTu");
        return the;
    }

    private void veDoiMatKhau() {
        cheDo = "matKhau";
        lanChonAnh++;
        xoaMatKhau();
        thongBao.setText("");
        Button luu = DoHoaView.nut("Lưu mật khẩu", "nutHoSoChinh"), huy =
                DoHoaView.nut("Hủy", "nutHoSoPhu");
        luu.setOnAction(suKien -> doiMatKhau());
        huy.setOnAction(suKien -> {
            veXem();
            thongBao.setText("");
        });
        Label goiY = DoHoaView.nhan(
                "Mật khẩu mới cần 8–128 ký tự. Các phiên khác của tài khoản này sẽ phải đăng nhập lại.",
                "goiYHoSo");
        goiY.setWrapText(true);
        noiDung.getChildren().setAll(DoHoaView.nhan("Đổi mật khẩu", "tenHoSo"), goiY,
                DoHoaView.nhan("Mật khẩu hiện tại", "nhanHoSo"), matKhauCu,
                DoHoaView.nhan("Mật khẩu mới", "nhanHoSo"), matKhauMoi,
                DoHoaView.nhan("Nhập lại mật khẩu mới", "nhanHoSo"), xacNhan, new HBox(10, luu, huy),
                thongBao);
    }

    private void doiMatKhau() {
        if (dangGui) {
            return;
        }
        if (!matKhauMoi.getText().equals(xacNhan.getText())) {
            thongBao.setText("Mật khẩu nhập lại chưa trùng khớp.");
            return;
        }
        if (matKhauMoi.getText().length() < 8 || matKhauMoi.getText().length() > 128) {
            thongBao.setText("Mật khẩu mới cần 8–128 ký tự.");
            return;
        }
        datDangGui(true);
        dieuKhien.doiMatKhau(matKhauCu.getText(), matKhauMoi.getText(), xacNhan.getText())
                .whenComplete((phanHoi, sai) -> Platform.runLater(() -> {
                    if (daDong) {
                        return;
                    }
                    datDangGui(false);
                    xoaMatKhau();
                    if (sai != null) {
                        thongBao.setText(DoHoaView.thongBao(sai));
                        return;
                    }
                    veXem();
                    thongBao.setText("Đã đổi mật khẩu. Các phiên khác đã được thu hồi.");
                }));
    }

    private void datDangGui(boolean gui) {
        dangGui = gui;
        noiDung.setDisable(gui);
        if (gui) {
            thongBao.setText("Đang lưu…");
        }
    }

    private void xoaMatKhau() {
        matKhauCu.clear();
        matKhauMoi.clear();
        xacNhan.clear();
    }

    public boolean choPhepRoi() {
        return !dangGui;
    }

    public void dong() {
        daDong = true;
        lanChonAnh++;
        xoaMatKhau();
    }
}
