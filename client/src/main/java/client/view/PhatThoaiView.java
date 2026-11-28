package client.view;

import client.hatang.PhatTinNhanThoai;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Nút phát trong khung chat và trong bản ghi đang soạn. */
final class PhatThoaiView extends VBox {
    private final Button nut = DoHoaView.nut("", "nutHoSoPhu");
    private final Label loi = DoHoaView.nhan("", "loiChat");
    private final String tenNut;
    private final Supplier<CompletableFuture<Path>> layTep;
    private final boolean xoaSauKhiPhat;
    private PhatTinNhanThoai trinhPhat;
    private CompletableFuture<Void> dangPhat = CompletableFuture.completedFuture(null);
    private int lan;

    PhatThoaiView(String tenNut, Supplier<CompletableFuture<Path>> layTep, boolean xoaSauKhiPhat) {
        this.tenNut = tenNut;
        this.layTep = layTep;
        this.xoaSauKhiPhat = xoaSauKhiPhat;
        nut.setText(tenNut);
        loi.setWrapText(true);
        loi.visibleProperty().bind(loi.textProperty().isNotEmpty());
        loi.managedProperty().bind(loi.visibleProperty());
        setSpacing(4);
        getChildren().addAll(nut, loi);
        nut.setOnAction(e -> {
            if (dangPhat.isDone()) {
                phat();
            } else {
                dung();
            }
        });
        sceneProperty().addListener((o, cu, moi) -> {
            if (moi == null) {
                lan++;
                if (trinhPhat != null) {
                    trinhPhat.close();
                    trinhPhat = null;
                }
                nut.setDisable(false);
                nut.setText(tenNut);
            }
        });
    }

    private void phat() {
        int hienTai = ++lan;
        loi.setText("");
        nut.setDisable(true);
        nut.setText("Đang mở…");
        layTep.get().whenComplete((tep, sai) -> Platform.runLater(() -> {
            if (hienTai != lan || getScene() == null) {
                if (xoaSauKhiPhat) {
                    xoaTepTam(tep);
                }
                return;
            }
            if (sai != null) {
                veKetQua(sai);
                return;
            }
            PhatTinNhanThoai luotPhat = new PhatTinNhanThoai();
            trinhPhat = luotPhat;
            nut.setText("Dừng phát");
            nut.setDisable(false);
            dangPhat = luotPhat.phat(tep);
            dangPhat.whenComplete((x, loiPhat) -> {
                luotPhat.close();
                if (xoaSauKhiPhat) {
                    xoaTepTam(tep);
                }
                Platform.runLater(() -> {
                    if (trinhPhat == luotPhat) {
                        trinhPhat = null;
                    }
                    if (hienTai == lan && getScene() != null) {
                        veKetQua(loiPhat);
                    }
                });
            });
        }));
    }

    CompletableFuture<Void> dung() {
        lan++;
        if (trinhPhat != null) {
            trinhPhat.dung();
        }
        nut.setText(tenNut);
        nut.setDisable(false);
        return dangPhat;
    }

    private void veKetQua(Throwable sai) {
        nut.setText(tenNut);
        nut.setDisable(false);
        if (sai != null && !(sai instanceof CancellationException)) {
            loi.setText(DoHoaView.thongBao(sai));
        }
    }

    static void xoaTepTam(Path tep) {
        if (tep != null) {
            try {
                Files.deleteIfExists(tep);
            } catch (IOException loi) {
                tep.toFile().deleteOnExit();
            }
        }
    }
}
