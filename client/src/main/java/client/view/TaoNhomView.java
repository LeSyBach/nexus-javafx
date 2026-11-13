package client.view;

import client.controller.NhomController;
import client.hatang.KenhTCP;
import client.hatang.XuLyAnh;

import com.google.gson.*;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Tên nhóm và chọn bạn bè, theo luồng CreateGroupScreen của Android NEXUS.
 */
final class TaoNhomView extends BorderPane {
    private final Set<String> daChon = new LinkedHashSet<>();
    private final TextField ten = new TextField();
    private final TextField tim = new TextField();
    private final VBox banBe = new VBox(6);
    private final Label soChon = DoHoaView.nhan("Chưa chọn ai", "nhanHoSo");
    private final Label loi = DoHoaView.nhan("", "loiChat");
    private final Button tao = DoHoaView.nut("Tạo nhóm", "nutHoSoChinh");
    private boolean dangTao, dangXuLyAnh, chiDaChon;
    private String anh = "";
    private final List<String> ganDay;
    private List<JsonObject> danhSachBan = List.of();
    private final NhomController dieuKhien;

    TaoNhomView(KenhTCP kenh, Collection<JsonObject> cacBan, List<String> ganDay, Runnable dong,
                Consumer<JsonObject> khiTao) {
        dieuKhien = new NhomController(kenh);
        this.ganDay = List.copyOf(ganDay);
        setId("taoNhom");
        getStyleClass().addAll("trangHoSo", "hopTaoNhom");
        setPrefSize(610, 690);
        Button thoat = DoHoaView.nut("×", "dongBangThongTin");
        thoat.setAccessibleText("Đóng tạo nhóm");
        Button huy = DoHoaView.nut("Hủy", "nutHoSoPhu");
        thoat.setOnAction(e -> {
            if (coTheDong()) {
                dong.run();
            }
        });
        huy.setOnAction(e -> {
            if (coTheDong()) {
                dong.run();
            }
        });
        BorderPane tieuDe = new BorderPane(DoHoaView.nhan("Tạo nhóm", "tenHoSo"), null, thoat, null, null);
        tieuDe.getStyleClass().add("dauHopTaoNhom");
        Button chonAnh = DoHoaView.nut("", "chonAnhTaoNhom");
        chonAnh.setGraphic(BieuTuongView.mayAnh());
        chonAnh.setAccessibleText("Chọn ảnh nhóm");
        chonAnh.setTooltip(new Tooltip("Chọn ảnh nhóm"));
        chonAnh.setOnAction(e -> chonAnh(chonAnh));
        ten.setId("tenNhomMoi");
        ten.setPromptText("Nhập tên nhóm…");
        ten.setAccessibleText("Tên nhóm");
        ten.setTextFormatter(
                new TextFormatter<String>(doi -> doi.getControlNewText().length() <= 80 ? doi : null));
        HBox hangTen = new HBox(12, chonAnh, ten);
        hangTen.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(ten, Priority.ALWAYS);
        tim.setPromptText("Tìm bạn bè theo tên");
        tim.setAccessibleText("Tìm bạn bè theo tên");
        ToggleGroup boLoc = new ToggleGroup();
        ToggleButton tatCa = new ToggleButton("Tất cả"), daChonNut = new ToggleButton("Đã chọn");
        for (ToggleButton nut : List.of(tatCa, daChonNut)) {
            nut.setToggleGroup(boLoc);
            nut.getStyleClass().add("theBanBe");
        }
        tatCa.setSelected(true);
        tatCa.setOnAction(e -> {
            tatCa.setSelected(true);
            chiDaChon = false;
            veBanBe();
        });
        daChonNut.setOnAction(e -> {
            daChonNut.setSelected(true);
            chiDaChon = true;
            veBanBe();
        });
        VBox nhap = new VBox(12, hangTen, tim, new HBox(8, tatCa, daChonNut));
        nhap.getStyleClass().add("thanNhapTaoNhom");
        setTop(new VBox(tieuDe, nhap));

        banBe.getStyleClass().add("danhSachTaoNhom");
        ScrollPane cuon = new ScrollPane(banBe);
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("cuonTaoNhom");
        setCenter(cuon);

        loi.setWrapText(true);
        loi.visibleProperty().bind(loi.textProperty().isNotEmpty());
        loi.managedProperty().bind(loi.visibleProperty());

        Region khoang = new Region();
        HBox.setHgrow(khoang, Priority.ALWAYS);
        HBox hanhDong = new HBox(10, soChon, khoang, huy, tao);
        hanhDong.setAlignment(Pos.CENTER_LEFT);
        VBox chan = new VBox(8, loi, hanhDong);
        chan.getStyleClass().add("chanHopTaoNhom");
        setBottom(chan);

        tao.setId("xacNhanTaoNhom");
        tim.textProperty().addListener((o, cu, moi) -> veBanBe());
        ten.textProperty().addListener((o, cu, moi) -> capNhatNut());
        tao.setOnAction(e -> {
            if (tao.isDisabled()) {
                return;
            }
            dangTao = true;
            nhap.setDisable(true);
            cuon.setDisable(true);
            huy.setDisable(true);
            thoat.setDisable(true);
            capNhatNut();
            loi.setText("");
            dieuKhien.tao(ten.getText(), daChon, anh)
                    .whenComplete((tra, sai) -> Platform.runLater(() -> {
                        dangTao = false;
                        nhap.setDisable(false);
                        cuon.setDisable(false);
                        huy.setDisable(false);
                        thoat.setDisable(false);
                        capNhatNut();
                        if (sai != null) {
                            loi.setText(DoHoaView.thongBao(sai));
                            return;
                        }
                        if (getScene() != null) {
                            khiTao.accept(tra.getAsJsonObject("nhom"));
                        }
                    }));
        });
        capNhatBanBe(cacBan);
    }

