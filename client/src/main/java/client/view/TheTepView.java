package client.view;

import client.controller.AnhTinNhanController;
import client.hatang.TruyenTepTCP;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

/**
 * Ảnh tự hiện trong chat; các tệp thông thường giữ tên và nút tải.
 */
final class TheTepView extends VBox {
    private final StackPane vungAnh = new StackPane();
    private final AnhTinNhanController dieuKhienAnh;
    private final String id;
    private ScrollPane khungCuon;
    private boolean daYeuCau, daHenTai, daHienAnh;
    private final InvalidationListener khiDoiViTri = giaTri -> henTaiAnh();

    TheTepView(JsonObject tin, Consumer<JsonObject> khiTai, AnhTinNhanController dieuKhienAnh) {
        this.dieuKhienAnh = dieuKhienAnh;
        this.id = DuLieu.chu(tin, "id");
        JsonObject tep = tin.getAsJsonObject("tep");
        String ten = DuLieu.chu(tep, "tenTep");
        boolean laAnh = DuLieu.chu(tep, "loaiTep").equals("anh");
        getStyleClass().add("theTep");
        setSpacing(10);
        setMaxWidth(430);
        if (laAnh) {
            getStyleClass().add("theAnhChat");
            setMaxWidth(332);
            vungAnh.getStyleClass().add("khungAnhChat");
            vungAnh.setPrefSize(320, 220);
            getChildren().add(vungAnh);
            hienDangTai();
            sceneProperty().addListener((giaTri, cu, moi) -> {
                if (cu != null) {
                    boTheoDoi();
                    if (!daHienAnh) {
                        daYeuCau = false;
                    }
                }
                if (moi != null) {
                    localToSceneTransformProperty().addListener(khiDoiViTri);
                    henTaiAnh();
                }
            });
        } else {
            Label nhanTen = DoHoaView.nhan(ten, "tenTep");
            nhanTen.setWrapText(true);
            nhanTen.setMaxWidth(380);
            getChildren().add(nhanTen);
        }
        String chu = DuLieu.chu(tin, "noiDung");
        if (!chu.equals("Tệp: " + ten)) {
            Label chuThich = DoHoaView.nhan(chu, "chuThichTep");
            chuThich.setWrapText(true);
            chuThich.setMaxWidth(laAnh ? 308 : 380);
            getChildren().add(chuThich);
        }
        Label kichThuoc =
                DoHoaView.nhan(TruyenTepTCP.dungLuong(tep.get("kichThuoc").getAsLong()), "moTaTep");
        HBox chan = new HBox(12, kichThuoc);
        chan.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(chan);
        setAccessibleText(laAnh ? "Ảnh trong cuộc trò chuyện. " + (chu.equals("Tệp: " + ten) ? "" : chu) :
                "Tệp " + ten);
    }

    private void hienDangTai() {
        ProgressIndicator cho = new ProgressIndicator();
        cho.setMaxSize(26, 26);
        VBox nhom = new VBox(10, cho, DoHoaView.nhan("Đang tải ảnh…", "moTaTep"));
        nhom.setAlignment(Pos.CENTER);
        vungAnh.getChildren().setAll(nhom);
    }

    private void henTaiAnh() {
        if (daYeuCau || daHenTai || getScene() == null) {
            return;
        }
        daHenTai = true;
        Platform.runLater(() -> {
            daHenTai = false;
            thuTaiAnh();
        });
    }

    private void thuTaiAnh() {
        if (daYeuCau || getScene() == null) {
            return;
        }
        if (khungCuon == null) {
            javafx.scene.Parent cha = getParent();
            while (cha != null && !(cha instanceof ScrollPane)) {
                cha = cha.getParent();
            }
            if (cha instanceof ScrollPane cuon) {
                khungCuon = cuon;
                cuon.vvalueProperty().addListener(khiDoiViTri);
                cuon.viewportBoundsProperty().addListener(khiDoiViTri);
            }
        }
        // Tự tải khi ảnh xuất hiện trong vùng chat, tránh tải toàn bộ lịch sử cũ ngoài màn hình.
        if (khungCuon != null) {
            var vungNhin = khungCuon.lookup(".viewport");
            if (vungNhin == null) {
                henTaiAnh();
                return;
            }
            if (!localToScene(getBoundsInLocal()).intersects(
                    vungNhin.localToScene(vungNhin.getBoundsInLocal()))) {
                return;
            }
        }
        daYeuCau = true;
        dieuKhienAnh.tai(id).whenComplete((anh, loi) -> {
            if (getScene() == null) {
                return;
            }
            if (loi != null) {
                hienLoi();
                return;
            }
            try {
                hienAnh(anh);
            } catch (IllegalArgumentException sai) {
                dieuKhienAnh.xoa(id);
                hienLoi();
            }
        });
    }

    private void hienLoi() {
        Label nhan = DoHoaView.nhan("Chưa tải được ảnh", "moTaTep");
        Button thu = DoHoaView.nut("Thử lại", "nutTep");
        thu.setOnAction(suKien -> {
            daYeuCau = false;
            hienDangTai();
            henTaiAnh();
        });
        VBox nhom = new VBox(10, nhan, thu);
        nhom.setAlignment(Pos.CENTER);
        vungAnh.getChildren().setAll(nhom);
    }

    private void hienAnh(byte[] cacByte) {
        Image anh = new Image(new ByteArrayInputStream(cacByte));
        if (anh.isError()) {
            throw new IllegalArgumentException();
        }
        ScrollPane cuon = khungCuon;
        boolean oCuoi = cuon != null && cuon.getVvalue() > 0.95;
        double tiLe = Math.min(1, Math.min(320 / anh.getWidth(), 300 / anh.getHeight()));
        double rong = anh.getWidth() * tiLe, cao = anh.getHeight() * tiLe;
        ImageView hinh = new ImageView(anh);
        hinh.setFitWidth(rong);
        hinh.setFitHeight(cao);
        hinh.setPreserveRatio(true);
        hinh.setSmooth(true);
        hinh.setAccessibleText("Ảnh trong tin nhắn");
        daHienAnh = true;
        vungAnh.setPrefSize(Math.max(220, rong), cao);
        vungAnh.getChildren().setAll(hinh);
        if (oCuoi) {
            Platform.runLater(() -> {
                if (getScene() != null && khungCuon == cuon) {
                    cuon.setVvalue(1);
                }
            });
        }
    }

    private void boTheoDoi() {
        localToSceneTransformProperty().removeListener(khiDoiViTri);
        if (khungCuon != null) {
            khungCuon.vvalueProperty().removeListener(khiDoiViTri);
            khungCuon.viewportBoundsProperty().removeListener(khiDoiViTri);
            khungCuon = null;
        }
    }
}
