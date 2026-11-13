package client.view;

import client.controller.BanBeController;
import client.controller.HoiThoaiController;
import client.controller.AnhTinNhanController;
import client.hatang.TruyenTepTCP;

import com.google.gson.*;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.scene.image.*;

import java.io.ByteArrayInputStream;
import java.util.function.*;

/**
 * Tham khảo ChatInfoScreen, SharedMediaScreen và OtherUserProfileScreen của Android NEXUS.
 */
final class ThongTinChatView {
    static void hoSo(Node neo, BanBeController banBe, String id, BiConsumer<String, String> moChat) {
        VBox noiDung = new VBox(18);
        noiDung.getStyleClass().add("noiDungChiTiet");
        BorderPane trang = taoTrang(noiDung);
        Button dong = nutQuayLai(trang, "Hồ sơ");
        boolean[] daDong = {false}, dangGui = {false};
        Runnable tat = chu(neo).moTrangChiTiet(trang);
        dong.setOnAction(e -> {
            if (!dangGui[0]) {
                daDong[0] = true;
                tat.run();
            }
        });
        Runnable[] tai = {null};
        tai[0] = () -> {
            noiDung.getChildren().setAll(DoHoaView.nhan("Đang tải hồ sơ…", "giaTriHoSo"));
            banBe.hoSoCongKhai(id).whenComplete((tra, sai) -> Platform.runLater(() -> {
                if (daDong[0]) {
                    return;
                }
                if (sai != null) {
                    noiDung.getChildren().setAll(DoHoaView.nhan(DoHoaView.thongBao(sai), "loiChat"));
                    return;
                }
                JsonObject ho = tra.getAsJsonObject("hoSo");
                String ten = DuLieu.chu(ho, "tenHienThi"), quanHe = DuLieu.chu(ho, "quanHe");
                noiDung.getChildren().setAll(dauHoSo(ho));
                HBox thaoTac = new HBox(10);
                thaoTac.setAlignment(Pos.CENTER);
                noiDung.getChildren().add(thaoTac);
                Button nhan = DoHoaView.nut("Nhắn tin", "nutHoSoChinh");
                nhan.setOnAction(e -> {
                    daDong[0] = true;
                    tat.run();
                    moChat.accept(id, ten);
                });
                thaoTac.getChildren().add(nhan);
                Label loi = DoHoaView.nhan("", "loiChat");
                loi.setWrapText(true);
                BiConsumer<String, String> themNut = (nhanNut, ma) -> {
                    Button nut = DoHoaView.nut(nhanNut, "nutHoSoPhu");
                    thaoTac.getChildren().add(nut);
                    Supplier<java.util.concurrent.CompletableFuture<Void>> gui = () -> {
                        dangGui[0] = true;
                        thaoTac.setDisable(true);
                        dong.setDisable(true);
                        return banBe.thayDoiQuanHe(id, ma).handle((k, l) -> {
                            Platform.runLater(() -> {
                                dangGui[0] = false;
                                dong.setDisable(false);
                                thaoTac.setDisable(false);
                                if (l == null) {
                                    tai[0].run();
                                } else {
                                    loi.setText(DoHoaView.thongBao(l));
                                }
                            });
                            if (l != null) {
                                throw new java.util.concurrent.CompletionException(l);
                            }
                            return (Void) null;
                        });
                    };
                    nut.setOnAction(e -> {
                        if (ma.equals("huyBan") || ma.equals("thuHoi")) {
                            ThongBaoNoiView.xacNhan(trang, nhanNut,
                                    "Bạn muốn " + nhanNut.toLowerCase() + " với " + ten + "?", nhanNut, gui);
                        } else {
                            gui.get().exceptionally(l -> null);
                        }
                    });
                };
                switch (quanHe) {
                    case "banBe" -> themNut.accept("Hủy kết bạn", "huyBan");
                    case "daGui" -> themNut.accept("Thu hồi lời mời", "thuHoi");
                    case "daNhan" -> {
                        themNut.accept("Chấp nhận", "chapNhan");
                        themNut.accept("Từ chối", "tuChoi");
                    }
                    default -> themNut.accept("Kết bạn", "gui");
                }
                VBox thongTin = new VBox(10);
                thongTin.getStyleClass().add("theThongTinChat");
                String bio = DuLieu.chu(ho, "gioiThieu");
                if (!bio.isBlank()) {
                    thongTin.getChildren().add(chu(bio));
                }
                for (String truong : java.util.List.of("email", "soDienThoai")) {
                    if (ho.has(truong) && !DuLieu.chu(ho, truong).isBlank()) {
                        thongTin.getChildren().add(chu(
                                (truong.equals("email") ? "Email: " : "Số điện thoại: ") +
                                        DuLieu.chu(ho, truong)));
                    }
                }
                thongTin.getChildren().add(chu("Tham gia: " + DuLieu.chu(ho, "thamGia")));
                noiDung.getChildren().addAll(thongTin, loi);
            }));
        };
        tai[0].run();
    }