    void capNhatBanBe(Collection<JsonObject> cacBan) {
        danhSachBan = cacBan.stream().sorted(Comparator.comparing(b -> tuKhoa(DuLieu.chu(b, "tenHienThi"))))
                .toList();
        daChon.removeIf(id -> danhSachBan.stream().noneMatch(b -> id.equals(DuLieu.chu(b, "id"))));
        veBanBe();
        capNhatNut();
    }

    boolean coTheDong() {
        return !dangTao;
    }

    private void chonAnh(Button nut) {
        javafx.stage.FileChooser chon = new javafx.stage.FileChooser();
        chon.setTitle("Chọn ảnh nhóm");
        chon.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"));
        var tep = chon.showOpenDialog(getScene().getWindow());
        if (tep == null) {
            return;
        }
        dangXuLyAnh = true;
        nut.setDisable(true);
        capNhatNut();
        XuLyAnh.docTrongNen(tep.toPath()).whenComplete((moi, sai) -> Platform.runLater(() -> {
            if (getScene() == null) {
                return;
            }
            dangXuLyAnh = false;
            nut.setDisable(false);
            capNhatNut();
            if (sai != null) {
                loi.setText(DoHoaView.thongBao(sai));
                return;
            }
            anh = moi;
            loi.setText("");
            nut.setGraphic(DoHoaView.anhHoSo(DuLieu.tao("id", "nhomMoi", "anhDaiDien", anh), 44));
        }));
    }

    private static String tuKhoa(String chu) {
        return java.text.Normalizer.normalize(chu, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').strip();
    }

    private void veBanBe() {
        banBe.getChildren().clear();
        String timKiem = tuKhoa(tim.getText());
        List<JsonObject> hien =
                danhSachBan.stream().filter(b -> tuKhoa(DuLieu.chu(b, "tenHienThi")).contains(timKiem))
                        .filter(b -> !chiDaChon || daChon.contains(DuLieu.chu(b, "id"))).toList();
        Map<String, JsonObject> theoId = new HashMap<>();
        hien.forEach(b -> theoId.put(DuLieu.chu(b, "id"), b));
        if (timKiem.isEmpty() && !chiDaChon) {
            boolean coGanDay = false;
            for (String id : ganDay) {
                JsonObject ban = theoId.remove(id);
                if (ban == null) {
                    continue;
                }
                if (!coGanDay) {
                    themNhan("Trò chuyện gần đây");
                    coGanDay = true;
                }
                themDong(ban);
            }
        }
        String chuTruoc = "";
        for (JsonObject ban : hien) {
            if (!theoId.containsKey(DuLieu.chu(ban, "id"))) {
                continue;
            }
            String ten = tuKhoa(DuLieu.chu(ban, "tenHienThi"));
            String chu = ten.isEmpty() ? "#" : ten.substring(0, 1).toUpperCase(Locale.ROOT);
            if (timKiem.isEmpty() && !chiDaChon && !chu.equals(chuTruoc)) {
                themNhan(chu);
                chuTruoc = chu;
            }
            themDong(ban);
        }
        if (banBe.getChildren().isEmpty()) {
            themNhan(chiDaChon ? "Chưa chọn bạn nào." : danhSachBan.isEmpty()
                    ? "Bạn cần kết bạn trước khi tạo nhóm." : "Không tìm thấy bạn bè.");
        }
    }

    private void themNhan(String ten) {
        banBe.getChildren().add(DoHoaView.nhan(ten, "nhanNhomBan"));
    }

    private void themDong(JsonObject ban) {
        String ten = DuLieu.chu(ban, "tenHienThi"), id = DuLieu.chu(ban, "id");
        CheckBox nut = new CheckBox(ten);
        nut.setId("chonBan" + id);
        nut.setSelected(daChon.contains(id));
        nut.setGraphic(DoHoaView.anh(ten, id, false));
        nut.setMaxWidth(Double.MAX_VALUE);
        nut.getStyleClass().add("chonBanTaoNhom");
        nut.setOnAction(e -> {
            if (!nut.isSelected()) {
                daChon.remove(id);
            } else if (daChon.size() < NhomController.gioiHanBan) {
                daChon.add(id);
            } else {
                nut.setSelected(false);
                loi.setText("Bạn có thể chọn tối đa 29 người.");
            }
            capNhatNut();
            if (chiDaChon && !nut.isSelected()) {
                veBanBe();
            }
        });
        banBe.getChildren().add(nut);
    }

    private void capNhatNut() {
        soChon.setText("Đã chọn " + daChon.size() + " người");
        tao.setDisable(dangTao || dangXuLyAnh || !dieuKhien.coTheTao(ten.getText(), daChon));
    }
}
