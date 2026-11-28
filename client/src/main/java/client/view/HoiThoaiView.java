package client.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.binding.BooleanBinding;
import client.controller.BanBeController;
import client.controller.GoiThoaiController;
import client.controller.HoiThoaiController;
import client.controller.NhomController;
import client.controller.TaiKhoanController;
import client.hatang.KenhTCP;
import client.controller.AnhTinNhanController;
import client.model.DanhSachHoiThoaiModel;
import client.model.LichSuTinNhanModel;
import client.hatang.TruyenTepTCP;

import com.google.gson.*;
import giaothuc.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class HoiThoaiView extends BorderPane {
    private final KenhTCP kenh;
    private final HoiThoaiController dieuKhien;
    private final GoiThoaiController dieuKhienGoiThoai;
    private final GoiThoaiView cuaSoGoiThoai = new GoiThoaiView(this);
    private final Button nutGoiThoai = DoHoaView.nut("Gọi thoại", "nutHoSoPhu");
    private final Button nutGoiVideo = DoHoaView.nut("Gọi video", "nutHoSoPhu");
    private final BooleanProperty dangCoCuocGoi = new SimpleBooleanProperty();
    private final DanhSachHoiThoaiModel danhSachModel = new DanhSachHoiThoaiModel();
    private final LichSuTinNhanModel lichSuModel = new LichSuTinNhanModel();
    private final TaiKhoanController dieuKhienTaiKhoan;
    private final BanBeController dieuKhienBanBe;
    private final NhomController dieuKhienNhom;
    private java.nio.file.Path tepDangChon;
    private final Label tenTepChon = DoHoaView.nhan("", "tenTepChon");
    private final javafx.scene.image.ImageView anhTepChon = new javafx.scene.image.ImageView();
    private java.nio.file.Path tepDaXem;
    private final HBox vungTepChon = new HBox(10);
    private final ProgressBar tienTrinhTep = new ProgressBar(0);
    private final Label trangThaiTep = DoHoaView.nhan("", "trangThaiTep");
    private final HBox vungTruyenTep = new HBox(10);
    private final AnhTinNhanController dieuKhienAnh;
    private final BanBeView hopBanBe;
    private boolean xemLuuTru;
    private final Button nutLuuTru = DoHoaView.nut("Lưu trữ", "nutTabTrai");
    private final Button nutTinNhan = DoHoaView.nut("Tin nhắn", "nutTabTrai");
    private final Label demLoiMoi = DoHoaView.nhan("", "demLoiMoi");
    private final Button nutBanBe = DoHoaView.nut("Bạn bè", "nutMoBanBe");
    private final JsonObject nguoiDung;
    private final String idCuaMinh;
    private final Runnable khiDangXuat;
    private final Runnable khiChuyenTaiKhoan;
    private final java.util.function.Consumer<JsonObject> khiCapNhatHoSo;
    private HoSoView manHinhHoSo;
    private final BorderPane vungChat = new BorderPane();
    private final VBox oSoan;
    private final ObjectProperty<TrangThaiGui> trangThaiGui = new SimpleObjectProperty<>(TrangThaiGui.binhThuong);
    private int lanTaiHoSoDoiPhuong;
    private final Button nutHoSo = DoHoaView.nut("", "nutAnhHoSo");
    private VBox cotHoiThoai;
    private final ListView<JsonObject> danhSach = new ListView<>();
    private final TextField timKiem = new TextField();
    private final Label tieuDe = DoHoaView.nhan("Phòng chat chung", "tenHoiThoai");
    private final Label moTa = DoHoaView.nhan("Một nơi để bắt đầu những câu chuyện", "moTaHoiThoai");
    private final Label soNguoi = DoHoaView.nhan("ĐANG KẾT NỐI", "nhanNho");
    private final Label loi = DoHoaView.nhan("", "loiChat");
    private final VBox cacTin = new VBox(15);
    private final ScrollPane lichSu = new ScrollPane(cacTin);
    private final TextArea noiDung = new TextArea();
    private final Label demKyTu = DoHoaView.nhan("0 / 4000", "chuGoiY");
    private final Button gui = DoHoaView.nut("Gửi  ↗", "nutGui");
    private final GhiAmView ghiAm = new GhiAmView(this::guiTinThoai);
    private final Button taiTinCu = DoHoaView.nut("Xem tin nhắn trước", "nutTaiTinCu");
    private JsonObject tinDangTraLoi;
    private final HBox vungTraLoi = new HBox(12);
    private final Label chuTraLoi = DoHoaView.nhan("", "chuTraLoi");
    private final Set<String> dangThaoTacTin = new HashSet<>();
    private ChatNhomView chatNhom;
    private TaoNhomView hopTaoNhom;
    private final Button phong = DoHoaView.nut("#   Phòng chat chung", "nutPhongChung");
    private boolean dangCapNhatDanhSach;
    private boolean dangTaiLichSu;
    private String doiPhuong;
    private int lanTai;
    private javafx.stage.Window cuaSo;
    private final javafx.beans.value.ChangeListener<Boolean> khiDoiTieuDiem = (giaTri, cu, moi) -> {
        if (moi) {
            danhDauDangDoc();
        }
    };
    private boolean daDong;
    private boolean dangGui;
    private boolean oCuoiKhiMoHoSo;
    private final StackPane anhDauChat = new StackPane();
    private final HBox thanhGhim = new HBox(10);
    private String tinCanTim;
    private int lanTaiGhim;
    private final Map<String, TinDaVe> tinDaVe = new HashMap<>();
    private final Map<String, javafx.scene.Node> hangDaVe = new HashMap<>();
    private final Button veCuoi = DoHoaView.nut("", "nutVeCuoi");

    private record TinDaVe(JsonObject duLieu, boolean daXem) {
    }

    private enum TrangThaiGui {
        binhThuong, dangTai, chuaTaiDuoc, daChan, biChan
    }

    public HoiThoaiView(KenhTCP kenh, JsonObject nguoiDung, Runnable khiDangXuat) {
        this(kenh, nguoiDung, khiDangXuat, () -> {
        }, hoSo -> {
        });
    }

    public HoiThoaiView(KenhTCP kenh, JsonObject nguoiDung, Runnable khiDangXuat, Runnable khiChuyenTaiKhoan,
                        java.util.function.Consumer<JsonObject> khiCapNhatHoSo) {
        this.khiChuyenTaiKhoan = khiChuyenTaiKhoan;
        this.khiCapNhatHoSo = khiCapNhatHoSo;
        this.kenh = kenh;
        this.dieuKhienAnh = new AnhTinNhanController(kenh, Platform::runLater);

        dieuKhien = new HoiThoaiController(kenh, Platform::runLater);
        dieuKhienGoiThoai = new GoiThoaiController(kenh, Platform::runLater, this::capNhatGoiThoai);
        cuaSoGoiThoai.ganHanhDong(dieuKhienGoiThoai);
        dieuKhienTaiKhoan = new TaiKhoanController(kenh);
        dieuKhienBanBe = new BanBeController(kenh);
        dieuKhienNhom = new NhomController(kenh);

        this.nguoiDung = nguoiDung;
        this.idCuaMinh = DuLieu.chu(nguoiDung, "id");
        this.khiDangXuat = khiDangXuat;

        hopBanBe = new BanBeView(kenh, cacQuanHe -> {
            long soMoi = danhSachModel.capNhatBanBe(cacQuanHe);
            demLoiMoi.setText(soMoi > 99 ? "99+" : Long.toString(soMoi));
            demLoiMoi.setVisible(soMoi > 0);
            demLoiMoi.setManaged(soMoi > 0);
            nutBanBe.setAccessibleText("Bạn bè" + (soMoi > 0 ? ", " + soMoi + " lời mời" : ""));
            if (hopTaoNhom != null && hopTaoNhom.getScene() != null) {
                hopTaoNhom.capNhatBanBe(danhSachModel.banBe());
            }
            locDanhSach();
            capNhatMoTa();
        }, (id, ten) -> {
            danhSach.getSelectionModel().clearSelection();
            chonDich(id, ten);
            locDanhSach();
        });

        getStyleClass().add("trangChat");
        HBox cotTrai = new HBox(thanhDieuHuong(), cotDanhSach());
        setLeft(cotTrai);

        vungChat.getStyleClass().add("vungChat");
        thanhGhim.setId("thanhGhimTin");
        thanhGhim.getStyleClass().add("thanhGhimTin");
        thanhGhim.setAlignment(Pos.CENTER_LEFT);
        thanhGhim.setFillHeight(false);
        thanhGhim.setPrefHeight(64);
        thanhGhim.setMaxHeight(64);
        thanhGhim.setVisible(false);
        thanhGhim.managedProperty().bind(thanhGhim.visibleProperty());
        vungChat.setTop(new VBox(dauHoiThoai(), thanhGhim));

        StackPane vungCuon = new StackPane(lichSu, veCuoi);
        StackPane.setAlignment(veCuoi, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(veCuoi, new Insets(16));
        veCuoi.setId("veCuoiHoiThoai");
        veCuoi.setGraphic(BieuTuongView.xuongCuoi());
        veCuoi.setAccessibleText("Về tin nhắn mới nhất");
        veCuoi.setTooltip(new Tooltip("Về cuối cuộc trò chuyện"));
        veCuoi.setOnAction(e -> cuonVeCuoi());
        veCuoi.setVisible(false);
        lichSu.vvalueProperty().addListener((o, cu, moi) -> {
            veCuoi.setVisible(
                    moi.doubleValue() < 0.96 && cacTin.getHeight() > lichSu.getViewportBounds().getHeight());
            if (moi.doubleValue() >= 0.96) {
                danhDauDangDoc();
            }
        });
        vungChat.setCenter(vungCuon);
        oSoan = vungSoan();
        vungChat.setBottom(oSoan);
        trangThaiGui.addListener((giaTri, cu, moi) -> capNhatVungSoan(moi));
        setCenter(vungChat);

        cacTin.getStyleClass().add("cacTinNhan");
        cacTin.setFillWidth(true);
        lichSu.setFitToWidth(true);
        lichSu.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lichSu.getStyleClass().add("lichSuChat");
        taiTinCu.setOnAction(suKien -> taiLichSu(true));
        taiTinCu.setVisible(false);
        taiTinCu.managedProperty().bind(taiTinCu.visibleProperty());

        danhSach.setCellFactory(khung -> new ListCell<>() {
            @Override
            protected void updateItem(JsonObject nguoi, boolean rong) {
                super.updateItem(nguoi, rong);
                setText(null);
                setContextMenu(null);
                if (rong || nguoi == null) {
                    setGraphic(null);
                    return;
                }
                String id = DuLieu.chu(nguoi, "id");
                String ten = DuLieu.chu(nguoi, "tenHienThi");
                JsonObject nhom = danhSachModel.timNhom(id);
                String tinhTrang = nhom != null ? nhom.get("soThanhVien").getAsInt() + " thành viên" :
                        nguoi.get("dangOnline").getAsBoolean() ? "● Đang online" : "○ Ngoại tuyến";
                Label trangThai = DoHoaView.nhan(tinhTrang, "trangThaiNguoi");
                JsonObject hoiThoai = danhSachModel.timHoiThoai(id);
                if (hoiThoai != null) {
                    setContextMenu(menuLuuTru(id));
                }
                String xemTruoc = nhom == null ? "Bắt đầu trò chuyện" : DuLieu.chu(nhom, "tinCuoi");
                if (xemTruoc.isEmpty()) {
                    xemTruoc = "Nhóm mới";
                }
                long chuaDoc = 0;
                if (hoiThoai != null) {
                    JsonObject tin = hoiThoai.getAsJsonObject("tinCuoi");
                    xemTruoc = (DuLieu.chu(tin, "nguoiGuiId").equals(idCuaMinh) ? "Bạn: " : "") +
                            DuLieu.chu(tin, "noiDung").replaceAll("\\s+", " ");
                    chuaDoc = hoiThoai.get("chuaDoc").getAsLong();
                }
                Label tenNhan = DoHoaView.nhan(ten, "tenTrongDanhSach");
                Label xemTruocNhan = DoHoaView.nhan(xemTruoc, "tinXemTruoc");
                tenNhan.setMinWidth(0);
                xemTruocNhan.setMinWidth(0);
                trangThai.setMinWidth(0);
                tenNhan.setMaxWidth(145);
                xemTruocNhan.setMaxWidth(145);
                tenNhan.setTextOverrun(OverrunStyle.ELLIPSIS);
                xemTruocNhan.setTextOverrun(OverrunStyle.ELLIPSIS);
                VBox chu = new VBox(5, tenNhan, xemTruocNhan, trangThai);
                chu.setMinWidth(0);
                HBox.setHgrow(chu, Priority.ALWAYS);
                Label so = DoHoaView.nhan(chuaDoc > 99 ? "99+" : Long.toString(chuaDoc), "soChuaDoc");
                so.setVisible(chuaDoc > 0);
                so.setManaged(chuaDoc > 0);
                StackPane anh =
                        nhom == null ? DoHoaView.anh(ten, id, false) : new StackPane(BieuTuongView.banBe());
                if (nhom != null) {
                    anh.getStyleClass().addAll("anhDaiDien", "anhVua", "anhMau0");
                }
                HBox hang = new HBox(8, anh, chu, so);
                hang.setAlignment(Pos.CENTER_LEFT);
                hang.prefWidthProperty()
                        .bind(javafx.beans.binding.Bindings.max(0, widthProperty().subtract(24)));
                setAccessibleText(ten + ", " + chuaDoc + " tin chưa đọc. " + xemTruoc);
                setGraphic(hang);
            }
        });
        danhSach.getSelectionModel().selectedItemProperty().addListener((giaTri, cu, moi) -> {
            if (dangCapNhatDanhSach || moi == null) {
                return;
            }
            String id = DuLieu.chu(moi, "id");
            JsonObject nhom = danhSachModel.timNhom(id);
            if (nhom != null) {
                moChatNhom(nhom);
            } else {
                chonDich(id, DuLieu.chu(moi, "tenHienThi"));
            }
        });
        timKiem.textProperty().addListener((giaTri, cu, moi) -> locDanhSach());
        dieuKhien.khiCoSuKien(this::capNhatTheoSuKien);
    }

    private void capNhatTheoSuKien(HoiThoaiController.SuKien suKien) {
        if (daDong) {
            return;
        }
        switch (suKien.loai()) {
            case danhSachNguoi -> {
                danhSachModel.capNhatNguoi(suKien.duLieu().getAsJsonArray("nguoiDung"), idCuaMinh);
                soNguoi.setText(xemLuuTru ? "ĐÃ LƯU TRỮ" : "CUỘC TRÒ CHUYỆN");
                locDanhSach();
                capNhatMoTa();
                hopBanBe.taiDanhSach();
            }
            case nhomCapNhat -> {
                JsonObject nhom = suKien.duLieu().getAsJsonObject("nhom");
                capNhatNhom(nhom);
                if (chatNhom != null && chatNhom.id().equals(DuLieu.chu(nhom, "id")) &&
                        suKien.duLieu().has("tinNhan")) {
                    chatNhom.nhan(suKien.duLieu().getAsJsonObject("tinNhan"));
                }
            }
            case tinMoi -> {
                JsonObject tin = suKien.duLieu().getAsJsonObject("tinNhan");
                taiHoiThoai();
                if (thuocDich(tin)) {
                    boolean oCuoi = lichSu.getVvalue() > 0.95 ||
                            cacTin.getHeight() <= lichSu.getViewportBounds().getHeight();
                    lichSuModel.ghiNhanTin(tin);
                    veTin(oCuoi);
                    if (!oCuoi) {
                        veCuoi.setVisible(true);
                    }
                    danhDauDangDoc();
                }
            }
            case tinCapNhat -> {
                taiThanhGhim();
                JsonObject tin = suKien.duLieu().getAsJsonObject("tinNhan");
                taiHoiThoai();
                if (thuocDich(tin)) {
                    nhanTinDoi(tin);
                    veTin(false);
                }
            }
            case tinBiXoa -> {
                taiThanhGhim();
                String id = DuLieu.chu(suKien.duLieu(), "tinNhanId");
                lichSuModel.xoaTin(id);
                dieuKhienAnh.xoa(id);
                if (id.equals(maTraLoi())) {
                    boTraLoi();
                }
                veTin(false);
                taiHoiThoai();
            }
            case ghimCapNhat -> {
                if (thuocDich(suKien.duLieu().getAsJsonObject("tinNhan"))) {
                    taiThanhGhim();
                }
            }
            case doiPhuongDaDoc -> {
                if (Objects.equals(doiPhuong, DuLieu.chu(suKien.duLieu(), "nguoiDocId"))) {
                    lichSuModel.capNhatMocDoiPhuong(
                            Long.parseLong(DuLieu.chu(suKien.duLieu(), "tinCuoiId")));
                    veTin(false);
                }
            }
            case danhSachThayDoi -> taiHoiThoai();
            case hoiThoaiBiXoa -> sauKhiXoaHoiThoai(DuLieu.chu(suKien.duLieu(), "nguoiNhanId"));
            case banBeCapNhat -> hopBanBe.taiDanhSach();
            case hoSoCapNhat -> capNhatHoSo(suKien.duLieu().getAsJsonObject("hoSo"));
            case trangThaiChanDoi -> {
                if (Objects.equals(doiPhuong, DuLieu.chu(suKien.duLieu(), "nguoiNhanId"))) {
                    lanTaiHoSoDoiPhuong++;
                    docTrangThaiChan(suKien.duLieu());
                }
            }
            case cuocGoiDen, cuocGoiBatDau, cuocGoiKetThuc ->
                    dieuKhienGoiThoai.nhanSuKien(suKien.loai().name(), suKien.duLieu());
        }
    }

    public void batDau() {
        dieuKhien.danhSachNguoi().exceptionally(sai -> {
            Platform.runLater(() -> loi.setText(DoHoaView.thongBao(sai)));
            return null;
        });
        taiHoiThoai();
        taiLichSu(false);
        hopBanBe.taiDanhSach();
        taiNhom(null);
        dieuKhienTaiKhoan.hoSo().whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            if (sai != null) {
                loi.setText(DoHoaView.thongBao(sai));
            } else {
                capNhatHoSo(duLieu.getAsJsonObject("hoSo"));
            }
        }));
        cuaSo = getScene().getWindow();
        cuaSo.focusedProperty().addListener(khiDoiTieuDiem);
    }

    private VBox thanhDieuHuong() {
        VBox thanh = new VBox(20);
        thanh.setAlignment(Pos.TOP_CENTER);
        thanh.getStyleClass().add("thanhDieuHuong");
        thanh.setPrefWidth(86);
        Label logo = DoHoaView.nhan("N", "logoNho");
        nutTinNhan.setGraphic(BieuTuongView.tinNhan());
        nutTinNhan.setContentDisplay(ContentDisplay.TOP);
        nutTinNhan.setTooltip(new Tooltip("Tin nhắn"));
        nutTinNhan.setAccessibleText("Tin nhắn");
        nutTinNhan.setOnAction(e -> moHopThu(false));
        nutLuuTru.setGraphic(BieuTuongView.luuTru());
        nutLuuTru.setContentDisplay(ContentDisplay.TOP);
        nutLuuTru.setTooltip(new Tooltip("Cuộc trò chuyện đã lưu trữ"));
        nutLuuTru.setOnAction(e -> moHopThu(true));
        StackPane bieuTuongBan = new StackPane(BieuTuongView.banBe(), demLoiMoi);
        StackPane.setAlignment(demLoiMoi, Pos.TOP_RIGHT);
        demLoiMoi.setVisible(false);
        demLoiMoi.setManaged(false);
        nutBanBe.getStyleClass().clear();
        nutBanBe.getStyleClass().addAll("button", "nutTabTrai", "nutMoBanBe");
        nutBanBe.setGraphic(bieuTuongBan);
        nutBanBe.setContentDisplay(ContentDisplay.TOP);
        nutBanBe.setTooltip(new Tooltip("Bạn bè và lời mời kết bạn"));
        nutBanBe.setOnAction(suKien -> hienBanBe());
        nutHoSo.setGraphic(DoHoaView.anhHoSo(nguoiDung, 34));
        nutHoSo.setTooltip(new Tooltip("Hồ sơ và tài khoản"));
        nutHoSo.setAccessibleText("Mở hồ sơ cá nhân");
        nutHoSo.setOnAction(suKien -> hienHoSo());
        Region khoang = new Region();
        VBox.setVgrow(khoang, Priority.ALWAYS);
        Button thoat = DoHoaView.nut("", "nutDieuHuong");
        thoat.setGraphic(BieuTuongView.dangXuat());
        thoat.setTooltip(new Tooltip("Đăng xuất"));
        thoat.setAccessibleText("Đăng xuất");
        thoat.setOnAction(suKien -> khiDangXuat.run());
        chonTabTrai("tinNhan");
        thanh.getChildren().addAll(logo, nutTinNhan, nutBanBe, nutLuuTru, khoang, nutHoSo, thoat);
        return thanh;
    }

    private void chonTabTrai(String tab) {
        if (cotHoiThoai != null) {
            cotHoiThoai.setVisible(tab.equals("tinNhan"));
            cotHoiThoai.setManaged(tab.equals("tinNhan"));
        }
        var chon = javafx.css.PseudoClass.getPseudoClass("dangChon");
        nutTinNhan.pseudoClassStateChanged(chon, tab.equals("tinNhan") && !xemLuuTru);
        nutLuuTru.pseudoClassStateChanged(chon, tab.equals("tinNhan") && xemLuuTru);
        nutBanBe.pseudoClassStateChanged(chon, tab.equals("banBe"));
        nutHoSo.pseudoClassStateChanged(chon, tab.equals("hoSo"));
    }

    private VBox cotDanhSach() {
        VBox cot = new VBox(17);
        cotHoiThoai = cot;
        cot.getStyleClass().add("cotDanhSach");
        cot.setPrefWidth(300);
        VBox dau = new VBox(10, DoHoaView.nhan("KHÔNG GIAN CỦA BẠN", "nhanNho"),
                DoHoaView.nhan("Tin nhắn.", "tieuDeChat"),
                DoHoaView.nhan("Những câu chuyện đáng giữ lại.", "chuGoiY"));
        timKiem.setPromptText("Tìm cuộc trò chuyện…");
        timKiem.getStyleClass().add("oTimKiem");
        phong.setMaxWidth(Double.MAX_VALUE);
        phong.setOnAction(suKien -> {
            danhSach.getSelectionModel().clearSelection();
            chonDich(null, "Phòng chat chung");
        });
        Label ketNoi = DoHoaView.nhan("●  Đã kết nối NEXUS", "trangThaiKetNoi");
        Button taoNhom = DoHoaView.nut("Tạo nhóm", "nutHoSoPhu");
        taoNhom.setId("moTaoNhom");
        taoNhom.setGraphic(BieuTuongView.banBe());
        taoNhom.setMaxWidth(Double.MAX_VALUE);
        taoNhom.setOnAction(e -> moTaoNhom());
        cot.getChildren().addAll(dau, timKiem, phong, taoNhom, soNguoi, danhSach, ketNoi);
        VBox.setVgrow(danhSach, Priority.ALWAYS);
        danhSach.getStyleClass().add("danhSachNguoi");
        danhSach.setPlaceholder(
                DoHoaView.nhan("Chưa có cuộc trò chuyện.\nMở Bạn bè ở thanh trái để bắt đầu.", "chuGoiY"));
        return cot;
    }

    private HBox dauHoiThoai() {
        HBox dau = new HBox(14);
        dau.getStyleClass().add("dauHoiThoai");
        dau.setAlignment(Pos.CENTER_LEFT);
        anhDauChat.getChildren().setAll(DoHoaView.nhan("#", "bieuTuongPhong"));
        VBox chu = new VBox(6, tieuDe, moTa);
        Button thongTin = DoHoaView.nut("", "nutTenHoiThoai");
        thongTin.setId("tenMoThongTin");
        thongTin.setAccessibleText("Mở thông tin hội thoại");
        thongTin.setOnAction(e -> {
            if (getRight() != null) {
                dongBangThongTin();
                return;
            }
            ThongTinChatView.mo(this, dieuKhien, dieuKhienBanBe, doiPhuong, dieuKhienAnh, this::taiTep,
                    this::denTinChiaSe, this::moHoiThoaiTuHoSo);
        });
        HBox danhTinh = new HBox(14, anhDauChat, chu);
        danhTinh.setAlignment(Pos.CENTER_LEFT);
        thongTin.setGraphic(danhTinh);
        Region khoang = new Region();
        HBox.setHgrow(khoang, Priority.ALWAYS);
        nutGoiThoai.setId("goiThoai");
        nutGoiVideo.setId("goiVideo");
        nutGoiVideo.setGraphic(BieuTuongView.video(false));
        nutGoiVideo.setOnAction(e -> dieuKhienGoiThoai.goiVideo(doiPhuong, tieuDe.getText()));
        nutGoiVideo.disableProperty().bind(dangCoCuocGoi);
        nutGoiVideo.managedProperty().bind(nutGoiVideo.visibleProperty());
        nutGoiThoai.setOnAction(e -> {
            if (dangCoCuocGoi.get()) {
                cuaSoGoiThoai.hienLai();
            } else {
                dieuKhienGoiThoai.goi(doiPhuong, tieuDe.getText());
            }
        });
        nutGoiThoai.textProperty().bind(javafx.beans.binding.Bindings.when(dangCoCuocGoi)
                .then("Mở cuộc gọi").otherwise("Gọi thoại"));
        nutGoiThoai.managedProperty().bind(nutGoiThoai.visibleProperty());
        capNhatNutGoiThoai();
        dau.getChildren().addAll(thongTin, khoang, nutGoiThoai, nutGoiVideo);
        return dau;
    }

    private void capNhatGoiThoai(GoiThoaiController.HienThi duLieu) {
        dangCoCuocGoi.set(duLieu.trangThai() != GoiThoaiController.TrangThai.ranh);
        if (dangCoCuocGoi.get() && ghiAm.dangGhi()) {
            ghiAm.huy();
        }
        capNhatNutGoiThoai();
        cuaSoGoiThoai.capNhat(duLieu);
        if (!dangCoCuocGoi.get() && !duLieu.thongBao().isEmpty()) {
            loi.setText(duLieu.thongBao());
        }
    }

    private void capNhatNutGoiThoai() {
        nutGoiThoai.setVisible(doiPhuong != null || dangCoCuocGoi.get());
        nutGoiVideo.setVisible(doiPhuong != null);
    }

    BooleanBinding daChanDoiPhuong() {
        return trangThaiGui.isEqualTo(TrangThaiGui.daChan);
    }

    BooleanBinding chuaBietTrangThaiChan() {
        return trangThaiGui.isEqualTo(TrangThaiGui.dangTai).or(trangThaiGui.isEqualTo(TrangThaiGui.chuaTaiDuoc));
    }

    String tenHoiThoai() {
        return tieuDe.getText();
    }

    void xacNhanChan(javafx.scene.Node neo, String dich, String ten, boolean chan) {
        if (!chan) {
            ThongBaoNoiView.xacNhan(neo, "Bỏ chặn người dùng", "Bạn muốn bỏ chặn " + ten + "?", "Bỏ chặn",
                    () -> datChan(dich, false));
        } else {
            ThongBaoNoiView.xacNhan(neo, "Chặn người dùng",
                    "Sau khi chặn, bạn và " + ten + " sẽ không thể nhắn tin riêng hoặc gọi cho nhau.", "Chặn",
                    () -> datChan(dich, true));
        }
    }

    private CompletableFuture<Void> datChan(String dich, boolean chan) {
        int lan = lanTaiHoSoDoiPhuong;
        return dieuKhienBanBe.datChan(dich, chan).thenRunAsync(() -> {
            if (!daDong && Objects.equals(doiPhuong, dich) && lan == lanTaiHoSoDoiPhuong) {
                taiHoSoDoiPhuong(dich);
            }
        }, Platform::runLater);
    }

    private void docTrangThaiChan(JsonObject duLieu) {
        if (duLieu.has("daChan") && duLieu.get("daChan").getAsBoolean()) {
            trangThaiGui.set(TrangThaiGui.daChan);
        } else if (duLieu.has("biChan") && duLieu.get("biChan").getAsBoolean()) {
            trangThaiGui.set(TrangThaiGui.biChan);
        } else {
            trangThaiGui.set(TrangThaiGui.binhThuong);
        }
    }

    private void capNhatVungSoan(TrangThaiGui trangThai) {
        if (trangThai == TrangThaiGui.binhThuong) {
            vungChat.setBottom(oSoan);
            return;
        }
        ghiAm.huy();
        VBox vung = new VBox();
        vung.setId("trangThaiChanHoiThoai");
        vung.getStyleClass().add("vungSoan");
        HBox hang = new HBox(12);
        hang.getStyleClass().add("khungSoan");
        hang.setAlignment(Pos.CENTER_LEFT);
        Label noiDungTrangThai = DoHoaView.nhan(switch (trangThai) {
            case dangTai -> "Đang kiểm tra quyền gửi tin…";
            case chuaTaiDuoc -> "Chưa tải được trạng thái hội thoại.";
            case daChan -> "Bạn đã chặn người này.";
            case biChan -> "Không thể gửi tin nhắn";
            default -> "";
        }, "giaTriHoSo");
        noiDungTrangThai.setWrapText(true);
        noiDungTrangThai.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(noiDungTrangThai, Priority.ALWAYS);
        hang.getChildren().add(noiDungTrangThai);
        vung.getChildren().add(hang);
        if (trangThai == TrangThaiGui.daChan) {
            Button boChan = DoHoaView.nut("Bỏ chặn", "nutHoSoPhu");
            boChan.setId("boChanTrongHoiThoai");
            boChan.setMinWidth(Region.USE_PREF_SIZE);
            Label loiBoChan = DoHoaView.nhan("", "loiChat");
            loiBoChan.setWrapText(true);
            loiBoChan.visibleProperty().bind(loiBoChan.textProperty().isNotEmpty());
            loiBoChan.managedProperty().bind(loiBoChan.visibleProperty());
            boChan.setOnAction(e -> {
                boChan.setDisable(true);
                loiBoChan.setText("");
                datChan(doiPhuong, false).whenCompleteAsync((x, sai) -> {
                    boChan.setDisable(false);
                    if (sai != null) {
                        loiBoChan.setText(DoHoaView.thongBao(sai));
                    }
                }, Platform::runLater);
            });
            hang.getChildren().add(boChan);
            vung.setSpacing(5);
            vung.getChildren().add(loiBoChan);
        } else if (trangThai == TrangThaiGui.chuaTaiDuoc) {
            Button thuLai = DoHoaView.nut("Thử lại", "nutHoSoPhu");
            thuLai.setOnAction(e -> {
                trangThaiGui.set(TrangThaiGui.dangTai);
                taiHoSoDoiPhuong(doiPhuong);
            });
            hang.getChildren().add(thuLai);
        }
        vungChat.setBottom(vung);
    }

    private VBox vungSoan() {
        VBox vung = new VBox(9);
        vung.getStyleClass().add("vungSoan");
        HBox khung = new HBox(10);
        khung.setAlignment(Pos.CENTER);
        khung.getStyleClass().add("hangSoan");
        ToggleButton bieuCam = new ToggleButton();
        bieuCam.getStyleClass().addAll("nutBieuCam", "nutIconSoan");
        bieuCam.setGraphic(BieuTuongView.matCuoi());
        bieuCam.setAccessibleText("Chọn biểu cảm");
        bieuCam.setTooltip(new Tooltip("Chọn biểu cảm"));
        String[] cacBieuCam =
                {"😊", "😂", "😍", "🥰", "😎", "🤔", "😢", "😮", "👍", "👋", "👏", "🙏", "❤️", "💚", "🎉", "☕"};
        TilePane luoi = new TilePane(8, 8);
        luoi.setPrefColumns(8);
        luoi.setVisible(false);
        luoi.managedProperty().bind(luoi.visibleProperty());
        bieuCam.setOnAction(suKien -> luoi.setVisible(bieuCam.isSelected()));
        for (String chu : cacBieuCam) {
            Button nut = DoHoaView.nut(chu, "nutChonBieuCam");
            nut.setOnAction(suKien -> {
                noiDung.replaceSelection(chu);
                bieuCam.setSelected(false);
                luoi.setVisible(false);
                noiDung.requestFocus();
            });
            luoi.getChildren().add(nut);
        }
        noiDung.setPromptText("Viết một lời nhắn…");
        noiDung.setWrapText(true);
        noiDung.getStyleClass().add("oSoanTin");
        noiDung.setPrefRowCount(1);
        noiDung.setMinHeight(40);
        noiDung.setPrefHeight(40);
        noiDung.setMaxHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(noiDung, Priority.ALWAYS);
        noiDung.textProperty().addListener((giaTri, cu, moi) -> {
            demKyTu.setText(moi.length() + " / 4000");
            noiDung.setPrefHeight(
                    40 + 20 * Math.min(3, (int) moi.chars().filter(kyTu -> kyTu == 10).count()));
            gui.setDisable(dangGui || (moi.isBlank() && tepDangChon == null) || moi.length() > 4000);
        });
        noiDung.addEventFilter(KeyEvent.KEY_PRESSED, suKien -> {
            if (suKien.getCode() == KeyCode.ENTER && !suKien.isShiftDown()) {
                suKien.consume();
                guiTin();
            }
        });
        gui.setDisable(true);
        gui.setOnAction(suKien -> guiTin());
        Button tep = DoHoaView.nut("", "nutIconSoan"), anh = DoHoaView.nut("", "nutIconSoan");
        tep.setGraphic(BieuTuongView.kepGiay());
        tep.setTooltip(new Tooltip("Đính kèm tệp"));
        tep.setAccessibleText("Đính kèm tệp");
        anh.setGraphic(BieuTuongView.anh());
        anh.setTooltip(new Tooltip("Gửi ảnh"));
        anh.setAccessibleText("Gửi ảnh");
        Button micro = DoHoaView.nut("", "nutIconSoan");
        micro.setId("ghiAmTinNhan");
        micro.setGraphic(BieuTuongView.micro(false));
        micro.setTooltip(new Tooltip("Ghi âm tin nhắn"));
        micro.setAccessibleText("Ghi âm tin nhắn");
        micro.setMinWidth(Region.USE_PREF_SIZE);
        micro.disableProperty().bind(dangCoCuocGoi);
        micro.setOnAction(suKien -> ghiAm.batDau());
        gui.setText("");
        gui.setGraphic(BieuTuongView.gui());
        gui.getStyleClass().add("nutIconSoan");
        gui.setTooltip(new Tooltip("Gửi tin nhắn"));
        gui.setAccessibleText("Gửi tin nhắn");
        tep.setMinWidth(Region.USE_PREF_SIZE);
        anh.setMinWidth(Region.USE_PREF_SIZE);
        gui.setMinWidth(Region.USE_PREF_SIZE);
        bieuCam.setMinWidth(Region.USE_PREF_SIZE);
        noiDung.setMinWidth(80);
        tep.setOnAction(suKien -> chonTep(false));
        anh.setOnAction(suKien -> chonTep(true));
        khung.getChildren().addAll(bieuCam, tep, anh, micro, noiDung, gui);
        Button bo = DoHoaView.nut("Bỏ tệp", "nutDinhKem");
        bo.setOnAction(suKien -> {
            tepDangChon = null;
            capNhatTepChon();
        });
        tenTepChon.setWrapText(true);
        tenTepChon.setMaxWidth(410);
        anhTepChon.setFitWidth(140);
        anhTepChon.setFitHeight(96);
        anhTepChon.setPreserveRatio(true);
        anhTepChon.setAccessibleText("Ảnh đang chọn để gửi");
        anhTepChon.setVisible(false);
        anhTepChon.managedProperty().bind(anhTepChon.visibleProperty());
        vungTepChon.getChildren().addAll(anhTepChon, tenTepChon, bo);
        vungTepChon.setAlignment(Pos.CENTER_LEFT);
        vungTepChon.managedProperty().bind(vungTepChon.visibleProperty());
        vungTepChon.setVisible(false);
        Button huy = DoHoaView.nut("Hủy truyền", "nutDinhKem");
        huy.setOnAction(suKien -> dieuKhien.huyTruyen());
        tienTrinhTep.setPrefWidth(120);
        trangThaiTep.setWrapText(true);
        trangThaiTep.setMaxWidth(350);
        HBox.setHgrow(trangThaiTep, Priority.ALWAYS);
        vungTruyenTep.getChildren().addAll(tienTrinhTep, trangThaiTep, huy);
        vungTruyenTep.setAlignment(Pos.CENTER_LEFT);
        vungTruyenTep.managedProperty().bind(vungTruyenTep.visibleProperty());
        vungTruyenTep.setVisible(false);
        HBox goiY = new HBox(DoHoaView.nhan("Enter để gửi · Shift + Enter để xuống dòng", "chuGoiY"));
        Region khoang = new Region();
        HBox.setHgrow(khoang, Priority.ALWAYS);
        goiY.getChildren().addAll(khoang, demKyTu);
        Button boTraLoi = DoHoaView.nut("×", "nutBoTraLoi");
        boTraLoi.setAccessibleText("Bỏ trả lời");
        boTraLoi.setTooltip(new Tooltip("Bỏ trả lời"));
        boTraLoi.setOnAction(suKien -> boTraLoi());
        chuTraLoi.setWrapText(true);
        chuTraLoi.setMaxWidth(560);
        HBox.setHgrow(chuTraLoi, Priority.ALWAYS);
        vungTraLoi.getChildren().addAll(chuTraLoi, boTraLoi);
        vungTraLoi.getStyleClass().add("thanhTraLoi");
        vungTraLoi.setAlignment(Pos.CENTER_LEFT);
        vungTraLoi.setVisible(false);
        vungTraLoi.managedProperty().bind(vungTraLoi.visibleProperty());
        loi.setWrapText(true);
        loi.setMaxWidth(Double.MAX_VALUE);
        VBox khungNhap = new VBox(8, vungTraLoi, vungTepChon, ghiAm, khung);
        khungNhap.getStyleClass().add("khungSoan");
        vung.getChildren().addAll(loi, vungTruyenTep, luoi, khungNhap, goiY);
        return vung;
    }

    private void chonDich(String id, String ten) {
        if (!hienChat()) {
            return;
        }
        if (Objects.equals(doiPhuong, id)) {
            danhDauDangDoc();
            return;
        }
        ghiAm.huy();
        if (!dangGui) {
            tepDangChon = null;
            capNhatTepChon();
        }
        boTraLoi();
        lichSuModel.xoaCapNhatGanDay();
        doiPhuong = id;
        capNhatNutGoiThoai();
        taiAnhDauChat(id, ten);
        tieuDe.setText(ten);
        noiDung.clear();
        loi.setText("");
        capNhatMoTa();
        taiLichSu(false);
    }

    private void taiAnhDauChat(String id, String ten) {
        if (id == null) {
            lanTaiHoSoDoiPhuong++;
            trangThaiGui.set(TrangThaiGui.binhThuong);
            anhDauChat.getChildren().setAll(DoHoaView.nhan("#", "bieuTuongPhong"));
            return;
        }
        trangThaiGui.set(TrangThaiGui.dangTai);
        anhDauChat.getChildren().setAll(DoHoaView.anhHoSo(DuLieu.tao("id", id, "tenHienThi", ten), 44));
        taiHoSoDoiPhuong(id);
    }

    private void taiHoSoDoiPhuong(String id) {
        int lan = ++lanTaiHoSoDoiPhuong;
        dieuKhienBanBe.hoSoCongKhai(id).whenComplete((tra, sai) -> Platform.runLater(() -> {
            if (daDong || !Objects.equals(doiPhuong, id)) {
                return;
            }
            if (sai != null) {
                if (lan == lanTaiHoSoDoiPhuong && trangThaiGui.get() == TrangThaiGui.dangTai) {
                    trangThaiGui.set(TrangThaiGui.chuaTaiDuoc);
                }
                return;
            }
            JsonObject hoSo = tra.getAsJsonObject("hoSo");
            if (lan == lanTaiHoSoDoiPhuong) {
                docTrangThaiChan(hoSo);
            }
            StackPane anh = DoHoaView.anhHoSo(hoSo, 44);
            anh.setClip(new javafx.scene.shape.Circle(22, 22, 22));
            anhDauChat.getChildren().setAll(anh);
        }));
    }

    private void capNhatMoTa() {
        if (doiPhuong == null) {
            moTa.setText("Tất cả người đang đăng nhập đều nhận tin trong phòng này");
            return;
        }
        danhSachModel.timNguoi(doiPhuong)
                .ifPresent(nguoi -> {
                    tieuDe.setText(DuLieu.chu(nguoi, "tenHienThi"));
                    moTa.setText(nguoi.get("dangOnline").getAsBoolean() ?
                            "● Đang online · " + (danhSachModel.laBanBe(doiPhuong) ? "Bạn bè" : "Người lạ") :
                            "Ngoại tuyến · " + (danhSachModel.laBanBe(doiPhuong) ? "Bạn bè" : "Người lạ"));
                });
    }

    private void locDanhSach() {
        // Không chọn lại dòng khi danh sách online cập nhật, tránh tải lại lịch sử đang đọc.
        dangCapNhatDanhSach = true;
        try {
            danhSach.getItems().setAll(danhSachModel.loc(timKiem.getText(), xemLuuTru, doiPhuong));
            String dangMo = getCenter() == chatNhom && chatNhom != null ? "nhom:" + chatNhom.id() : doiPhuong;
            danhSach.getItems().stream().filter(nguoi -> Objects.equals(dangMo, DuLieu.chu(nguoi, "id")))
                    .findFirst()
                    .ifPresent(nguoi -> danhSach.getSelectionModel().select(nguoi));
            danhSach.refresh();
        } finally {
            dangCapNhatDanhSach = false;
        }
    }

    private void taiNhom(String sau) {
        dieuKhienNhom.danhSach(sau).whenComplete((tra, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            if (sai != null) {
                loi.setText(DoHoaView.thongBao(sai));
                return;
            }
            JsonArray ds = tra.getAsJsonArray("cacNhom");
            for (JsonElement phanTu : ds) {
                JsonObject nhom = phanTu.getAsJsonObject();
                danhSachModel.themNhomNeuThieu(nhom);
            }
            locDanhSach();
            if (ds.size() == 50) {
                taiNhom(DuLieu.chu(ds.get(49).getAsJsonObject(), "id"));
            }
        }));
    }

    private void capNhatNhom(JsonObject nhom) {
        danhSachModel.capNhatNhom(nhom);
        locDanhSach();
    }

    private void moTaoNhom() {
        Runnable[] dong = {null};
        List<String> ganDay = danhSachModel.banBeGanDay();
        hopTaoNhom = new TaoNhomView(kenh, danhSachModel.banBe(), ganDay, () -> dong[0].run(), nhom -> {
            dong[0].run();
            capNhatNhom(nhom);
            moChatNhom(nhom);
        });
        dong[0] = ThongBaoNoiView.mo(this, hopTaoNhom, hopTaoNhom::coTheDong);
        hopBanBe.taiDanhSach();
    }

    private void moChatNhom(JsonObject nhom) {
        if (chatNhom != null && getCenter() == chatNhom && chatNhom.id().equals(DuLieu.chu(nhom, "id"))) {
            return;
        }
        if (!hienChat()) {
            return;
        }
        xemLuuTru = false;
        chonTabTrai("tinNhan");
        soNguoi.setText("CUỘC TRÒ CHUYỆN");
        chatNhom = new ChatNhomView(kenh, nhom, idCuaMinh);
        setCenter(chatNhom);
        locDanhSach();
    }

    private void taiHoiThoai() {
        dieuKhien.taiDanhSachHoiThoai(ketQua -> {
            danhSachModel.capNhatHoiThoai(ketQua);
            JsonObject chung = danhSachModel.timHoiThoai("0");
            long so = chung == null ? 0 : chung.get("chuaDoc").getAsLong();
            phong.setText("#   Phòng chat chung" + (so > 0 ? "  (" + so + " chưa đọc)" : ""));
            locDanhSach();
        }, sai -> loi.setText(DoHoaView.thongBao(sai)));
    }

    private void danhDauDangDoc() {
        if (daDong || getCenter() != vungChat || dangTaiLichSu || getScene() == null ||
                !getScene().getWindow().isFocused() || lichSuModel.rong()) {
            return;
        }
        if (lichSu.getVvalue() < 0.95 && cacTin.getHeight() > lichSu.getViewportBounds().getHeight()) {
            return;
        }
        String dich = doiPhuong;
        long tinCuoi = lichSuModel.maTinMoiNhat();
        if (!lichSuModel.ghiMocDaGui(dich, tinCuoi)) {
            return;
        }
        dieuKhien.danhDauDoc(dich, tinCuoi).whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            if (sai != null) {
                lichSuModel.boMocDaGui(dich, tinCuoi);
                loi.setText(DoHoaView.thongBao(sai));
            }
        }));
    }

    private void taiLichSu(boolean taiCu) {
        int lan = ++lanTai;
        String dich = doiPhuong;
        dangTaiLichSu = true;
        String truoc = taiCu ? lichSuModel.maTinCuNhat() : null;
        if (!taiCu) {
            tinCanTim = null;
            thanhGhim.setVisible(false);
            taiThanhGhim();
            veCuoi.setVisible(false);
            lichSuModel.xoaTrangDangMo();
            String dichDoc = doiPhuong;
            dieuKhien.thongTin(dichDoc, "ghim", null).thenAccept(tra -> Platform.runLater(() -> {
                if (!daDong && lan == lanTai && Objects.equals(dichDoc, doiPhuong)) {
                    lichSuModel.capNhatMocDoiPhuong(Long.parseLong(DuLieu.chu(tra, "daDocId")));
                    veTin(false);
                }
            }));
            tinDaVe.clear();
            hangDaVe.clear();
            cacTin.getChildren().setAll(DoHoaView.nhan("Đang tải câu chuyện…", "chuGoiY"));
        }
        taiTinCu.setDisable(true);
        dieuKhien.lichSu(dich, truoc).whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong || lan != lanTai || !Objects.equals(dich, doiPhuong)) {
                return;
            }
            taiTinCu.setDisable(false);
            dangTaiLichSu = false;
            if (sai != null) {
                dangTaiLichSu = true;
                loi.setText(DoHoaView.thongBao(sai));
                return;
            }
            JsonArray cacTinMoi = duLieu.getAsJsonArray("tinNhan");
            for (JsonElement phanTu : cacTinMoi) {
                JsonObject tin = phanTu.getAsJsonObject();
                lichSuModel.ghiNhanTin(tin);
            }
            lichSuModel.capNhatCacTrichDan();
            taiTinCu.setVisible(duLieu.get("coTinCu").getAsBoolean());
            veTin(!taiCu);
            if (tinCanTim != null) {
                Platform.runLater(() -> denTinGhim(tinCanTim));
            }
            danhDauDangDoc();
        }));
    }

    private boolean thuocDich(JsonObject tin) {
        return lichSuModel.thuocHoiThoai(tin, doiPhuong, idCuaMinh);
    }

    private void veTin(boolean xuongCuoi) {
        double caoCu = cacTin.getHeight(), viTriCu = lichSu.getVvalue();
        long dauCu = tinDaVe.keySet().stream().mapToLong(Long::parseLong).min().orElse(0);
        String maTinCuNhat = lichSuModel.maTinCuNhat();
        boolean themTruoc = dauCu > 0 && maTinCuNhat != null && Long.parseLong(maTinCuNhat) < dauCu;
        if (!cacTin.getChildren().contains(taiTinCu)) {
            cacTin.getChildren().setAll(taiTinCu);
        }
        tinDaVe.keySet().removeIf(id -> !lichSuModel.coTin(id));
        hangDaVe.keySet().removeIf(id -> !lichSuModel.coTin(id));
        java.util.List<javafx.scene.Node> cacHang = new ArrayList<>();
        cacHang.add(taiTinCu);
        if (lichSuModel.rong()) {
            VBox trong = new VBox(14, DoHoaView.nhan("Một lời chào đang chờ bạn.", "tieuDeTrong"),
                    DoHoaView.nhan("Gửi tin nhắn đầu tiên để bắt đầu câu chuyện.", "chuGoiY"));
            trong.setAlignment(Pos.CENTER);
            trong.setPadding(new Insets(90, 15, 60, 15));
            cacTin.getChildren().setAll(taiTinCu, trong);
            return;
        }
        lichSuModel.cacTinTangDan().forEach(tinDaLuu -> {
            String id = DuLieu.chu(tinDaLuu, "id");
            boolean daXem = doiPhuong != null && lichSuModel.doiPhuongDaXem(id);
            TinDaVe cu = tinDaVe.get(id);
            if (cu != null && daXem == cu.daXem() && tinDaLuu.equals(cu.duLieu()) && hangDaVe.containsKey(id)) {
                cacHang.add(hangDaVe.get(id));
                return;
            }

            JsonObject tin = tinDaLuu.deepCopy();
            tin.addProperty("daXem", daXem);
            boolean cuaMinh = DuLieu.chu(tin, "nguoiGuiId").equals(idCuaMinh);
            VBox nhom;
            if ("cuocGoi".equals(DuLieu.chu(tin, "loaiTin")) && !LichSuTinNhanModel.daThuHoi(tin)) {
                String idNguoiGoiLai = doiPhuong;
                String tenNguoiGoiLai = tieuDe.getText();
                nhom = new LichSuGoiView(tin, cuaMinh, dangCoCuocGoi,
                        () -> {
                            if (tin.has("goiVideo") && tin.get("goiVideo").getAsBoolean()) {
                                dieuKhienGoiThoai.goiVideo(idNguoiGoiLai, tenNguoiGoiLai);
                            } else {
                                dieuKhienGoiThoai.goi(idNguoiGoiLai, tenNguoiGoiLai);
                            }
                        }, this::thaoTacTin);
            } else {
                nhom = new BongTinNhanView(tin, idCuaMinh, this::chonTraLoi, this::thaoTacTin,
                        this::taiTep, dieuKhienAnh, dieuKhien::taiTinThoai);
            }
            HBox hang = new HBox(9);
            hang.setAlignment(cuaMinh ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            if (!cuaMinh) {
                hang.getChildren().add(DoHoaView.anh(DuLieu.chu(tin, "tenNguoiGui"),
                        DuLieu.chu(tin, "nguoiGuiId"), true));
            }
            hang.getChildren().add(nhom);
            cacHang.add(hang);
            hangDaVe.put(id, hang);
            tinDaVe.put(id, new TinDaVe(tinDaLuu.deepCopy(), daXem));
        });
        for (int i = 0; i < cacHang.size(); i++) {
            var hang = cacHang.get(i);
            if (i < cacTin.getChildren().size() && cacTin.getChildren().get(i) == hang) {
                continue;
            }
            cacTin.getChildren().remove(hang);
            cacTin.getChildren().add(i, hang);
        }
        while (cacTin.getChildren().size() > cacHang.size()) {
            cacTin.getChildren().remove(cacTin.getChildren().size() - 1);
        }
        if (xuongCuoi) {
            cuonVeCuoi();
        } else {
            Platform.runLater(() -> {
                cacTin.applyCss();
                cacTin.layout();
                double moi = cacTin.getHeight() - lichSu.getViewportBounds().getHeight();
                if (moi > 0) {
                    lichSu.setVvalue(Math.max(0, Math.min(1,
                            (viTriCu * Math.max(0, caoCu - lichSu.getViewportBounds().getHeight()) +
                                    (themTruoc ? Math.max(0, cacTin.getHeight() - caoCu) : 0)) / moi)));
                }
            });
        }
    }

    private void cuonVeCuoi() {
        String dich = doiPhuong;
        int lan = lanTai;
        Platform.runLater(() -> {
            if (daDong || lan != lanTai || !Objects.equals(dich, doiPhuong)) {
                return;
            }
            cacTin.applyCss();
            cacTin.layout();
            lichSu.setVvalue(1);
            veCuoi.setVisible(false);
            Platform.runLater(() -> {
                if (daDong || lan != lanTai || !Objects.equals(dich, doiPhuong)) {
                    return;
                }
                lichSu.setVvalue(1);
                danhDauDangDoc();
            });
        });
    }

    private void moHoiThoaiTuHoSo(String id, String ten) {
        hienChat();
        chonDich(id, ten);
    }

    private void taiThanhGhim() {
        String dich = doiPhuong;
        int lan = ++lanTaiGhim;
        dieuKhien.thongTin(dich, "ghim", null).whenComplete((tra, sai) -> Platform.runLater(() -> {
            if (daDong || lan != lanTaiGhim || !Objects.equals(dich, doiPhuong)) {
                return;
            }
            if (sai != null) {
                thanhGhim.setVisible(false);
                return;
            }
            JsonArray ds = tra.getAsJsonArray("tinNhan");
            thanhGhim.getChildren().clear();
            thanhGhim.setVisible(!ds.isEmpty());
            if (ds.isEmpty()) {
                return;
            }
            JsonObject tin =
                    tra.has("ghimMoi") ? tra.getAsJsonObject("ghimMoi") : ds.get(0).getAsJsonObject();
            String id = DuLieu.chu(tin, "id");
            Button xem = DoHoaView.nut("", "nutXemGhim");
            Label tenGui = DoHoaView.nhan(DuLieu.chu(tin, "tenNguoiGui"), "tenTrichDan");
            Label tomTat =
                    DoHoaView.nhan(TrichDanTinView.noiDung(tin).replaceAll("\\s+", " "), "chuTrichDan");
            VBox xemTruoc = new VBox(3, tenGui, tomTat);
            xemTruoc.setMinWidth(0);
            xemTruoc.setMaxHeight(Region.USE_PREF_SIZE);
            xem.setGraphic(xemTruoc);
            xem.setAlignment(Pos.CENTER_LEFT);
            xem.setMinWidth(0);
            xem.setMaxHeight(46);
            xem.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(xem, Priority.ALWAYS);
            xem.setAccessibleText("Đi tới tin nhắn đã ghim");
            xem.setOnAction(e -> denTinGhim(id));
            Button bo = DoHoaView.nut("×", "nutBoTraLoi");
            bo.setAccessibleText("Bỏ ghim tin nhắn");
            bo.setTooltip(new Tooltip("Bỏ ghim"));
            bo.setOnAction(e -> ThongBaoNoiView.xacNhan(this, "Bỏ ghim tin nhắn",
                    "Bỏ tin này khỏi danh sách ghim của cuộc trò chuyện?", "Bỏ ghim",
                    () -> dieuKhien.boGhim(id).thenApply(k -> null)));
            thanhGhim.getChildren().addAll(BieuTuongView.ghim(), xem, bo);
        }));
    }

    private void denTinGhim(String id) {
        if (id == null || daDong) {
            return;
        }
        tinCanTim = id;
        if (lichSuModel.coTin(id)) {
            tinCanTim = null;
            String dich = doiPhuong;
            Platform.runLater(() -> {
                if (!Objects.equals(dich, doiPhuong) || daDong) {
                    return;
                }
                cacTin.applyCss();
                cacTin.layout();
                javafx.scene.Node hang = hangDaVe.get(id);
                if (hang == null) {
                    return;
                }
                double du = cacTin.getHeight() - lichSu.getViewportBounds().getHeight();
                if (du > 0) {
                    lichSu.setVvalue(
                            Math.max(0, Math.min(1, (hang.getBoundsInParent().getMinY() - 24) / du)));
                }
                hang.getStyleClass().add("tinDuocDinhVi");
                javafx.animation.PauseTransition cho =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                cho.setOnFinished(e -> hang.getStyleClass().remove("tinDuocDinhVi"));
                cho.play();
            });
        } else if (!dangTaiLichSu && taiTinCu.isVisible()) {
            taiLichSu(true);
        } else if (!dangTaiLichSu) {
            tinCanTim = null;
            loi.setText("Tin này không còn trong lịch sử của bạn.");
        }
    }

    private String maTraLoi() {
        return tinDangTraLoi == null ? null : DuLieu.chu(tinDangTraLoi, "id");
    }

    private void boTraLoi() {
        tinDangTraLoi = null;
        vungTraLoi.setVisible(false);
    }

    private void chonTraLoi(JsonObject tin) {
        if (trangThaiGui.get() != TrangThaiGui.binhThuong || dangGui || LichSuTinNhanModel.daThuHoi(tin)) {
            return;
        }
        tinDangTraLoi = tin;
        javafx.scene.Node bo = vungTraLoi.getChildren().get(vungTraLoi.getChildren().size() - 1);
        vungTraLoi.getChildren().setAll(new TrichDanTinView(tin, dieuKhienAnh), bo);
        vungTraLoi.setVisible(true);
        noiDung.requestFocus();
    }

    private void nhanTinDoi(JsonObject tin) {
        if (!lichSuModel.nhanTinDoi(tin)) {
            return;
        }
        if (LichSuTinNhanModel.daThuHoi(tin)) {
            String id = DuLieu.chu(tin, "id");
            dieuKhienAnh.xoa(id);
            if (id.equals(maTraLoi())) {
                boTraLoi();
            }
        }
    }

    private java.util.concurrent.CompletableFuture<Void> thaoTacTin(String loai, JsonObject duLieu) {
        String id = DuLieu.chu(duLieu, "tinNhanId"), dich = doiPhuong;
        if (!dangThaoTacTin.add(id)) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        if (loai.equals("moChuyenTiep")) {
            dangThaoTacTin.remove(id);
            moChuyenTiep(id);
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        loi.setText("");
        var hoanTat = new java.util.concurrent.CompletableFuture<Void>();
        dieuKhien.thaoTacTin(loai, duLieu).whenComplete((tra, sai) -> Platform.runLater(() -> {
            dangThaoTacTin.remove(id);
            if (daDong) {
                hoanTat.complete(null);
                return;
            }
            if (sai != null) {
                if (Objects.equals(dich, doiPhuong)) {
                    loi.setText(DoHoaView.thongBao(sai));
                }
                hoanTat.completeExceptionally(sai);
                return;
            }
            if (loai.equals("ghimTin")) {
                loi.setText("Đã ghim tin.");
                hoanTat.complete(null);
                return;
            }
            if (loai.equals("xoaTin")) {
                lichSuModel.xoaTin(id);
                dieuKhienAnh.xoa(id);
                if (id.equals(maTraLoi())) {
                    boTraLoi();
                }
                veTin(false);
                taiHoiThoai();
                hoanTat.complete(null);
                return;
            }
            taiHoiThoai();
            JsonObject tin = tra.getAsJsonObject("tinNhan");
            if (thuocDich(tin)) {
                nhanTinDoi(tin);
                veTin(false);
            }
            hoanTat.complete(null);
        }));
        return hoanTat;
    }

    private void moChuyenTiep(String tinId) {
        JsonObject tin = lichSuModel.timTin(tinId);
        if (tin == null) {
            return;
        }
        new ChiaSeView(kenh, tin, dieuKhienAnh, danhSachModel.banSaoHoiThoai(),
                danhSachModel.banSaoBanBe(), () -> {
            taiHoiThoai();
            loi.setText("Đã chia sẻ tin nhắn.");
        }).mo(this);
    }

    private void guiTin() {
        if (trangThaiGui.get() != TrangThaiGui.binhThuong) {
            return;
        }
        if (tepDangChon != null) {
            guiTep();
            return;
        }
        String chu = noiDung.getText().strip();
        if (dangGui || chu.isEmpty() || chu.length() > 4000 || daDong) {
            return;
        }
        String dich = doiPhuong;
        String traLoi = maTraLoi();

        dangGui = true;
        gui.setDisable(true);
        loi.setText("");
        dieuKhien.guiTin(dich, chu, traLoi).whenComplete((duLieu, sai) -> Platform.runLater(() -> {
            if (daDong) {
                return;
            }
            dangGui = false;
            if (sai == null) {
                dieuKhien.daGui(dich, false);
                if (Objects.equals(doiPhuong, dich)) {
                    JsonObject tin = duLieu.getAsJsonObject("tinNhan");
                    lichSuModel.ghiNhanTin(tin);
                    if (noiDung.getText().strip().equals(chu)) {
                        noiDung.clear();
                    }
                    if (Objects.equals(maTraLoi(), traLoi)) {
                        boTraLoi();
                    }
                    veTin(true);
                }
            } else if (Objects.equals(doiPhuong, dich)) {
                loi.setText(DoHoaView.thongBao(sai));
            }
            gui.setDisable((noiDung.getText().isBlank() && tepDangChon == null) ||
                    noiDung.getText().length() > 4000);
            noiDung.requestFocus();
        }));
    }

    private void chonTep(boolean laAnh) {
        if (dangGui) {
            loi.setText("Đang gửi tin. Bạn chờ một chút nhé.");
            return;
        }
        javafx.stage.FileChooser chon = new javafx.stage.FileChooser();
        chon.setTitle(laAnh ? "Chọn ảnh để gửi" : "Chọn tệp để gửi");
        if (laAnh) {
            chon.getExtensionFilters()
                    .add(new javafx.stage.FileChooser.ExtensionFilter("Ảnh PNG/JPEG", "*.png", "*.jpg",
                            "*.jpeg"));
        } else {
            chon.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Tất cả tệp", "*"));
        }
        var tep = chon.showOpenDialog(getScene().getWindow());
        if (tep != null) {
            chonTep(tep.toPath());
        }
    }

    void chonTep(java.nio.file.Path tep) {
        try {
            dieuKhien.kiemTraTepGui(tep);
            tepDangChon = tep;
            loi.setText("");
            capNhatTepChon();
        } catch (java.io.IOException sai) {
            loi.setText(DoHoaView.thongBao(sai));
        }
    }

    private void capNhatTepChon() {
        vungTepChon.setVisible(tepDangChon != null);
        if (tepDangChon == null) {
            tepDaXem = null;
            anhTepChon.setImage(null);
            anhTepChon.setVisible(false);
        } else {
            tenTepChon.setText(tepDangChon.getFileName() + " · " +
                    TruyenTepTCP.dungLuong(tepDangChon.toFile().length()));
            tenTepChon.setVisible(true);
            tenTepChon.setManaged(true);
            if (!tepDangChon.equals(tepDaXem)) {
                java.nio.file.Path tep = tepDangChon;
                tepDaXem = tep;
                anhTepChon.setImage(null);
                anhTepChon.setVisible(false);
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try (var vao = java.nio.file.Files.newInputStream(tep)) {
                        var anh = new javafx.scene.image.Image(vao, 140, 96, true, true);
                        return anh.isError() ? null : anh;
                    } catch (Exception sai) {
                        return null;
                    }
                }).thenAccept(anh -> Platform.runLater(() -> {
                    if (daDong || !Objects.equals(tepDangChon, tep)) {
                        return;
                    }
                    anhTepChon.setImage(anh);
                    anhTepChon.setVisible(anh != null);
                    tenTepChon.setVisible(anh == null);
                    tenTepChon.setManaged(anh == null);
                }));
            }
        }
        gui.setDisable(dangGui || (noiDung.getText().isBlank() && tepDangChon == null) ||
                noiDung.getText().length() > 4000);
    }

    private void batTienTrinh(String ten) {
        vungTruyenTep.setVisible(true);
        tienTrinhTep.setProgress(-1);
        trangThaiTep.setText(ten);
        loi.setText("");
    }

    private java.util.function.DoubleConsumer capNhatTienTrinh(String ten) {
        return so -> Platform.runLater(() -> {
            if (!daDong) {
                tienTrinhTep.setProgress(so);
                trangThaiTep.setText(ten +
                        (so < 0 ? " · Đang kiểm tra…" : " · " + Math.min(100, Math.round(so * 100)) + "%"));
            }
        });
    }

    private CompletableFuture<Void> guiTinThoai(Path tep) {
        if (trangThaiGui.get() != TrangThaiGui.binhThuong) {
            return CompletableFuture.failedFuture(new IllegalStateException("Không thể gửi tin nhắn."));
        }
        if (daDong || dangGui || dieuKhien.dangTruyen()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Chưa thể gửi đoạn thoại. Hãy đợi lượt gửi hiện tại hoàn tất."));
        }

        String dich = doiPhuong;
        String traLoi = maTraLoi();
        String ten = "Gửi đoạn thoại";
        dangGui = true;
        gui.setDisable(true);
        batTienTrinh(ten);
        return dieuKhien.guiTinThoai(tep, dich, traLoi, capNhatTienTrinh(ten))
                .handleAsync((tin, sai) -> {
                    if (!daDong) {
                        dangGui = false;
                        vungTruyenTep.setVisible(false);
                        if (sai == null) {
                            dieuKhien.daGui(dich, true);
                            taiHoiThoai();
                            if (Objects.equals(dich, doiPhuong)) {
                                lichSuModel.ghiNhanTin(tin);
                                if (Objects.equals(maTraLoi(), traLoi)) {
                                    boTraLoi();
                                }
                                veTin(true);
                            }
                        }
                        capNhatTepChon();
                    }
                    if (sai != null) {
                        throw new CompletionException(sai);
                    }
                    return null;
                }, Platform::runLater);
    }

    private void guiTep() {
        if (trangThaiGui.get() != TrangThaiGui.binhThuong || dangGui || daDong) {
            return;
        }
        if (dieuKhien.dangTruyen()) {
            loi.setText("Đang truyền một tệp khác. Bạn chờ hoặc hủy tệp đó trước.");
            return;
        }
        java.nio.file.Path tep = tepDangChon;
        String dich = doiPhuong, chu = noiDung.getText().strip(), traLoi = maTraLoi();
        if (chu.length() > 4000) {
            return;
        }
        dangGui = true;
        tepDangChon = null;
        capNhatTepChon();
        String ten = "Gửi " + tep.getFileName() + " tới " + tieuDe.getText();
        batTienTrinh(ten);
        dieuKhien.guiTep(tep, dich, chu, traLoi, capNhatTienTrinh(ten))
                .whenComplete((tin, sai) -> Platform.runLater(() -> {
                    if (daDong) {
                        return;
                    }
                    dangGui = false;
                    vungTruyenTep.setVisible(false);
                    if (sai == null) {
                        dieuKhien.daGui(dich, true);
                        taiHoiThoai();
                        if (Objects.equals(dich, doiPhuong)) {
                            lichSuModel.ghiNhanTin(tin);
                            if (noiDung.getText().strip().equals(chu)) {
                                noiDung.clear();
                            }
                            if (Objects.equals(maTraLoi(), traLoi)) {
                                boTraLoi();
                            }
                            veTin(true);
                        }
                    } else {
                        loi.setText(DoHoaView.thongBao(sai));
                        if (Objects.equals(dich, doiPhuong) && tepDangChon == null) {
                            tepDangChon = tep;
                        }
                    }
                    capNhatTepChon();
                }));
    }

    private void taiTep(JsonObject tin) {
        if (dieuKhien.dangTruyen()) {
            loi.setText("Đang truyền một tệp khác. Bạn chờ hoặc hủy tệp đó trước.");
            return;
        }
        String ten = DuLieu.chu(tin.getAsJsonObject("tep"), "tenTep");
        javafx.stage.FileChooser chon = new javafx.stage.FileChooser();
        chon.setTitle("Lưu tệp tải về");
        chon.setInitialFileName(TruyenTepTCP.tenLuuAnToan(ten));
        var dich = chon.showSaveDialog(getScene().getWindow());
        if (dich == null) {
            return;
        }
        String trangThai = "Tải " + ten;
        batTienTrinh(trangThai);
        dieuKhien.taiTep(DuLieu.chu(tin, "id"), dich.toPath(), capNhatTienTrinh(trangThai))
                .whenComplete((tep, sai) -> Platform.runLater(() -> {
                    if (daDong) {
                        return;
                    }
                    vungTruyenTep.setVisible(false);
                    loi.setText(
                            sai == null ? "Đã tải tệp: " + tep.toAbsolutePath() : DoHoaView.thongBao(sai));
                }));
    }

    private void capNhatHoSo(JsonObject moi) {
        nguoiDung.entrySet().clear();
        moi.entrySet().forEach(cap -> nguoiDung.add(cap.getKey(), cap.getValue()));
        nutHoSo.setGraphic(DoHoaView.anhHoSo(nguoiDung, 34));
        khiCapNhatHoSo.accept(nguoiDung);
        if (manHinhHoSo != null) {
            manHinhHoSo.capNhat(nguoiDung);
        }
    }

    private void hienBanBe() {
        if (manHinhHoSo != null) {
            if (!manHinhHoSo.choPhepRoi()) {
                return;
            }
            manHinhHoSo.dong();
            manHinhHoSo = null;
        }
        dongBangThongTin();
        chonTabTrai("banBe");
        Button quayLai = DoHoaView.nut("← Quay về trò chuyện", "nutHoSoPhu");
        quayLai.setOnAction(suKien -> {
            if (hienChat()) {
                danhDauDangDoc();
            }
        });
        HBox dau = new HBox(quayLai);
        dau.getStyleClass().add("dauTrangHoSo");
        StackPane khung = new StackPane(hopBanBe);
        khung.setAlignment(Pos.TOP_CENTER);
        khung.setPadding(new Insets(20, 24, 24, 24));
        BorderPane trang = new BorderPane(khung);
        trang.setTop(dau);
        trang.getStyleClass().add("trangHoSo");
        setCenter(trang);
        hopBanBe.taiDanhSach();
    }

    private void moHopThu(boolean luuTru) {
        if (!hienChat()) {
            return;
        }
        xemLuuTru = luuTru;
        chonTabTrai("tinNhan");
        soNguoi.setText(luuTru ? "ĐÃ LƯU TRỮ" : "CUỘC TRÒ CHUYỆN");
        danhSach.setPlaceholder(
                DoHoaView.nhan(luuTru ? "Chưa có cuộc trò chuyện lưu trữ." : "Chưa có cuộc trò chuyện.",
                        "chuGoiY"));
        locDanhSach();
        danhDauDangDoc();
    }

    private ContextMenu menuLuuTru(String dich) {
        boolean daLuu = daLuuTru(dich);
        MenuItem muc = new MenuItem(daLuu ? "Bỏ lưu trữ cuộc trò chuyện" : "Lưu trữ cuộc trò chuyện");
        muc.setOnAction(e -> dieuKhien.luuTru(dich, daLuu)
                .whenComplete((tra, sai) -> Platform.runLater(() -> {
                    if (daDong) {
                        return;
                    }
                    if (sai != null) {
                        loi.setText(DoHoaView.thongBao(sai));
                    } else {
                        sauKhiLuuTru(dich);
                    }
                })));
        return new ContextMenu(muc);
    }

    Runnable moBangThongTin(javafx.scene.Node bang) {
        setRight(bang);
        cotHoiThoai.setPrefWidth(220);
        return () -> {
            if (getRight() == bang) {
                dongBangThongTin();
            }
        };
    }

    private void dongBangThongTin() {
        setRight(null);
        cotHoiThoai.setPrefWidth(300);
    }

    private void denTinChiaSe(String id) {
        if (getCenter() == vungChat) {
            denTinGhim(id);
        } else if (hienChat()) {
            denTinGhim(id);
        }
    }

    boolean daLuuTru(String dich) {
        return danhSachModel.daLuuTru(dich);
    }

    private void sauKhiXoaHoiThoai(String dich) {
        danhSachModel.xoaHoiThoai(dich);
        if (Objects.equals(doiPhuong, dich)) {
            lichSuModel.maCacTin().forEach(dieuKhienAnh::xoa);
            chonDich(null, "Phòng chat chung");
        }
        locDanhSach();
        taiHoiThoai();
    }

    void sauKhiLuuTru(String dich) {
        taiHoiThoai();
        if (Objects.equals(doiPhuong, dich)) {
            chonDich(null, "Phòng chat chung");
            locDanhSach();
        }
    }

    Runnable moTrangChiTiet(javafx.scene.Node trang) {
        javafx.scene.Node cu = getCenter(), bang = getRight();
        dongBangThongTin();
        boolean hienCot = cotHoiThoai.isVisible();
        cotHoiThoai.setVisible(false);
        cotHoiThoai.setManaged(false);
        setCenter(trang);
        return () -> {
            if (getCenter() != trang) {
                return;
            }
            setCenter(cu);
            if (bang != null) {
                moBangThongTin(bang);
            }
            cotHoiThoai.setVisible(hienCot);
            cotHoiThoai.setManaged(hienCot);
            if (cu == vungChat) {
                danhDauDangDoc();
            }
        };
    }

    public void khiHienLai() {
        danhDauDangDoc();
    }

    private void hienHoSo() {
        if (manHinhHoSo != null) {
            return;
        }
        oCuoiKhiMoHoSo =
                lichSu.getVvalue() >= 0.95 || cacTin.getHeight() <= lichSu.getViewportBounds().getHeight();
        manHinhHoSo = new HoSoView(kenh, nguoiDung, this::capNhatHoSo, khiChuyenTaiKhoan, () -> {
            if (!hienChat()) {
                return;
            }
            if (oCuoiKhiMoHoSo) {
                cuonVeCuoi();
            } else {
                Platform.runLater(this::danhDauDangDoc);
            }
        });
        dongBangThongTin();
        chonTabTrai("hoSo");
        setCenter(manHinhHoSo);
    }

    private boolean hienChat() {
        if (manHinhHoSo != null) {
            if (!manHinhHoSo.choPhepRoi()) {
                return false;
            }
            manHinhHoSo.dong();
            manHinhHoSo = null;
        }
        dongBangThongTin();
        chatNhom = null;
        chonTabTrai("tinNhan");
        setCenter(vungChat);
        return true;
    }

    public void dong() {
        ghiAm.huy();
        dieuKhienGoiThoai.dong();
        dieuKhienAnh.dong();
        if (manHinhHoSo != null) {
            manHinhHoSo.dong();
        }
        hopBanBe.dong();
        if (cuaSo != null) {
            cuaSo.focusedProperty().removeListener(khiDoiTieuDiem);
        }
        daDong = true;
        lanTai++;
        dieuKhien.dong();
    }
}
