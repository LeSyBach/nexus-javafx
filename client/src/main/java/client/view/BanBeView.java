package client.view;

import client.controller.BanBeController;
import client.model.BanBeModel;
import client.hatang.KenhTCP;

import com.google.gson.*;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.*;

/**
 * Giao diện bạn bè; sự kiện TCP vẫn được nhận tập trung ở HoiThoaiView.
 */
public final class BanBeView extends VBox {
    private final BanBeController dieuKhien;
    private final Consumer<Map<String, JsonObject>> khiDoiDanhSach;
    private final BiConsumer<String, String> khiMoChat;
    private final BanBeModel model = new BanBeModel();
    private final VBox cacDong = new VBox(10), ketQuaTim = new VBox(10);
    private final Label thongBao = DoHoaView.nhan("", "thongBaoBanBe");
    private final TextField email = new TextField();
    private final Button tim = DoHoaView.nut("Tìm tài khoản", "nutChinh");
    private final ToggleButton banBe = new ToggleButton("Bạn bè"), loiMoi = new ToggleButton("Lời mời nhận"),
            daGui = new ToggleButton("Đã gửi");
    private final VBox xacNhanHuy = new VBox(10);
    private String theDangMo = "banBe";
    private boolean dangThaoTac, daDong;

    public BanBeView(KenhTCP kenh, Consumer<Map<String, JsonObject>> khiDoiDanhSach,
                     BiConsumer<String, String> khiMoChat) {
        dieuKhien = new BanBeController(kenh);
        this.khiDoiDanhSach = khiDoiDanhSach;
        this.khiMoChat = khiMoChat;
        setSpacing(16);
        getStyleClass().add("hopBanBe");
        setPrefSize(630, 580);
        setMaxWidth(720);
        setId("manHinhBanBe");
        Label tieuDe = DoHoaView.nhan("Kết nối bạn bè", "tieuDeBanBe");
        Label moTa =
                DoHoaView.nhan("Tìm bằng email hoặc số điện thoại để gửi lời mời kết bạn.", "moTaBanBe");
        Label nhanEmail = DoHoaView.nhan("Email hoặc số điện thoại", "nhanEmailBanBe");
        email.setId("emailBanBe");
        email.setPromptText("Nhập email hoặc số điện thoại");
        email.setAccessibleText("Email hoặc số điện thoại của người muốn kết bạn");
        HBox hangTim = new HBox(10, email, tim);
        HBox.setHgrow(email, Priority.ALWAYS);
        tim.setOnAction(suKien -> timTaiKhoan());
        email.setOnAction(suKien -> timTaiKhoan());
        ToggleGroup nhom = new ToggleGroup();
        ToggleButton[] cacThe = {banBe, loiMoi, daGui};
        String[] cacMa = {"banBe", "daNhan", "daGui"};
        for (int i = 0; i < cacThe.length; i++) {
            ToggleButton the = cacThe[i];
            String ma = cacMa[i];
            the.setToggleGroup(nhom);
            the.getStyleClass().add("theBanBe");
            the.setOnAction(suKien -> {
                the.setSelected(true);
                theDangMo = ma;
                veDanhSach();
                veKetQuaTim();
            });
        }
        banBe.setSelected(true);
        HBox cacTheNgang = new HBox(8, banBe, loiMoi, daGui);

        ScrollPane cuon = new ScrollPane(cacDong);
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("cuonBanBe");
        VBox.setVgrow(cuon, Priority.ALWAYS);

        thongBao.setWrapText(true);
        thongBao.setMinHeight(25);
        thongBao.setAccessibleText("Kết quả thao tác bạn bè");

        xacNhanHuy.getStyleClass().add("xacNhanTrongTrang");
        xacNhanHuy.setVisible(false);
        xacNhanHuy.managedProperty().bind(xacNhanHuy.visibleProperty());

        getChildren().addAll(tieuDe, moTa, nhanEmail, hangTim, ketQuaTim, cacTheNgang, cuon, xacNhanHuy,
                thongBao);
        ketQuaTim.managedProperty().bind(ketQuaTim.visibleProperty());
        ketQuaTim.setVisible(false);
        veDanhSach();
    }