    private static Label chu(String noiDung) {
        Label chu = DoHoaView.nhan(noiDung, "giaTriHoSo");
        chu.setWrapText(true);
        chu.setMaxWidth(Double.MAX_VALUE);
        return chu;
    }

    private static VBox dauHoSo(JsonObject ho) {
        String quanHe = switch (DuLieu.chu(ho, "quanHe")) {
            case "banBe" -> "Bạn bè";
            case "daGui" -> "Đã gửi lời mời kết bạn";
            case "daNhan" -> "Đã gửi lời mời cho bạn";
            default -> "Người lạ";
        };
        Label ten = DoHoaView.nhan(DuLieu.chu(ho, "tenHienThi"), "tenHoSo");
        ten.setWrapText(true);
        ten.setAlignment(Pos.CENTER);
        StackPane anh = DoHoaView.anhHoSo(ho, 96);
        anh.setClip(new javafx.scene.shape.Circle(48, 48, 48));
        anh.getStyleClass().add("anhTrangChiTiet");
        VBox dau = new VBox(10, anh, ten, DoHoaView.nhan(quanHe, "nhanHoSo"));
        dau.setAlignment(Pos.CENTER);
        dau.setPadding(new Insets(12));
        return dau;
    }

    private static HoiThoaiView chu(Node neo) {
        for (Node n = neo; n != null; n = n.getParent()) {
            if (n instanceof HoiThoaiView hoiThoai) {
                return hoiThoai;
            }
        }
        throw new IllegalStateException("Trang chi tiết phải nằm trong ứng dụng trò chuyện.");
    }

    private static BorderPane taoTrang(VBox noiDung) {
        noiDung.setMaxWidth(660);
        noiDung.setMinHeight(Region.USE_PREF_SIZE);
        StackPane khung = new StackPane(noiDung);
        khung.setAlignment(Pos.TOP_CENTER);
        ScrollPane cuon = new ScrollPane(khung);
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("cuonHoSo");
        BorderPane trang = new BorderPane(cuon);
        trang.getStyleClass().addAll("trangHoSo", "trangChiTiet");
        return trang;
    }

    private static Button nutQuayLai(BorderPane trang, String ten) {
        Button nut = DoHoaView.nut("", "nutQuayChiTiet");
        nut.setId("quayLaiChiTiet");
        nut.setGraphic(BieuTuongView.quayLai());
        nut.setAccessibleText("Quay lại");
        nut.setTooltip(new Tooltip("Quay lại"));
        HBox dau = new HBox(12, nut, DoHoaView.nhan(ten, "tenHoSo"));
        dau.setAlignment(Pos.CENTER_LEFT);
        dau.getStyleClass().add("dauChiTiet");
        trang.setTop(dau);
        return nut;
    }

