package client.view;

import client.hatang.GhiAmTinNhan;
import client.hatang.GhiAmTinNhan.BanGhi;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Ghi âm và nghe lại bản nháp trong ô soạn tin. */
final class GhiAmView extends VBox {
    private final Function<Path, CompletableFuture<Void>> guiTin;
    private final Label trangThai = DoHoaView.nhan("", "giaTriHoSo");
    private final Label loi = DoHoaView.nhan("", "loiChat");
    private final Button dung = DoHoaView.nut("Dừng", "nutHoSoPhu");
    private final Button bo = DoHoaView.nut("Hủy", "nutHoSoPhu");
    private final Button gui = DoHoaView.nut("Gửi đoạn thoại", "nutHoSoChinh");
    private final HBox thaoTac = new HBox(8);
    private final Timeline dongHo = new Timeline(new KeyFrame(Duration.millis(250), e -> hienThoiGian()));
    private GhiAmTinNhan ghiAm;
    private BanGhi banGhi;
    private PhatThoaiView ngheLai;
    private CompletableFuture<Void> dangGui;
    private long batDau;

    GhiAmView(Function<Path, CompletableFuture<Void>> guiTin) {
        this.guiTin = guiTin;
        setSpacing(6);
        getStyleClass().add("vungTinThoai");
        trangThai.setWrapText(true);
        loi.setWrapText(true);
        loi.visibleProperty().bind(loi.textProperty().isNotEmpty());
        loi.managedProperty().bind(loi.visibleProperty());
        thaoTac.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(trangThai, thaoTac, loi);
        managedProperty().bind(visibleProperty());
        setVisible(false);
        dongHo.setCycleCount(Animation.INDEFINITE);
        dung.setOnAction(e -> dungGhi());
        bo.setOnAction(e -> huy());
        gui.setOnAction(e -> guiBanGhi());
        sceneProperty().addListener((o, cu, moi) -> {
            if (moi == null) {
                huy();
            }
        });
    }

    boolean dangGhi() {
        return ghiAm != null;
    }

    void batDau() {
        if (ghiAm != null || banGhi != null || dangGui != null) {
            return;
        }
        setVisible(true);
        trangThai.setText("Đang mở micro…");
        loi.setText("");
        bo.setText("Hủy");
        dung.setDisable(true);
        thaoTac.getChildren().setAll(dung, bo);
        GhiAmTinNhan luot = GhiAmTinNhan.batDau();
        ghiAm = luot;
        luot.khiSanSang().thenRun(() -> Platform.runLater(() -> {
            if (ghiAm == luot) {
                batDau = System.nanoTime();
                hienThoiGian();
                dongHo.playFromStart();
                dung.setDisable(false);
            }
        }));
        luot.ketQua().whenComplete((ketQua, sai) -> Platform.runLater(() -> {
            if (ghiAm != luot || getScene() == null) {
                if (ketQua != null) {
                    PhatThoaiView.xoaTepTam(ketQua.tep());
                }
                return;
            }
            ghiAm = null;
            dongHo.stop();
            if (sai == null) {
                veBanGhi(ketQua);
            } else {
                trangThai.setText("Chưa ghi được đoạn thoại");
                loi.setText(DoHoaView.thongBao(sai));
                thaoTac.getChildren().setAll(bo);
            }
        }));
    }

    private void dungGhi() {
        if (ghiAm != null) {
            ghiAm.dung();
            dongHo.stop();
            trangThai.setText("Đang chuẩn bị đoạn thoại…");
            dung.setDisable(true);
        }
    }

    private void hienThoiGian() {
        long giay = Math.min(GhiAmTinNhan.GIOI_HAN_GIAY, (System.nanoTime() - batDau) / 1_000_000_000);
        trangThai.setText("Đang ghi âm · " + dongHo(giay) + " / 01:00");
    }

    private void veBanGhi(BanGhi moi) {
        banGhi = moi;
        trangThai.setText("Tin nhắn thoại · " + dongHo((moi.miliGiay() + 999) / 1000));
        ngheLai = new PhatThoaiView("Nghe lại", () -> CompletableFuture.completedFuture(moi.tep()), false);
        bo.setText("Bỏ đoạn ghi âm");
        gui.setDisable(false);
        ngheLai.setMinWidth(Region.USE_PREF_SIZE);
        thaoTac.getChildren().setAll(ngheLai, bo, gui);
    }

    private void guiBanGhi() {
        if (banGhi == null || dangGui != null) {
            return;
        }
        BanGhi banGui = banGhi;
        CompletableFuture<Void> daDungPhat = ngheLai.dung();
        ngheLai.setDisable(true);
        loi.setText("");
        gui.setDisable(true);
        bo.setDisable(true);
        dangGui = guiTin.apply(banGui.tep());
        dangGui.whenComplete((x, sai) -> Platform.runLater(() -> {
            dangGui = null;
            if (banGhi != banGui) {
                daDungPhat.whenComplete((y, loiPhat) -> PhatThoaiView.xoaTepTam(banGui.tep()));
            } else if (sai == null) {
                huy();
            } else {
                loi.setText(DoHoaView.thongBao(sai));
                ngheLai.setDisable(false);
                gui.setDisable(false);
                bo.setDisable(false);
            }
        }));
    }

    void huy() {
        dongHo.stop();
        if (ghiAm != null) {
            GhiAmTinNhan cu = ghiAm;
            ghiAm = null;
            cu.close();
        }
        if (banGhi != null && dangGui == null) {
            Path tep = banGhi.tep();
            ngheLai.dung().whenComplete((x, sai) -> PhatThoaiView.xoaTepTam(tep));
        }
        banGhi = null;
        ngheLai = null;
        thaoTac.getChildren().clear();
        bo.setDisable(false);
        setVisible(false);
    }

    private static String dongHo(long giay) {
        return String.format(Locale.ROOT, "%02d:%02d", giay / 60, giay % 60);
    }
}