    public void taiDanhSach() {
        dieuKhien.taiDanhSach(Platform::runLater, ketQua -> {
            model.capNhatDanhSach(ketQua);
            khiDoiDanhSach.accept(model.danhSach());
            veDanhSach();
            veKetQuaTim();
        }, sai -> thongBao.setText(DoHoaView.thongBao(sai)));
    }

    private void timTaiKhoan() {
        String chu = email.getText().strip();
        if (chu.isEmpty()) {
            thongBao.setText("Nhập email hoặc số điện thoại.");
            email.requestFocus();
            return;
        }
        tim.setDisable(true);
        email.setDisable(true);
        thongBao.setText("Đang tìm tài khoản…");
        dieuKhien.timTaiKhoan(chu).whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            tim.setDisable(false);
            email.setDisable(false);
            model.capNhatKetQuaTim(null);
            if (sai != null) {
                thongBao.setText(DoHoaView.thongBao(sai));
            } else if (!duLieu.get("timThay").getAsBoolean()) {
                thongBao.setText("Không tìm thấy tài khoản phù hợp.");
            } else {
                model.capNhatKetQuaTim(duLieu.getAsJsonObject("nguoiDung"));
                thongBao.setText("Đã tìm thấy tài khoản.");
            }
            veKetQuaTim();
        }));
    }

    private void veKetQuaTim() {
        ketQuaTim.getChildren().clear();
        JsonObject nguoi = model.ketQuaTim();
        ketQuaTim.setVisible(nguoi != null);
        if (nguoi == null) {
            return;
        }
        // Nếu người này đã nằm trong thẻ đang xem, chỉ hiện một dòng ở danh sách.
        if (DuLieu.chu(nguoi, "quanHe").equals(theDangMo)) {
            ketQuaTim.setVisible(false);
            return;
        }
        ketQuaTim.getChildren().add(taoDong(nguoi));
    }

    private void veDanhSach() {
        long soBan = model.demQuanHe("banBe");
        long soMoi = model.demQuanHe("daNhan");
        long soGui = model.tongSoQuanHe() - soBan - soMoi;
        banBe.setText("Bạn bè (" + soBan + ")");
        loiMoi.setText("Lời mời nhận (" + soMoi + ")");
        daGui.setText("Đã gửi (" + soGui + ")");
        cacDong.getChildren().clear();
        model.loc(theDangMo).forEach(nguoi -> cacDong.getChildren().add(taoDong(nguoi)));
        if (cacDong.getChildren().isEmpty()) {
            Label trong = DoHoaView.nhan(theDangMo.equals("banBe") ?
                    "Chưa có bạn bè. Tìm bằng email hoặc số điện thoại để gửi lời mời." :
                    theDangMo.equals("daNhan") ? "Bạn chưa có lời mời kết bạn mới." :
                            "Bạn chưa có lời mời đang chờ phản hồi.", "trongBanBe");
            trong.setWrapText(true);
            cacDong.getChildren().add(trong);
        }
    }

    private HBox taoDong(JsonObject nguoi) {
        String id = DuLieu.chu(nguoi, "id"), ten = DuLieu.chu(nguoi, "tenHienThi"), quanHe =
                DuLieu.chu(nguoi, "quanHe");
        Label tenNhan = DoHoaView.nhan(ten, "tenBanBe");
        tenNhan.setMaxWidth(220);
        tenNhan.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label trangThai = DoHoaView.nhan(switch (quanHe) {
            case "banBe" -> nguoi.get("dangOnline").getAsBoolean() ? "Đã kết bạn · Đang online" :
                    "Đã kết bạn · Ngoại tuyến";
            case "daNhan" -> "Muốn kết bạn với bạn";
            case "daGui" -> "Đang chờ phản hồi";
            default -> "Chưa kết bạn";
        }, "trangThaiBanBe");
        VBox chu = new VBox(5, tenNhan, trangThai);
        HBox.setHgrow(chu, Priority.ALWAYS);
        chu.setMinWidth(0);
        HBox hang = new HBox(12, DoHoaView.anh(ten, id, false), chu);
        hang.setAlignment(Pos.CENTER_LEFT);
        hang.getStyleClass().add("dongBanBe");
        hang.setAccessibleText(ten + ", " + trangThai.getText());
        tenNhan.setStyle("-fx-cursor: hand;");
        tenNhan.setOnMouseClicked(e -> ThongTinChatView.hoSo(this, dieuKhien, id, khiMoChat));
        Button xem = DoHoaView.nut("Hồ sơ", "nutBanBePhu");
        xem.setOnAction(e -> ThongTinChatView.hoSo(this, dieuKhien, id, khiMoChat));
        hang.getChildren().add(xem);
        switch (quanHe) {
            case "banBe" -> {
                Button nhan = DoHoaView.nut("Nhắn tin", "nutBanBeChinh");
                nhan.setOnAction(suKien -> khiMoChat.accept(id, ten));
                Button huy = nutThaoTac("Hủy kết bạn", "huyBan", id);
                huy.setOnAction(suKien -> ThongBaoNoiView.xacNhan(this, "Hủy kết bạn",
                        "Hủy kết bạn với " + ten + "? Lịch sử trò chuyện vẫn được giữ.", "Hủy kết bạn",
                        () -> {
                            thaoTac(id, "huyBan");
                            return java.util.concurrent.CompletableFuture.completedFuture(null);
                        }));
                hang.getChildren().addAll(nhan, huy);
            }
            case "daNhan" -> hang.getChildren()
                    .addAll(nutThaoTac("Chấp nhận", "chapNhan", id), nutThaoTac("Từ chối", "tuChoi", id));
            case "daGui" -> hang.getChildren().add(nutThaoTac("Thu hồi", "thuHoi", id));
            default -> hang.getChildren().add(nutThaoTac("Kết bạn", "gui", id));
        }
        return hang;
    }

    private Button nutThaoTac(String ten, String thaoTac, String id) {
        Button nut = DoHoaView.nut(ten,
                thaoTac.equals("gui") || thaoTac.equals("chapNhan") ? "nutBanBeChinh" : "nutBanBePhu");
        nut.setOnAction(suKien -> {
            if (thaoTac.equals("thuHoi")) {
                ThongBaoNoiView.xacNhan(this, "Thu hồi lời mời", "Bạn muốn thu hồi lời mời kết bạn này?",
                        "Thu hồi", () -> {
                            thaoTac(id, thaoTac);
                            return java.util.concurrent.CompletableFuture.completedFuture(null);
                        });
            } else {
                thaoTac(id, thaoTac);
            }
        });
        return nut;
    }

    private void thaoTac(String id, String thaoTac) {
        if (dangThaoTac || daDong) {
            return;
        }
        xacNhanHuy.setVisible(false);
        dangThaoTac = true;
        cacDong.setDisable(true);
        ketQuaTim.setDisable(true);
        thongBao.setText("Đang xử lý…");
        dieuKhien.thayDoiQuanHe(id, thaoTac).whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            dangThaoTac = false;
            cacDong.setDisable(false);
            ketQuaTim.setDisable(false);
            thongBao.setText(sai == null ? switch (thaoTac) {
                case "gui" -> "Đã gửi lời mời kết bạn.";
                case "chapNhan" -> "Đã kết bạn. Bạn có thể bắt đầu trò chuyện.";
                case "tuChoi" -> "Đã từ chối lời mời.";
                case "thuHoi" -> "Đã thu hồi lời mời.";
                default -> "Đã hủy kết bạn.";
            } : DoHoaView.thongBao(sai));
            taiDanhSach();
        }));
    }

    public void dong() {
        daDong = true;
        dieuKhien.dongDanhSach();
    }
}