    static void mo(Node neo, HoiThoaiController dieuKhien, BanBeController banBe, String dich,
                   AnhTinNhanController dieuKhienAnh, Consumer<JsonObject> tai, Consumer<String> denTin,
                   BiConsumer<String, String> moChat) {
        HoiThoaiView hoiThoai = chu(neo);
        VBox noiDung = new VBox(), dau = new VBox(10);
        dau.getStyleClass().add("dauBangThongTin");
        dau.setAlignment(Pos.CENTER);
        noiDung.getChildren().add(dau);
        ScrollPane cuon = new ScrollPane(noiDung);
        cuon.setFitToWidth(true);
        cuon.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        cuon.getStyleClass().add("cuonBangThongTin");
        BorderPane trang = new BorderPane(cuon);
        trang.setId("thongTinHoiThoai");
        trang.getStyleClass().add("bangThongTin");
        trang.prefWidthProperty().bind(javafx.beans.binding.Bindings.min(392,
                javafx.beans.binding.Bindings.max(320, hoiThoai.widthProperty().multiply(0.33))));
        trang.setMinWidth(320);
        Label tenBang = DoHoaView.nhan("Thông tin hội thoại", "tenBangThongTin");
        Button dong = DoHoaView.nut("×", "dongBangThongTin");
        dong.setAccessibleText("Đóng thông tin hội thoại");
        BorderPane thanh = new BorderPane(tenBang, null, dong, null, null);
        thanh.getStyleClass().add("thanhBangThongTin");
        trang.setTop(thanh);
        Runnable tat = hoiThoai.moBangThongTin(trang);
        dong.setOnAction(e -> tat.run());

        Button chan = DoHoaView.nut("Chặn", "nutThaoTacNguyHiem");
        chan.setDisable(true);
        if (dich == null) {
            dau.getChildren().add(DoHoaView.nhan("Phòng chat chung", "tenHoSo"));
        } else {
            dau.getChildren().add(chu("Đang tải…"));
            banBe.hoSoCongKhai(dich).whenComplete((tra, sai) -> Platform.runLater(() -> {
                if (sai != null) {
                    dau.getChildren().setAll(chu(DoHoaView.thongBao(sai)));
                    return;
                }
                JsonObject ho = tra.getAsJsonObject("hoSo");
                Button hoSo = DoHoaView.nut("Trang cá nhân", "nutHoSoPhu");
                hoSo.setOnAction(e -> hoSo(trang, banBe, dich, moChat));
                dau.getChildren().setAll(dauHoSo(ho), hoSo);
                boolean daChan = ho.has("daChan") && ho.get("daChan").getAsBoolean();
                datNutChan(trang, dieuKhien, dich, chan, daChan);
                chan.setDisable(false);
            }));
        }

        themMuc(noiDung, trang, dieuKhien, dich, dieuKhienAnh, tai, denTin, "anh", "Ảnh", 8);
        themMuc(noiDung, trang, dieuKhien, dich, dieuKhienAnh, tai, denTin, "tepRieng", "Tệp", 3);
        themMuc(noiDung, trang, dieuKhien, dich, dieuKhienAnh, tai, denTin, "lienKet", "Liên kết", 3);
        if (dich != null) {
            Button xoa = DoHoaView.nut("Xóa cuộc trò chuyện", "nutThaoTacNguyHiem");
            xoa.setOnAction(e -> ThongBaoNoiView.xacNhan(trang, "Xóa cuộc trò chuyện",
                    "Lịch sử hiện tại sẽ bị xóa khỏi hộp thư của bạn. Người kia vẫn thấy tin nhắn của họ.",
                    "Xóa",
                    () -> dieuKhien.xoa(dich).thenApply(tra -> null)));
            VBox thaoTac = new VBox(4, chan, xoa);
            thaoTac.getStyleClass().add("cuoiBangThongTin");
            chan.setMaxWidth(Double.MAX_VALUE);
            xoa.setMaxWidth(Double.MAX_VALUE);
            noiDung.getChildren().add(thaoTac);
        }
    }

