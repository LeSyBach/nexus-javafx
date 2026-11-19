package client.view;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

final class LichSuGoiView extends VBox {
    private static final DateTimeFormatter GIO =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    LichSuGoiView(JsonObject tin, boolean cuaMinh, ObservableBooleanValue dangCoCuocGoi, Runnable goiLai,
                  BiFunction<String, JsonObject, CompletableFuture<Void>> thaoTac) {
        String id = DuLieu.chu(tin, "id");
        setId("tinNhan" + id);
        setSpacing(4);
        setAlignment(cuaMinh ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        setFillWidth(false);

        Label ketQua = DoHoaView.nhan(DuLieu.chu(tin, "noiDung"), "tenTrichDan");
        ketQua.setWrapText(true);
        Button nutGoiLai = DoHoaView.nut("Gọi lại", "nutHoSoPhu");
        nutGoiLai.disableProperty().bind(dangCoCuocGoi);
        nutGoiLai.setOnAction(e -> goiLai.run());

        HBox the = new HBox(12, ketQua, nutGoiLai);
        the.setAlignment(Pos.CENTER_LEFT);
        the.getStyleClass().add("bongTinBan");
        String gio = GIO.format(Instant.parse(DuLieu.chu(tin, "taoLuc")));
        getChildren().addAll(the, DoHoaView.nhan(gio, "mocGio"));

        MenuItem xoa = new MenuItem("Xóa ở phía tôi");
        xoa.setOnAction(e -> ThongBaoNoiView.xacNhan(this, "Xóa mục cuộc gọi",
                "Xóa mục cuộc gọi ở phía bạn? Đối phương vẫn thấy mục này.", "Xóa",
                () -> thaoTac.apply("xoaTin", DuLieu.tao("tinNhanId", id))));
        ContextMenu menu = new ContextMenu(xoa);
        menu.getStyleClass().add("menuTinNhan");
        setUserData(menu);
        setOnContextMenuRequested(e -> {
            menu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        sceneProperty().addListener((giaTri, cu, moi) -> {
            if (moi == null) {
                menu.hide();
            }
        });
    }
}
