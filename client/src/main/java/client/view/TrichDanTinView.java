package client.view;

import client.controller.AnhTinNhanController;
import client.model.LichSuTinNhanModel;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.layout.*;

import java.io.ByteArrayInputStream;

/**
 * Trích dẫn thống nhất cho lịch sử, ô soạn và hộp chia sẻ.
 */
final class TrichDanTinView extends HBox {
    TrichDanTinView(JsonObject tin, AnhTinNhanController dieuKhienAnh) {
        setSpacing(10);
        setMaxWidth(370);
        getStyleClass().add("trichDanTin");
        boolean laAnh = DuLieu.chu(tin, "loaiTin").equals("anh") && !LichSuTinNhanModel.daThuHoi(tin);
        Label ten = DoHoaView.nhan(DuLieu.chu(tin, "tenNguoiGui"), "tenTrichDan");
        Label chu = DoHoaView.nhan(noiDung(tin), "chuTrichDan");
        chu.setWrapText(true);
        chu.setMaxWidth(280);
        VBox cacChu = new VBox(4, ten, chu);
        cacChu.setMinWidth(0);
        HBox.setHgrow(cacChu, Priority.ALWAYS);
        if (laAnh) {
            StackPane khung = new StackPane(DoHoaView.nhan("Ảnh", "chuTrichDan"));
            khung.setMinSize(56, 56);
            khung.setPrefSize(56, 56);
            khung.setMaxSize(56, 56);
            khung.getStyleClass().add("anhTrichDan");
            getChildren().add(khung);
            sceneProperty().addListener((o, cu, moi) -> {
                if (moi != null) {
                    dieuKhienAnh.tai(DuLieu.chu(tin, "id")).whenComplete((anh, loi) -> {
                        if (getScene() == null || loi != null) {
                            return;
                        }
                        Image hinh = new Image(new ByteArrayInputStream(anh));
                        if (hinh.isError()) {
                            return;
                        }
                        ImageView xem = new ImageView(hinh);
                        xem.setFitWidth(56);
                        xem.setFitHeight(56);
                        xem.setPreserveRatio(true);
                        xem.setAccessibleText("Ảnh được trích dẫn");
                        khung.getChildren().setAll(xem);
                    });
                }
            });
        }
        getChildren().add(cacChu);
    }

    static String noiDung(JsonObject tin) {
        String chu = DuLieu.chu(tin, "noiDung");
        if (DuLieu.chu(tin, "loaiTin").equals("anh") && !LichSuTinNhanModel.daThuHoi(tin) &&
                chu.startsWith("Tệp: ")) {
            return "Ảnh";
        }
        return chu.substring(0,
                chu.offsetByCodePoints(0, Math.min(120, chu.codePointCount(0, chu.length()))));
    }
}