    private static void datNutChan(Node neo, HoiThoaiController dieuKhien, String dich, Button nut,
                                   boolean daChan) {
        nut.setText(daChan ? "Bỏ chặn" : "Chặn");
        nut.setOnAction(e -> {
            if (daChan) {
                nut.setDisable(true);
                dieuKhien.chan(dich, false)
                        .whenComplete((tra, sai) -> Platform.runLater(() -> {
                            nut.setDisable(false);
                            if (sai == null) {
                                datNutChan(neo, dieuKhien, dich, nut, false);
                            } else {
                                ThongBaoNoiView.xacNhan(neo, "Không thể bỏ chặn", DoHoaView.thongBao(sai),
                                        "Đóng",
                                        () -> java.util.concurrent.CompletableFuture.completedFuture(null));
                            }
                        }));
            } else {
                ThongBaoNoiView.xacNhan(neo, "Chặn người dùng",
                        "Hai tài khoản sẽ không thể gửi tin nhắn riêng cho nhau cho đến khi bỏ chặn.", "Chặn",
                        () -> dieuKhien.chan(dich, true).thenApply(tra -> {
                            Platform.runLater(() -> datNutChan(neo, dieuKhien, dich, nut, true));
                            return null;
                        }));
            }
        });
    }

    private static void themMuc(VBox noiDung, Node neo, HoiThoaiController dieuKhien, String dich,
                                AnhTinNhanController dieuKhienAnh,
                                Consumer<JsonObject> tai, Consumer<String> denTin, String loai, String ten,
                                int gioiHan) {
        VBox trong = new VBox(10);
        trong.getChildren().add(chu("Đang tải…"));
        TitledPane muc = new TitledPane(ten, trong);
        muc.setAnimated(false);
        muc.getStyleClass().add("mucBangThongTin");
        noiDung.getChildren().add(muc);
        Button xem = DoHoaView.nut("Xem tất cả", "nutXemTatCa");
        xem.setMaxWidth(Double.MAX_VALUE);
        xem.setAccessibleText("Xem tất cả " + ten.toLowerCase(java.util.Locale.ROOT));
        xem.setOnAction(e -> moKho(neo, dieuKhien, dich, dieuKhienAnh, tai, denTin, loai));

        dieuKhien.thongTin(dich, loai, null).whenComplete((tra, sai) -> Platform.runLater(() -> {
            trong.getChildren().clear();
            if (sai != null) {
                trong.getChildren().add(chu(DoHoaView.thongBao(sai)));
                return;
            }
            JsonArray danhSach = tra.getAsJsonArray("tinNhan");
            if (danhSach.isEmpty()) {
                trong.getChildren().add(DoHoaView.nhan(switch (loai) {
                    case "anh" -> "Chưa có ảnh.";
                    case "tepRieng" -> "Chưa có tệp.";
                    default -> "Chưa có liên kết.";
                }, "chuTrongChiaSe"));
                return;
            }
            if (loai.equals("anh")) {
                GridPane luoi = new GridPane();
                luoi.setHgap(7);
                luoi.setVgap(7);
                for (int i = 0; i < 4; i++) {
                    ColumnConstraints cot = new ColumnConstraints();
                    cot.setPercentWidth(25);
                    luoi.getColumnConstraints().add(cot);
                }
                for (int i = 0; i < Math.min(gioiHan, danhSach.size()); i++) {
                    JsonObject tin = danhSach.get(i).getAsJsonObject();
                    luoi.add(oAnh(tin, dieuKhienAnh, denTin), i % 4, i / 4);
                }
                trong.getChildren().add(luoi);
            } else {
                for (int i = 0; i < Math.min(gioiHan, danhSach.size()); i++) {
                    trong.getChildren().add(dongChiaSe(danhSach.get(i).getAsJsonObject(), loai, denTin));
                }
            }
            trong.getChildren().add(xem);
        }));
    }

