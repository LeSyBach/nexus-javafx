package client.view;

import client.controller.ChiaSeController;
import client.hatang.KenhTCP;
import client.controller.AnhTinNhanController;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Chọn nhiều cuộc trò chuyện, xem trước nội dung và chia sẻ trong cùng cửa sổ.
 */
final class ChiaSeView extends VBox {
    private final ChiaSeController dieuKhien;
    private final JsonObject tin;
    private final List<JsonObject> ganDay = new ArrayList<>();
    private final List<JsonObject> banBe = new ArrayList<>();
    private final Map<String, JsonObject> daChon = new LinkedHashMap<>();
    private final Set<String> daGui = new HashSet<>();
    private final VBox cacDong = new VBox(4);
    private final TextField tim = new TextField();
    private final TextArea loiNhan = new TextArea();
    private final Label bao = DoHoaView.nhan("", "loiChat");
    private final Button chiaSe = DoHoaView.nut("Chia sẻ", "nutBanBeChinh");
    private final Button huy = DoHoaView.nut("Hủy", "nutHoSoPhu");
    private final Button dong = DoHoaView.nut("×", "nutBoTraLoi");
    private final HBox cacTab = new HBox(8);
    private String tab = "ganDay";
    private boolean dangGui;
    private Runnable khiDong = () -> {
    };
    private final Runnable khiDaGui;

    ChiaSeView(KenhTCP kenh, JsonObject tin, AnhTinNhanController dieuKhienAnh, Map<String, JsonObject> hoiThoai,
               Map<String, JsonObject> ban, Runnable khiDaGui) {
        dieuKhien = new ChiaSeController(kenh);
        this.tin = tin.deepCopy();
        this.khiDaGui = khiDaGui;

        chiaSe.setId("guiChiaSe");
        setId("manHinhChiaSe");
        setSpacing(14);
        setPrefWidth(620);
        setPrefHeight(680);
        getStyleClass().add("hopChiaSe");

        Comparator<JsonObject> theoMaTinCuoi = Comparator.comparingLong(hoiThoaiDaLuu -> {
            JsonObject tinCuoi = hoiThoaiDaLuu.getAsJsonObject("tinCuoi");
            return Long.parseLong(DuLieu.chu(tinCuoi, "id"));
        });
        hoiThoai.values().stream()
                .sorted(theoMaTinCuoi.reversed())
                .forEach(hoiThoaiDaLuu -> ganDay.add(DuLieu.tao(
                        "id", DuLieu.chu(hoiThoaiDaLuu, "id"),
                        "tenHienThi", DuLieu.chu(hoiThoaiDaLuu, "tenHienThi")
                )));
        banBe.addAll(ban.values());
        banBe.sort(Comparator.comparing(h -> DuLieu.chu(h, "tenHienThi")));

        Label ten = DoHoaView.nhan("Chia sẻ", "tenHoSo");
        Region rong = new Region();
        HBox.setHgrow(rong, Priority.ALWAYS);
        HBox dau = new HBox(ten, rong, dong);
        dau.setAlignment(Pos.CENTER_LEFT);

        tim.setPromptText("Tìm cuộc trò chuyện hoặc bạn bè…");
        tim.setId("timNguoiChiaSe");
        tim.textProperty().addListener((o, cu, moi) -> veDanhSach());

        themTab("Gần đây", "ganDay");
        themTab("Bạn bè", "banBe");
        themTab("Phòng chung", "phong");

        ScrollPane cuon = new ScrollPane(cacDong);
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("cuonBanBe");
        cuon.setMinHeight(100);
        VBox.setVgrow(cuon, Priority.ALWAYS);

        Label tieuDeXemTruoc = DoHoaView.nhan("Chia sẻ tin nhắn", "tenTrichDan");
        TrichDanTinView trichDan = new TrichDanTinView(tin, dieuKhienAnh);
        VBox xemTruoc = new VBox(7, tieuDeXemTruoc, trichDan);
        xemTruoc.getStyleClass().add("xemTruocChiaSe");

        loiNhan.setPromptText("Nhập lời nhắn kèm theo (không bắt buộc)…");
        loiNhan.setWrapText(true);
        loiNhan.setPrefRowCount(2);
        loiNhan.setPrefHeight(64);
        loiNhan.setMaxHeight(64);
        loiNhan.setId("loiNhanChiaSe");
        loiNhan.textProperty().addListener((o, cu, moi) -> capNhatNut());

        bao.setWrapText(true);
        bao.setVisible(false);
        bao.managedProperty().bind(bao.visibleProperty());

        HBox chan = new HBox(12, huy, chiaSe);
        chan.setAlignment(Pos.CENTER_RIGHT);
        huy.setOnAction(suKien -> dong());
        dong.setOnAction(suKien -> dong());
        chiaSe.setOnAction(suKien -> gui());

        getChildren().addAll(dau, tim, cacTab, cuon, xemTruoc, loiNhan, bao, chan);
        veDanhSach();
        capNhatNut();
    }

