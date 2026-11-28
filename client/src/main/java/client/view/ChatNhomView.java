package client.view;

import client.controller.NhomController;
import client.hatang.KenhTCP;

import com.google.gson.*;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class ChatNhomView extends BorderPane {
    private final NhomController dieuKhien;
    private final String id, toi;
    private final VBox cacTin = new VBox(12);
    private final ScrollPane cuon = new ScrollPane(cacTin);
    private final TreeMap<Long, Node> daVe = new TreeMap<>();
    private final Button tinCu = DoHoaView.nut("Xem tin nhắn trước", "nutTaiTinCu");
    private final Button veCuoi = DoHoaView.nut("", "nutVeCuoi");
    private final Button gui = DoHoaView.nut("", "nutGui");
    private final TextArea noiDung = new TextArea();
    private final Label loi = DoHoaView.nhan("", "loiChat");
    private final Label trong = DoHoaView.nhan("Chưa có tin nhắn trong nhóm.", "giaTriHoSo");
    private boolean dangGui, dangTai;

    ChatNhomView(KenhTCP kenh, JsonObject nhom, String toi) {
        dieuKhien = new NhomController(kenh);
        this.toi = toi;
        id = DuLieu.chu(nhom, "id");
        setId("chatNhom");
        getStyleClass().add("vungChat");
        String ten = DuLieu.chu(nhom, "tenHienThi");
        VBox danhTinh = new VBox(5, DoHoaView.nhan(ten, "tenHoiThoai"),
                DoHoaView.nhan(nhom.get("soThanhVien").getAsInt() + " thành viên", "moTaHoiThoai"));
        StackPane avatar = new StackPane(BieuTuongView.banBe());
        HBox dau = new HBox(12, avatar, danhTinh);
        if (nhom.has("coAnh") && nhom.get("coAnh").getAsBoolean()) {
            dieuKhien.anh(id).thenAccept(tra -> Platform.runLater(() -> {
                String anh = DuLieu.chu(tra, "anhDaiDien");
                if (!anh.isEmpty()) {
                    avatar.getChildren().setAll(DoHoaView.anhHoSo(
                            DuLieu.tao("id", id, "tenHienThi", ten, "anhDaiDien", anh), 44));
                }
            }));
        }
        dau.setAlignment(Pos.CENTER_LEFT);
        dau.getStyleClass().add("dauHoiThoai");
        setTop(dau);

        cacTin.getStyleClass().add("cacTinNhan");
        cacTin.getChildren().addAll(tinCu, trong);
        tinCu.setVisible(false);
        tinCu.managedProperty().bind(tinCu.visibleProperty());
        tinCu.setOnAction(e -> tai(true));
        trong.managedProperty().bind(trong.visibleProperty());
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("lichSuChat");
        cuon.setId("lichSuNhom");
        veCuoi.setGraphic(BieuTuongView.xuongCuoi());
        veCuoi.setAccessibleText("Về tin nhắn mới nhất");
        veCuoi.setOnAction(e -> xuongCuoi());
        veCuoi.setVisible(false);
        cuon.vvalueProperty().addListener((o, cu, moi) -> veCuoi.setVisible(!oCuoi()));
        StackPane lichSu = new StackPane(cuon, veCuoi);
        StackPane.setAlignment(veCuoi, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(veCuoi, new Insets(14));
        setCenter(lichSu);

        noiDung.setId("noiDungNhom");
        noiDung.setPromptText("Viết tin nhắn nhóm…");
        noiDung.setWrapText(true);
        noiDung.setPrefRowCount(2);
        noiDung.getStyleClass().add("oSoanTin");
        HBox.setHgrow(noiDung, Priority.ALWAYS);
        noiDung.setTextFormatter(
                new TextFormatter<String>(doi -> doi.getControlNewText().length() <= 4000 ? doi : null));
        noiDung.textProperty().addListener((o, cu, moi) -> capNhatNut());
        noiDung.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                guiTin();
            }
        });
        gui.setId("guiTinNhom");
        gui.getStyleClass().add("nutIconSoan");
        gui.setGraphic(BieuTuongView.gui());
        gui.setAccessibleText("Gửi tin nhắn nhóm");
        gui.setOnAction(e -> guiTin());
        capNhatNut();
        HBox hang = new HBox(10, noiDung, gui);
        hang.setAlignment(Pos.CENTER);
        loi.setWrapText(true);
        loi.visibleProperty().bind(loi.textProperty().isNotEmpty());
        loi.managedProperty().bind(loi.visibleProperty());
        VBox khungSoan = new VBox(hang);
        khungSoan.getStyleClass().add("khungSoan");
        VBox soan = new VBox(8, loi, khungSoan);
        soan.getStyleClass().add("vungSoan");
        setBottom(soan);
        tai(false);
    }

    String id() {
        return id;
    }

    void nhan(JsonObject tin) {
        boolean oCuoi = oCuoi();
        them(tin);
        if (oCuoi) {
            xuongCuoi();
        } else {
            veCuoi.setVisible(true);
        }
    }

    private void tai(boolean cu) {
        if (dangTai) {
            return;
        }
        dangTai = true;
        tinCu.setDisable(true);
        String truoc = cu && !daVe.isEmpty() ? Long.toString(daVe.firstKey()) : null;
        double caoCu = cacTin.getHeight(), viTri = cuon.getVvalue();
        dieuKhien.lichSu(id, truoc)
                .whenComplete((tra, sai) -> Platform.runLater(() -> {
                    dangTai = false;
                    tinCu.setDisable(false);
                    if (sai != null) {
                        loi.setText(DoHoaView.thongBao(sai));
                        tinCu.setText("Thử tải lại");
                        tinCu.setVisible(true);
                        return;
                    }
                    loi.setText("");
                    tinCu.setText("Xem tin nhắn trước");
                    tra.getAsJsonArray("tinNhan").forEach(p -> them(p.getAsJsonObject()));
                    tinCu.setVisible(tra.get("coTinCu").getAsBoolean());
                    if (!cu) {
                        xuongCuoi();
                    } else {
                        Platform.runLater(() -> {
                            layout();
                            double khung = cuon.getViewportBounds().getHeight(), moi =
                                    cacTin.getHeight() - khung;
                            if (moi > 0) {
                                cuon.setVvalue(Math.min(1,
                                        (viTri * Math.max(0, caoCu - khung) + cacTin.getHeight() - caoCu) /
                                                moi));
                            }
                        });
                    }
                }));
    }

    private void them(JsonObject tin) {
        long ma = Long.parseLong(DuLieu.chu(tin, "id"));
        if (daVe.containsKey(ma)) {
            return;
        }
        boolean minh = toi.equals(DuLieu.chu(tin, "nguoiGuiId"));
        Node chu = NoiDungTinView.tao(DuLieu.chu(tin, "noiDung"), minh ? "bongTinMinh" : "bongTinBan", 450, true);
        String gio = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                .format(Instant.parse(DuLieu.chu(tin, "taoLuc")));
        Label moc =
                DoHoaView.nhan((minh ? "Bạn" : DuLieu.chu(tin, "tenNguoiGui")) + " · " + gio, "mocGio");
        VBox bong = new VBox(5, chu, moc);
        bong.setAlignment(minh ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bong.setFillWidth(false);
        HBox hang = new HBox(bong);
        hang.setAlignment(minh ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        hang.setId("tinNhom" + ma);
        int viTri = daVe.headMap(ma).size();
        daVe.put(ma, hang);
        cacTin.getChildren().add(viTri + 2, hang);
        trong.setVisible(false);
    }

    private boolean oCuoi() {
        return cuon.getVvalue() >= 0.96 || cacTin.getHeight() <= cuon.getViewportBounds().getHeight();
    }

    private void xuongCuoi() {
        Platform.runLater(() -> {
            layout();
            cuon.setVvalue(1);
            veCuoi.setVisible(false);
        });
    }

    private void capNhatNut() {
        gui.setDisable(dangGui || noiDung.getText().isBlank());
    }

    private void guiTin() {
        if (gui.isDisabled()) {
            return;
        }
        String chu = noiDung.getText().strip();
        dangGui = true;
        loi.setText("");
        capNhatNut();
        dieuKhien.gui(id, chu)
                .whenComplete((tra, sai) -> Platform.runLater(() -> {
                    dangGui = false;
                    if (sai != null) {
                        loi.setText(DoHoaView.thongBao(sai));
                    } else {
                        them(tra.getAsJsonObject("tinNhan"));
                        if (noiDung.getText().strip().equals(chu)) {
                            noiDung.clear();
                        }
                        dieuKhien.daGui();
                        xuongCuoi();
                    }
                    capNhatNut();
                }));
    }
}
