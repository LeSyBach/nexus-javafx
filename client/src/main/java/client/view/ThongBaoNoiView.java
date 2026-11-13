package client.view;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Xác nhận phủ trong Scene hiện tại, không tạo Stage hay cửa sổ hệ điều hành.
 */
public final class ThongBaoNoiView {
    private ThongBaoNoiView() {
    }

    static Runnable mo(Node neo, Region noiDung, java.util.function.BooleanSupplier duocDong) {
        Scene canh = neo.getScene();
        if (canh == null) {
            return () -> {
            };
        }
        Parent goc = canh.getRoot();
        StackPane khung;
        if (goc instanceof StackPane coSan) {
            khung = coSan;
        } else {
            khung = new StackPane();
            canh.setRoot(khung);
            khung.getChildren().add(goc);
        }
        Node tieuDiemCu = canh.getFocusOwner();
        Map<Node, Boolean> cu = new LinkedHashMap<>();
        for (Node con : khung.getChildren()) {
            cu.put(con, con.isDisable());
            con.setDisable(true);
        }
        StackPane nen = new StackPane(noiDung);
        nen.getStyleClass().add("nenXacNhanNoi");
        nen.setId("lopChiaSe");
        Runnable dong = () -> {
            khung.getChildren().remove(nen);
            cu.forEach(Node::setDisable);
            if (tieuDiemCu != null && tieuDiemCu.getScene() == canh) {
                tieuDiemCu.requestFocus();
            }
        };
        nen.setOnKeyPressed(suKien -> {
            if (suKien.getCode() == KeyCode.ESCAPE) {
                if (duocDong.getAsBoolean()) {
                    dong.run();
                }
                suKien.consume();
            }
        });
        noiDung.maxHeightProperty().bind(canh.heightProperty().subtract(48));
        noiDung.maxWidthProperty()
                .bind(javafx.beans.binding.Bindings.min(620, canh.widthProperty().subtract(48)));
        khung.getChildren().add(nen);
        return dong;
    }

    public static void xacNhan(Node neo, String tieuDe, String noiDung, String tenNut,
                               Supplier<CompletableFuture<Void>> thucHien) {
        Scene canh = neo.getScene();
        if (canh == null || canh.getRoot().lookup("#xacNhanNoi") != null) {
            return;
        }
        Parent goc = canh.getRoot();
        StackPane khung;
        if (goc instanceof StackPane coSan) {
            khung = coSan;
        } else {
            khung = new StackPane();
            canh.setRoot(khung);
            khung.getChildren().add(goc);
        }
        Node tieuDiemCu = canh.getFocusOwner();
        Map<Node, Boolean> trangThaiCu = new LinkedHashMap<>();
        for (Node con : khung.getChildren()) {
            trangThaiCu.put(con, con.isDisable());
            con.setDisable(true);
        }
        StackPane nen = new StackPane();
        nen.setId("xacNhanNoi");
        nen.getStyleClass().add("nenXacNhanNoi");
        Label ten = DoHoaView.nhan(tieuDe, "tieuDeXacNhanNoi"), chu =
                DoHoaView.nhan(noiDung, "noiDungXacNhanNoi");
        chu.setWrapText(true);
        Label loi = DoHoaView.nhan("", "loiChat");
        loi.setWrapText(true);
        loi.setVisible(false);
        loi.managedProperty().bind(loi.visibleProperty());
        Button huy = DoHoaView.nut("Hủy", "nutHuyThuHoi"), dongY =
                DoHoaView.nut(tenNut, "nutXacNhanThuHoi");
        huy.setCancelButton(true); // Enter không mặc định kích hoạt thao tác có ảnh hưởng dữ liệu.
        HBox cacNut = new HBox(12, huy, dongY);
        cacNut.setAlignment(Pos.CENTER_RIGHT);
        VBox the = new VBox(18, ten, chu, loi, cacNut);
        the.getStyleClass().add("theXacNhanNoi");
        the.setMaxWidth(420);
        the.setMaxHeight(Region.USE_PREF_SIZE);
        nen.getChildren().add(the);
        Runnable dong = () -> {
            khung.getChildren().remove(nen);
            trangThaiCu.forEach(Node::setDisable);
            if (tieuDiemCu != null && tieuDiemCu.getScene() == canh) {
                tieuDiemCu.requestFocus();
            }
        };
        huy.setOnAction(suKien -> dong.run());
        nen.setOnKeyPressed(suKien -> {
            if (suKien.getCode() == KeyCode.ESCAPE) {
                if (!huy.isDisabled()) {
                    dong.run();
                }
                suKien.consume();
            }
        });
        dongY.setOnAction(suKien -> {
            huy.setDisable(true);
            dongY.setDisable(true);
            loi.setVisible(false);
            CompletableFuture<Void> cho;
            try {
                cho = thucHien.get();
            } catch (Exception sai) {
                cho = CompletableFuture.failedFuture(sai);
            }
            cho.whenComplete((ketQua, sai) -> Platform.runLater(() -> {
                if (sai == null) {
                    dong.run();
                } else {
                    huy.setDisable(false);
                    dongY.setDisable(false);
                    loi.setText(DoHoaView.thongBao(sai));
                    loi.setVisible(true);
                    huy.requestFocus();
                }
            }));
        });
        khung.getChildren().add(nen);
        Platform.runLater(huy::requestFocus);
    }
}
