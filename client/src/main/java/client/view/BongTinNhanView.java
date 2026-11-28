package client.view;

import client.controller.AnhTinNhanController;
import client.model.LichSuTinNhanModel;

import com.google.gson.*;
import giaothuc.DuLieu;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.*;
import java.util.concurrent.CompletableFuture;

/**
 * Một tin nhắn: trích dẫn, nội dung, cảm xúc và các thao tác ngay trong khung chat.
 */
final class BongTinNhanView extends VBox {
    BongTinNhanView(JsonObject tin, String idMinh, Consumer<JsonObject> traLoi,
                    BiFunction<String, JsonObject, CompletableFuture<Void>> thaoTac,
                    Consumer<JsonObject> taiTep, AnhTinNhanController dieuKhienAnh,
                    Function<String, CompletableFuture<Path>> taiTinThoai) {
        boolean cuaMinh = DuLieu.chu(tin, "nguoiGuiId").equals(idMinh);
        boolean daThu = LichSuTinNhanModel.daThuHoi(tin);

        setId("tinNhan" + DuLieu.chu(tin, "id"));
        setSpacing(5);
        setMaxWidth(450);
        setAlignment(cuaMinh ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        getStyleClass().add("nhomTin");
        setFillWidth(false);

        if (!daThu && tin.has("trichDan")) {
            getChildren().add(new TrichDanTinView(tin.getAsJsonObject("trichDan"), dieuKhienAnh));
        }

        Node chu = NoiDungTinView.tao(DuLieu.chu(tin, "noiDung"), cuaMinh ? "bongTinMinh" : "bongTinBan",
                440, !daThu);
        if (daThu) {
            chu.getStyleClass().add("tinDaThuHoi");
        }
        Node noiDung = !daThu && tin.has("tep") ? new TheTepView(tin, dieuKhienAnh, taiTinThoai) : chu;
        getChildren().add(noiDung);

        String gio = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                .format(Instant.parse(DuLieu.chu(tin, "taoLuc")));
        Label moc = DoHoaView.nhan((cuaMinh ? "Bạn" : DuLieu.chu(tin, "tenNguoiGui")) + "  ·  " + gio,
                "mocGio");

        HBox camXuc = new HBox(5);
        camXuc.setAlignment(cuaMinh ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        String cuaToi = DuLieu.chu(tin, "camXucCuaToi");
        if (tin.has("camXuc")) {
            for (JsonElement phan : tin.getAsJsonArray("camXuc")) {
                JsonObject cam = phan.getAsJsonObject();
                String bieu = DuLieu.chu(cam, "bieuCam");
                Button nut = DoHoaView.nut(bieu + " " + cam.get("soLuong").getAsLong(), "nutCamXucTin");
                if (bieu.equals(cuaToi)) {
                    nut.getStyleClass().add("camXucCuaMinh");
                }
                nut.setAccessibleText("Cảm xúc " + bieu + ", " + cam.get("soLuong").getAsLong() + " người" +
                        (bieu.equals(cuaToi) ? ", bạn đã chọn" : ""));
                nut.setOnAction(suKien -> thaoTac.apply("datCamXuc",
                        DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id"), "bieuCam",
                                bieu.equals(cuaToi) ? "" : bieu)));
                camXuc.getChildren().add(nut);
            }
        }
        if (!camXuc.getChildren().isEmpty()) {
            getChildren().add(camXuc);
        }
        getChildren().add(moc);
        if (cuaMinh) {
            Label trangThai = DoHoaView.nhan(
                    tin.has("daXem") && tin.get("daXem").getAsBoolean() ? "Đã xem" : "Đã gửi", "mocGio");
            getChildren().add(trangThai);
        }
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("menuTinNhan");
        if (!daThu) {
            MenuItem saoChep = new MenuItem("Sao chép");
            saoChep.setOnAction(suKien -> {
                if (tin.has("tep") && DuLieu.chu(tin.getAsJsonObject("tep"), "loaiTep").equals("anh")) {
                    dieuKhienAnh.tai(DuLieu.chu(tin, "id")).whenComplete((anh, loi) -> {
                        if (loi != null) {
                            getChildren().add(
                                    DoHoaView.nhan("Chưa sao chép được ảnh. Hãy thử lại.", "loiChat"));
                            return;
                        }
                        var noiDungChep = new javafx.scene.input.ClipboardContent();
                        noiDungChep.putImage(
                                new javafx.scene.image.Image(new java.io.ByteArrayInputStream(anh)));
                        javafx.scene.input.Clipboard.getSystemClipboard().setContent(noiDungChep);
                    });
                } else {
                    var noiDungChep = new javafx.scene.input.ClipboardContent();
                    noiDungChep.putString(DuLieu.chu(tin, "noiDung"));
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(noiDungChep);
                }
            });
            menu.getItems().add(saoChep);
            if (tin.has("tep")) {
                MenuItem tai = new MenuItem("Tải xuống");
                tai.setOnAction(suKien -> taiTep.accept(tin));
                menu.getItems().add(tai);
            }
            MenuItem chuyen = new MenuItem("Chuyển tiếp");
            chuyen.setOnAction(suKien -> thaoTac.apply("moChuyenTiep",
                    DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id"))));
            menu.getItems().add(chuyen);
            MenuItem ghim = new MenuItem("Ghim tin nhắn");
            ghim.setOnAction(
                    e -> thaoTac.apply("ghimTin", DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id"))));
            menu.getItems().add(ghim);
            MenuItem dap = new MenuItem("Trả lời");
            dap.setOnAction(suKien -> traLoi.accept(tin));
            menu.getItems().add(dap);
            Menu cam = new Menu("Thả cảm xúc");
            for (String bieu : new String[]{"👍", "❤️", "😂", "😮", "😢", "😡"}) {
                MenuItem chon = new MenuItem(bieu + (bieu.equals(cuaToi) ? " ✓" : ""));
                chon.setOnAction(suKien -> thaoTac.apply("datCamXuc",
                        DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id"), "bieuCam",
                                bieu.equals(cuaToi) ? "" : bieu)));
                cam.getItems().add(chon);
            }
            menu.getItems().add(cam);
            if (cuaMinh) {
                MenuItem thu = new MenuItem("Thu hồi");
                thu.setOnAction(suKien -> xacNhan("Thu hồi tin này với mọi người?", "Thu hồi",
                        () -> thaoTac.apply("thuHoiTin",
                                DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id")))));
                menu.getItems().add(thu);
            }
        }
        MenuItem xoa = new MenuItem("Xóa ở phía tôi");
        xoa.setOnAction(suKien -> xacNhan("Xóa tin này ở phía bạn? Người khác vẫn thấy tin.", "Xóa",
                () -> thaoTac.apply("xoaTin", DuLieu.tao("tinNhanId", DuLieu.chu(tin, "id")))));
        menu.getItems().add(xoa);
        setUserData(menu);
        setOnContextMenuRequested(suKien -> {
            menu.show(this, suKien.getScreenX(), suKien.getScreenY());
            suKien.consume();
        });
        sceneProperty().addListener((giaTri, cu, moi) -> {
            if (moi == null) {
                menu.hide();
            }
        });
    }

    private void xacNhan(String chu, String ten, java.util.function.Supplier<CompletableFuture<Void>> viec) {
        ThongBaoNoiView.xacNhan(this, ten + " tin nhắn", chu, ten, viec);
    }

    private Button nutIcon(Node hinh, String nhan, String lop) {
        Button nut = DoHoaView.nut("", "nutThaoTacTin");
        nut.getStyleClass().add(lop);
        nut.setGraphic(hinh);
        nut.setTooltip(new Tooltip(nhan));
        nut.setAccessibleText(nhan);
        return nut;
    }

    private VBox trichDan(JsonObject trich) {
        Label ten = DoHoaView.nhan(DuLieu.chu(trich, "tenNguoiGui"), "tenTrichDan");
        Label chu = DoHoaView.nhan(DuLieu.chu(trich, "noiDung"), "chuTrichDan");
        chu.setWrapText(true);
        chu.setMaxWidth(380);
        VBox khung = new VBox(4, ten, chu);
        khung.getStyleClass().add("trichDanTin");
        khung.setMaxWidth(430);
        return khung;
    }
}