    void mo(Node neo) {
        khiDong = ThongBaoNoiView.mo(neo, this, () -> !dangGui);
        Platform.runLater(tim::requestFocus);
    }

    private void dong() {
        if (!dangGui) {
            khiDong.run();
        }
    }

    private void themTab(String ten, String ma) {
        ToggleButton nut = new ToggleButton(ten);
        nut.getStyleClass().add("tabChiaSe");
        nut.setUserData(ma);
        nut.setOnAction(suKien -> {
            tab = ma;
            veDanhSach();
        });
        cacTab.getChildren().add(nut);
    }

    private void veDanhSach() {
        for (Node nut : cacTab.getChildren()) {
            ((ToggleButton) nut).setSelected(nut.getUserData().equals(tab));
        }

        cacDong.getChildren().clear();
        List<JsonObject> nguon = tab.equals("ganDay") ? ganDay : tab.equals("banBe") ? banBe :
                List.of(DuLieu.tao("id", "0", "tenHienThi", "Phòng chat chung"));
        String chu = tim.getText().strip().toLowerCase(Locale.ROOT);
        for (JsonObject nguoi : nguon) {
            String id = DuLieu.chu(nguoi, "id");
            String ten = DuLieu.chu(nguoi, "tenHienThi");
            if (!ten.toLowerCase(Locale.ROOT).contains(chu)) {
                continue;
            }

            CheckBox chon = new CheckBox();
            chon.setSelected(daChon.containsKey(id));
            chon.setDisable(daGui.contains(id));
            chon.setAccessibleText("Chọn " + ten);
            chon.selectedProperty().addListener((o, cu, moi) -> {
                if (moi) {
                    daChon.put(id, nguoi);
                } else {
                    daChon.remove(id);
                }
                capNhatNut();
            });

            Label nhan = DoHoaView.nhan(ten + (daGui.contains(id) ? " · Đã chia sẻ" : ""), "tenBanBe");
            nhan.setWrapText(true);
            nhan.setMaxWidth(420);
            HBox hang = new HBox(14, chon, DoHoaView.anh(ten, id, false), nhan);
            hang.setAlignment(Pos.CENTER_LEFT);
            hang.getStyleClass().add("dongChiaSe");
            hang.setOnMouseClicked(suKien -> {
                if (!dangGui && !chon.isDisabled() && !(suKien.getTarget() instanceof CheckBox)) {
                    chon.setSelected(!chon.isSelected());
                }
            });
            // CheckBox tự xử lý click; không để sự kiện nổi lên chọn hai lần.
            chon.setOnMouseClicked(suKien -> suKien.consume());
            cacDong.getChildren().add(hang);
        }
        if (cacDong.getChildren().isEmpty()) {
            cacDong.getChildren().add(DoHoaView.nhan("Không có cuộc trò chuyện phù hợp.", "chuGoiY"));
        }
    }

    private void capNhatNut() {
        chiaSe.setText("Chia sẻ" + (daChon.isEmpty() ? "" : " (" + daChon.size() + ")"));
        chiaSe.setDisable(dangGui || daChon.isEmpty() || loiNhan.getText().length() > 4000);
    }

    private void gui() {
        if (dangGui || daChon.isEmpty() || loiNhan.getText().length() > 4000) {
            return;
        }
        dangGui = true;
        capNhatNut();
        huy.setDisable(true);
        dong.setDisable(true);
        tim.setDisable(true);
        cacDong.setDisable(true);
        cacTab.setDisable(true);
        loiNhan.setDisable(true);
        bao.setVisible(false);
        dieuKhien.gui(DuLieu.chu(tin, "id"), daChon.keySet(), loiNhan.getText().strip())
                .whenComplete((ketQua, sai) -> Platform.runLater(() -> {
                    List<String> thanhCong = sai == null ? ketQua.thanhCong() : List.of();
                    List<Throwable> thatBai = sai == null ? ketQua.thatBai() : List.of(sai);
                    dangGui = false;
                    thanhCong.forEach(id -> {
                        daChon.remove(id);
                        daGui.add(id);
                    });
                    if (!thanhCong.isEmpty()) {
                        khiDaGui.run();
                    }
                    if (thatBai.isEmpty()) {
                        khiDong.run();
                        return;
                    }
                    huy.setDisable(false);
                    dong.setDisable(false);
                    tim.setDisable(false);
                    cacDong.setDisable(false);
                    cacTab.setDisable(false);
                    loiNhan.setDisable(false);
                    veDanhSach();
                    capNhatNut();
                    bao.setText(
                            "Đã chia sẻ " + thanhCong.size() + " nơi. " + DoHoaView.thongBao(thatBai.get(0)) +
                                    " Có thể thử lại các nơi còn chọn.");
                    bao.setVisible(true);
                }));
    }
}
