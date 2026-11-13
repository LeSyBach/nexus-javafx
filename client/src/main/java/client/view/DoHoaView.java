package client.view;

import giaothuc.DuLieu;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.concurrent.CompletionException;

public final class DoHoaView {
    private DoHoaView() {
    }

    static Label nhan(String chu, String lop) {
        Label nhan = new Label(chu);
        nhan.getStyleClass().add(lop);
        return nhan;
    }

    public static Button nut(String chu, String lop) {
        Button nut = new Button(chu);
        nut.getStyleClass().add(lop);
        return nut;
    }

    static StackPane anh(String ten, String id, boolean nho) {
        String[] cacTu = ten.strip().split("\\s+");
        String chu = cacTu.length > 1 ?
                cacTu[cacTu.length - 2].substring(0, 1) + cacTu[cacTu.length - 1].substring(0, 1) :
                ten.substring(0, 1);
        Label nhan = new Label(chu.toUpperCase());
        StackPane anh = new StackPane(nhan);
        anh.getStyleClass().addAll("anhDaiDien", "anhMau" + Math.floorMod(id.hashCode(), 3),
                nho ? "anhNho" : "anhVua");
        return anh;
    }

    static StackPane anhHoSo(com.google.gson.JsonObject nguoi, int kichThuoc) {
        String ten = DuLieu.chu(nguoi, "tenHienThi");
        if (ten.isBlank()) {
            ten = "N";
        }
        StackPane anh = anh(ten, DuLieu.chu(nguoi, "id"), false);
        anh.setStyle("-fx-min-width:" + kichThuoc + ";-fx-max-width:" + kichThuoc + ";-fx-pref-width:" +
                kichThuoc +
                ";-fx-min-height:" + kichThuoc + ";-fx-max-height:" + kichThuoc + ";-fx-pref-height:" +
                kichThuoc + ";");
        String maAnh = DuLieu.chu(nguoi, "anhDaiDien");
        if (!maAnh.isEmpty()) {
            try {
                var hinh = new javafx.scene.image.Image(
                        new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(maAnh)));
                if (!hinh.isError()) {
                    var hinhVe = new javafx.scene.image.ImageView(hinh);
                    hinhVe.setFitWidth(kichThuoc);
                    hinhVe.setFitHeight(kichThuoc);
                    var cat = new javafx.scene.shape.Rectangle(kichThuoc, kichThuoc);
                    cat.setArcWidth(24);
                    cat.setArcHeight(24);
                    hinhVe.setClip(cat);
                    anh.getChildren().setAll(hinhVe);
                }
            } catch (IllegalArgumentException loi) {/* Hiện chữ cái khi ảnh không đọc được. */}
        }
        return anh;
    }

    public static String thongBao(Throwable loi) {
        while (loi instanceof CompletionException && loi.getCause() != null) {
            loi = loi.getCause();
        }
        return loi.getMessage() == null ? "Chưa thực hiện được yêu cầu. Bạn thử lại nhé." : loi.getMessage();
    }
}