    private static Button oAnh(JsonObject tin, AnhTinNhanController dieuKhienAnh, Consumer<String> denTin) {
        return oAnh(tin, dieuKhienAnh, denTin, 66, "oAnhThongTin");
    }

    private static Button oAnh(JsonObject tin, AnhTinNhanController dieuKhienAnh, Consumer<String> denTin, int cao,
                               String lop) {
        String id = DuLieu.chu(tin, "id");
        Button nut = DoHoaView.nut("", lop);
        nut.setMaxWidth(Double.MAX_VALUE);
        nut.setMinWidth(0);
        nut.setPrefHeight(cao);
        nut.setMaxHeight(cao);
        nut.setGraphic(BieuTuongView.anh());
        nut.setAccessibleText("Xem ảnh trong hội thoại");
        nut.setOnAction(e -> denTin.accept(id));
        dieuKhienAnh.tai(id).whenComplete((bytes, sai) -> Platform.runLater(() -> {
            if (sai != null) {
                return;
            }
            ImageView anh = new ImageView(new Image(new ByteArrayInputStream(bytes)));
            anh.setPreserveRatio(true);
            anh.setFitHeight(cao - 12);
            anh.fitWidthProperty()
                    .bind(javafx.beans.binding.Bindings.max(1, nut.widthProperty().subtract(12)));
            nut.setGraphic(anh);
        }));
        return nut;
    }

    private static Button dongChiaSe(JsonObject tin, String loai, Consumer<String> denTin) {
        boolean laTep = loai.equals("tepRieng");
        String tieuDe, moTa;
        if (laTep) {
            JsonObject tep = tin.getAsJsonObject("tep");
            tieuDe = DuLieu.chu(tep, "tenTep");
            moTa = TruyenTepTCP.dungLuong(tep.get("kichThuoc").getAsLong());
        } else {
            String url = DuLieu.chu(tin, "lienKet");
            if (url.isEmpty()) {
                var tim = java.util.regex.Pattern.compile("https?://[^\\s<>]+",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(DuLieu.chu(tin, "noiDung"));
                url = tim.find() ? tim.group() : DuLieu.chu(tin, "noiDung");
            }
            tieuDe = url;
            moTa = "";
            try {
                String mayChu = java.net.URI.create(url).getHost();
                if (mayChu != null) {
                    moTa = mayChu;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        Label ten = DoHoaView.nhan(tieuDe, "tenNoiDungChiaSe");
        ten.setMaxWidth(Double.MAX_VALUE);
        ten.setMinWidth(0);
        Label chiTiet = DoHoaView.nhan(moTa, "chuTrongChiaSe");
        chiTiet.setMinWidth(0);
        VBox chu = new VBox(4, ten, chiTiet);
        chu.setMinWidth(0);
        HBox.setHgrow(chu, Priority.ALWAYS);
        String ngay = "";
        try {
            ngay = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.parse(DuLieu.chu(tin, "taoLuc")));
        } catch (java.time.DateTimeException ignored) {
        }
        Node icon = laTep ? BieuTuongView.kepGiay() : BieuTuongView.lienKet();
        StackPane nenIcon = new StackPane(icon);
        nenIcon.getStyleClass().add("nenIconChiaSe");
        HBox dong = new HBox(9, nenIcon, chu, DoHoaView.nhan(ngay, "chuTrongChiaSe"));
        dong.setAlignment(Pos.CENTER_LEFT);
        dong.setFillHeight(false);
        Button nut = DoHoaView.nut("", "dongNoiDungChiaSe");
        nut.setGraphic(dong);
        nut.setMaxWidth(Double.MAX_VALUE);
        nut.setMinHeight(53);
        nut.setPrefHeight(53);
        nut.setMaxHeight(53);
        nut.setAccessibleText(tieuDe);
        nut.setTooltip(new Tooltip(tieuDe));
        nut.setOnAction(e -> denTin.accept(DuLieu.chu(tin, "id")));
        return nut;
    }

    private static void moKho(Node neo, HoiThoaiController dieuKhien, String dich, AnhTinNhanController dieuKhienAnh,
                              Consumer<JsonObject> tai, Consumer<String> denTin, String loai) {
        boolean laAnh = loai.equals("anh");
        String ten = switch (loai) {
            case "anh" -> "Ảnh đã chia sẻ";
            case "tepRieng" -> "Tệp đã chia sẻ";
            default -> "Liên kết đã chia sẻ";
        };
        VBox noiDung = new VBox(14);
        noiDung.getStyleClass().add("noiDungChiTiet");
        BorderPane trang = taoTrang(noiDung);
        trang.setId("khoChiaSe");
        if (laAnh) {
            noiDung.setMaxWidth(1000);
        }
        Button quay = nutQuayLai(trang, ten);
        boolean[] daDong = {false};
        String[] truoc = {null};
        GridPane luoi = new GridPane();
        luoi.setId("luoiAnhChiaSe");
        luoi.setHgap(12);
        luoi.setVgap(12);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cot = new ColumnConstraints();
            cot.setPercentWidth(25);
            luoi.getColumnConstraints().add(cot);
        }
        VBox danhSach = new VBox(8);
        Label thongBao = chu("Đang tải…");
        thongBao.managedProperty().bind(thongBao.visibleProperty());
        Button tiep = DoHoaView.nut("Xem thêm", "nutHoSoPhu");
        tiep.setVisible(false);
        tiep.managedProperty().bind(tiep.visibleProperty());
        noiDung.getChildren().addAll(thongBao, laAnh ? luoi : danhSach, tiep);
        Runnable tat = chu(neo).moTrangChiTiet(trang);
        quay.setOnAction(e -> {
            daDong[0] = true;
            tat.run();
        });
        Consumer<String> xem = id -> {
            daDong[0] = true;
            denTin.accept(id);
        };
        Runnable taiTrang = () -> {
            tiep.setDisable(true);
            dieuKhien.thongTin(dich, loai, truoc[0])
                    .whenComplete((tra, sai) -> Platform.runLater(() -> {
                        if (daDong[0]) {
                            return;
                        }
                        tiep.setDisable(false);
                        if (sai != null) {
                            thongBao.setText(DoHoaView.thongBao(sai));
                            thongBao.setVisible(true);
                            tiep.setText("Thử lại");
                            tiep.setVisible(true);
                            return;
                        }
                        JsonArray ds = tra.getAsJsonArray("tinNhan");
                        for (JsonElement phanTu : ds) {
                            JsonObject tin = phanTu.getAsJsonObject();
                            truoc[0] = DuLieu.chu(tin, "id");
                            if (laAnh) {
                                int viTri = luoi.getChildren().size();
                                luoi.add(oAnh(tin, dieuKhienAnh, xem, 148, "oAnhChiaSe"), viTri % 4, viTri / 4);
                            } else {
                                Button dong = dongChiaSe(tin, loai, xem);
                                HBox hang = new HBox(8, dong);
                                hang.setAlignment(Pos.CENTER_LEFT);
                                HBox.setHgrow(dong, Priority.ALWAYS);
                                if (tin.has("tep")) {
                                    Button luu = DoHoaView.nut("Tải xuống", "nutHoSoPhu");
                                    luu.setOnAction(e -> tai.accept(tin));
                                    hang.getChildren().add(luu);
                                }
                                danhSach.getChildren().add(hang);
                            }
                        }
                        boolean rong = truoc[0] == null;
                        thongBao.setText(switch (loai) {
                            case "anh" -> "Chưa có ảnh.";
                            case "tepRieng" -> "Chưa có tệp.";
                            default -> "Chưa có liên kết.";
                        });
                        thongBao.setVisible(rong);
                        tiep.setText("Xem thêm");
                        tiep.setVisible(ds.size() == 20);
                    }));
        };
        tiep.setOnAction(e -> taiTrang.run());
        taiTrang.run();
    }
}
