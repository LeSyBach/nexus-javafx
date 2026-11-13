package client.view;

import server.dao.NguoiDungDao;
import server.dao.TinNhanDao;
import server.hatang.BaoMat;
import server.hatang.CauHinh;
import client.hatang.KenhTCP;
import server.dao.CSDLDao;
import server.hatang.MayChuChat;
import client.hatang.TruyenTepTCP;

import server.hatang.*;
import server.model.*;
import server.controller.*;
import giaothuc.*;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Kiểm tra bằng nút thật trên JavaFX; server TCP và CSDL thật với tài khoản thử riêng.
 */
public class KiemTraTuongTac {
    static Stage cuaSo;
    static HoiThoaiView manHinh;
    static CSDLDao kho;
    static String idA, idB;

    static <T> T trenGiaoDien(Callable<T> viec) throws Exception {
        FutureTask<T> cho = new FutureTask<>(() -> {
            T tra = viec.call();
            if (Window.getWindows().stream().filter(w -> w instanceof Stage && w.isShowing()).count() > 1) {
                throw new AssertionError("Mở thêm cửa sổ");
            }
            return tra;
        });
        Platform.runLater(cho);
        return cho.get(30, TimeUnit.SECONDS);
    }

    static void doi(BooleanSupplier dieuKien) throws Exception {
        long het = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        while (System.nanoTime() < het) {
            if (dieuKien.getAsBoolean()) {
                return;
            }
            Thread.sleep(70);
        }
        throw new AssertionError("Quá thời gian chờ");
    }

    static List<Node> cacNut(Node nut) {
        List<Node> tra = new ArrayList<>();
        tra.add(nut);
        if (nut instanceof Parent cha) {
            for (Node con : cha.getChildrenUnmodifiable()) {
                tra.addAll(cacNut(con));
            }
        }
        return tra;
    }

    static boolean coChu(Node goc, String chu) {
        return cacNut(goc).stream().anyMatch(n -> n instanceof Label l && l.getText().contains(chu));
    }

    static Node timNut(String tin, String lop) {
        return cacNut(manHinh.lookup("#tinNhan" + tin)).stream().filter(n -> n.getStyleClass().contains(lop))
                .findFirst().orElseThrow();
    }

    static boolean hienTin(String id) {
        try {
            return trenGiaoDien(() -> manHinh.lookup("#tinNhan" + id) != null);
        } catch (Exception loi) {
            return false;
        }
    }

    static void bam(String id, String lop) throws Exception {
        if (lop.equals("nutThuHoiTin")) {
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.getScene().getRoot().lookup("#xacNhanNoi") == null);
                } catch (Exception loi) {
                    return false;
                }
            });
        }
        trenGiaoDien(() -> {
            if (lop.equals("nutTraLoiTin") || lop.equals("nutThuHoiTin")) {
                ContextMenu menu = (ContextMenu) manHinh.lookup("#tinNhan" + id).getUserData();
                String nhan = lop.equals("nutTraLoiTin") ? "Trả lời" : "Thu hồi";
                menu.getItems().stream().filter(m -> m.getText().equals(nhan)).findFirst().orElseThrow()
                        .fire();
            } else {
                ((ButtonBase) (lop.equals("nutHuyThuHoi") || lop.equals("nutXacNhanThuHoi") ?
                        manHinh.getScene().getRoot().lookup("." + lop) : timNut(id, lop))).fire();
            }
            return null;
        });
    }

    static JsonObject timTin(String chu) throws Exception {
        for (JsonElement t : new TinNhanDao(kho).lichSu(Long.parseLong(idA), Long.parseLong(idB), null)) {
            if (DuLieu.chu(t.getAsJsonObject(), "noiDung").equals(chu)) {
                return t.getAsJsonObject();
            }
        }
        return null;
    }

    static JsonObject docTin(KenhTCP kenh, String id) throws Exception {
        for (JsonElement t : kenh.yeuCau("lichSu", DuLieu.tao("nguoiNhanId", idA)).get()
                .getAsJsonArray("tinNhan")) {
            if (DuLieu.chu(t.getAsJsonObject(), "id").equals(id)) {
                return t.getAsJsonObject();
            }
        }
        throw new AssertionError();
    }

    static void chup(Path dich) throws Exception {
        trenGiaoDien(() -> {
            manHinh.applyCss();
            manHinh.layout();
            var anh = manHinh.snapshot(null, null);
            var doc = anh.getPixelReader();
            BufferedImage png = new BufferedImage((int) anh.getWidth(), (int) anh.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < png.getHeight(); y++) {
                for (int x = 0; x < png.getWidth(); x++) {
                    png.setRGB(x, y, doc.getArgb(x, y));
                }
            }
            ImageIO.write(png, "PNG", dich.toFile());
            return null;
        });
    }

    static void chupNoi(Path dich) throws Exception {
        trenGiaoDien(() -> {
            Parent goc = manHinh.getScene().getRoot();
            goc.applyCss();
            goc.layout();
            var anh = goc.snapshot(null, null);
            var doc = anh.getPixelReader();
            BufferedImage png = new BufferedImage((int) anh.getWidth(), (int) anh.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < png.getHeight(); y++) {
                for (int x = 0; x < png.getWidth(); x++) {
                    png.setRGB(x, y, doc.getArgb(x, y));
                }
            }
            ImageIO.write(png, "PNG", dich.toFile());
            return null;
        });
    }

    private static Button nut(String ten) {
        manHinh.applyCss();
        manHinh.layout();
        return cacNut(manHinh).stream().filter(n -> n instanceof Button b &&
                        (ten.equals(b.getText()) || ten.equals(b.getAccessibleText())))
                .map(Button.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu nút " + ten));
    }

    @SuppressWarnings("unchecked")
    private static boolean chonNguoi(String id) {
        var ds = (ListView<JsonObject>) manHinh.lookup(".danhSachNguoi");
        for (JsonObject dong : ds.getItems()) {
            if (DuLieu.chu(dong, "id").equals(id)) {
                ds.getSelectionModel().select(dong);
                return true;
            }
        }
        return false;
    }

    private static void bamMenuHoiThoai(String id, String ten) {
        manHinh.applyCss();
        manHinh.layout();
        for (Node n : manHinh.lookupAll(".list-cell")) {
            if (n instanceof ListCell<?> dong && dong.getItem() instanceof JsonObject ho &&
                    DuLieu.chu(ho, "id").equals(id)) {
                dong.getContextMenu().getItems().stream().filter(m -> ten.equals(m.getText())).findFirst()
                        .orElseThrow().fire();
                return;
            }
        }
        throw new AssertionError("Không tìm thấy dòng hội thoại");
    }

    private static void kiemTraNhom(KenhTCP a, KenhTCP b, Path goc) throws Exception {
        a.yeuCau("thaoTacBanBe", DuLieu.tao("doiPhuongId", idB, "thaoTac", "gui")).get();
        b.yeuCau("thaoTacBanBe", DuLieu.tao("doiPhuongId", idA, "thaoTac", "chapNhan")).get();
        Thread.sleep(400);
        trenGiaoDien(() -> {
            ((Button) manHinh.lookup("#moTaoNhom")).fire();
            return null;
        });
        doi(() -> {
            try {
                return trenGiaoDien(() -> manHinh.getScene().getRoot().lookup("#chonBan" + idA) != null);
            } catch (Exception e) {
                return false;
            }
        });
        trenGiaoDien(() -> {
            ((TextField) manHinh.getScene().getRoot().lookup("#tenNhomMoi")).setText("Nhóm đồ án NEXUS");
            ((CheckBox) manHinh.getScene().getRoot().lookup("#chonBan" + idA)).fire();
            return null;
        });
        chupNoi(goc.resolve("hop-tao-nhom-nexus.png"));
        trenGiaoDien(() -> {
            ((Button) manHinh.getScene().getRoot().lookup("#xacNhanTaoNhom")).fire();
            return null;
        });
        doi(() -> {
            try {
                return trenGiaoDien(() -> manHinh.lookup("#chatNhom") != null);
            } catch (Exception e) {
                return false;
            }
        });
        trenGiaoDien(() -> {
            manHinh.applyCss();
            manHinh.layout();
            TextArea o = (TextArea) manHinh.lookup("#noiDungNhom");
            o.setText("Chào nhóm, mình thử gửi qua TCP.");
            Button gui = (Button) manHinh.lookup("#guiTinNhom");
            if (gui.isDisabled()) {
                throw new AssertionError(
                        "Nút gửi nhóm đang tắt: " + o.getText() + " / " + manHinh.isDisabled());
            }
            gui.fire();
            return null;
        });
        try {
            doi(() -> {
                try {
                    return trenGiaoDien(() -> {
                        manHinh.applyCss();
                        manHinh.layout();
                        return coChu(manHinh.lookup("#chatNhom"), "Chào nhóm, mình thử gửi qua TCP.");
                    });
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (AssertionError sai) {
            System.out.println(trenGiaoDien(() -> cacNut(manHinh).stream().filter(n -> n instanceof Label)
                    .map(n -> ((Label) n).getText()).toList()).toString());
            throw sai;
        }
        JsonArray nhomCuaA = a.yeuCau("layNhom", new JsonObject()).get().getAsJsonArray("cacNhom");
        String maNhom = DuLieu.chu(nhomCuaA.get(0).getAsJsonObject(), "id");
        a.yeuCau("guiTinNhom",
                DuLieu.tao("nhomId", maNhom, "noiDung", "Máy thứ hai đã nhận được.", "maGui",
                        UUID.randomUUID().toString())).get();
        doi(() -> {
            try {
                return trenGiaoDien(() -> coChu(manHinh.lookup("#chatNhom"), "Máy thứ hai đã nhận được."));
            } catch (Exception e) {
                return false;
            }
        });
        chup(goc.resolve("chat-nhom-nexus.png"));
        System.out.println("PASS: tạo nhóm từ bạn bè và nhắn tin nhóm qua hai máy.");
    }

    public static void main(String[] args) throws Exception {
        kho = new CSDLDao(CauHinh.doc(Path.of("D:/nexus-javafx/cau-hinh/may-chu.properties")));
        Path goc = Path.of("D:/nexus-javafx/nhat-ky/kiem-tra");
        Path tam = Files.createTempDirectory(goc, "qa-tuong-tac-");
        MayChuChat mayChu = new MayChuChat(kho, "127.0.0.1", 0, tam.resolve("server"));
        Thread luong = new Thread(() -> {
            try {
                mayChu.phucVu();
            } catch (Exception loi) {
            }
        });
        luong.start();
        String emailA = "qa-tuong-tac-ui-" + UUID.randomUUID() + "@example.invalid", emailB =
                "qa-tuong-tac-ui-" + UUID.randomUUID() + "@example.invalid", matKhau = "TuongTacUI2026!";
        KenhTCP a = new KenhTCP(), b = new KenhTCP();
        TruyenTepTCP truyen = null;
        CountDownLatch sanSang = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            sanSang.countDown();
        });
        sanSang.await();
        try {
            String bam = BaoMat.bam(matKhau);
            idA = Long.toString(new NguoiDungDao(kho).themNguoi("An Nguyễn", emailA, bam).id());
            idB = Long.toString(new NguoiDungDao(kho).themNguoi("Bình Trần", emailB, bam).id());
            a.ketNoi("127.0.0.1", mayChu.cong()).get();
            a.yeuCau("dangNhap", DuLieu.tao("email", emailA, "matKhau", matKhau)).get();
            a.startHeartbeat();
            b.ketNoi("127.0.0.1", mayChu.cong()).get();
            JsonObject nguoiB = b.yeuCau("dangNhap", DuLieu.tao("email", emailB, "matKhau", matKhau)).get()
                    .getAsJsonObject("nguoiDung");
            b.startHeartbeat();
            JsonObject tinA = a.yeuCau("guiTin", DuLieu.tao("nguoiNhanId", idB, "noiDung",
                            "Chiều nay mình cùng hoàn thiện bài TCP nhé!", "maGui", UUID.randomUUID().toString()))
                    .get().getAsJsonObject("tinNhan");
            String maA = DuLieu.chu(tinA, "id");
            trenGiaoDien(() -> {
                cuaSo = new Stage();
                manHinh = new HoiThoaiView(b, nguoiB, () -> {
                });
                Scene canh = new Scene(manHinh, 1120, 850);
                canh.getStylesheets().add(Path.of(
                                "D:/nexus-javafx/client/src/main/resources/giao-dien.css").toUri()
                        .toString());
                cuaSo.setScene(canh);
                cuaSo.show();
                manHinh.batDau();
                return null;
            });
            if (args.length > 0 && args[0].equals("nhom")) {
                kiemTraNhom(a, b, goc);
                return;
            }
            trenGiaoDien(() -> {
                Node ban = manHinh.lookup(".nutMoBanBe");
                if (!ban.getParent().getStyleClass().contains("thanhDieuHuong")) {
                    throw new AssertionError("Bạn bè sai vị trí");
                }
                ((Button) ban).fire();
                if (!manHinh.lookup(".cotDanhSach").isManaged()) {
                } else {
                    throw new AssertionError("Cột chat còn hiện ở tab Bạn bè");
                }
                ((Button) manHinh.lookup(".nutTabTrai")).fire();
                ThongBaoNoiView.xacNhan(manHinh, "Đăng xuất tài khoản",
                        "Kiểm tra xác nhận trước khi đăng xuất.", "Đăng xuất", () -> {
                            throw new AssertionError("Hủy vẫn thực hiện");
                        });
                if (manHinh.getScene().getRoot().lookup("#xacNhanNoi") == null || !manHinh.isDisabled()) {
                    throw new AssertionError("Không khóa nền");
                }
                ((Button) manHinh.getScene().getRoot().lookup(".nutHuyThuHoi")).fire();
                if (manHinh.isDisabled()) {
                    throw new AssertionError("Hủy chưa mở khóa nền");
                }
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(
                            () -> ((ListView<JsonObject>) manHinh.lookup(".danhSachNguoi")).getItems()
                                    .stream().anyMatch(n -> DuLieu.chu(n, "id").equals(idA)));
                } catch (Exception loi) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                var ds = (ListView<JsonObject>) manHinh.lookup(".danhSachNguoi");
                ds.getItems().stream().filter(n -> DuLieu.chu(n, "id").equals(idA)).findFirst()
                        .ifPresent(n -> ds.getSelectionModel().select(n));
                return null;
            });
            doi(() -> hienTin(maA));
            bam(maA, "nutTraLoiTin");
            trenGiaoDien(() -> {
                if (!manHinh.lookup(".thanhTraLoi").isVisible()) {
                    throw new AssertionError("Không hiện trích dẫn");
                }
                ((TextArea) manHinh.lookup(".text-area")).setText(
                        "Được nhé, mình đã chuẩn bị xong tài liệu.");
                ((Button) manHinh.lookup(".nutGui")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return timTin("Được nhé, mình đã chuẩn bị xong tài liệu.") != null;
                } catch (Exception loi) {
                    return false;
                }
            });
            JsonObject tinB = timTin("Được nhé, mình đã chuẩn bị xong tài liệu.");
            String maB = DuLieu.chu(tinB, "id");
            doi(() -> hienTin(maB));
            trenGiaoDien(() -> {
                ContextMenu menu = (ContextMenu) manHinh.lookup("#tinNhan" + maB).getUserData();
                menu.getItems().stream().filter(m -> m.getText().equals("Ghim tin nhắn")).findFirst()
                        .orElseThrow().fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup("#thanhGhimTin").isVisible());
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                ((Button) manHinh.lookup(".nutXemGhim")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup(".tinDuocDinhVi") != null);
                } catch (Exception e) {
                    return false;
                }
            });
            chup(goc.resolve("thanh-ghim-theo-android.png"));
            b.yeuCau("ghimTin", DuLieu.tao("tinNhanId", maA)).get();
            JsonObject ghim =
                    a.yeuCau("thongTinChat", DuLieu.tao("nguoiNhanId", idB, "loai", "ghim")).get();
            if (ghim.getAsJsonArray("tinNhan").size() != 1 ||
                    !DuLieu.chu(ghim.getAsJsonObject("ghimMoi"), "id").equals(maA)) {
                throw new AssertionError("Ghim mới phải thay tin ghim cũ ở cả hai phía");
            }
            b.yeuCau("boGhimTin", DuLieu.tao("tinNhanId", maA)).get();
            doi(() -> {
                try {
                    return trenGiaoDien(() -> !manHinh.lookup("#thanhGhimTin").isVisible());
                } catch (Exception e) {
                    return false;
                }
            });
            if (!DuLieu.chu(tinB, "traLoiId").equals(maA)) {
                throw new AssertionError("Gửi thiếu mã trả lời");
            }
            trenGiaoDien(() -> {
                Node tin = manHinh.lookup("#tinNhan" + maA);
                var diem = tin.localToScreen(10, 10);
                tin.fireEvent(new javafx.scene.input.ContextMenuEvent(
                        javafx.scene.input.ContextMenuEvent.CONTEXT_MENU_REQUESTED, 10, 10, diem.getX(),
                        diem.getY(), false, null));
                ContextMenu menu = (ContextMenu) tin.getUserData();
                if (!menu.isShowing()) {
                    throw new AssertionError("Chuột phải không mở menu");
                }
                menu.getItems().stream().filter(m -> m.getText().equals("Sao chép")).findFirst().orElseThrow()
                        .fire();
                if (!javafx.scene.input.Clipboard.getSystemClipboard().getString().contains("Chiều nay")) {
                    throw new AssertionError("Sao chép sai");
                }
                if (cacNut(tin).stream().anyMatch(n -> n.getStyleClass().contains("nutThaoTacTin"))) {
                    throw new AssertionError("Còn nút thao tác hiện sẵn");
                }
                Menu cam = (Menu) menu.getItems().stream().filter(m -> m instanceof Menu).findFirst()
                        .orElseThrow();
                menu.hide();
                cam.getItems().stream().filter(m -> m.getText().startsWith("❤️")).findFirst().orElseThrow()
                        .fire();
                return null;
            });
            doi(() -> {
                try {
                    return DuLieu.chu(docTin(b, maA), "camXucCuaToi").equals("❤️");
                } catch (Exception loi) {
                    return false;
                }
            });
            a.yeuCau("datCamXuc", DuLieu.tao("tinNhanId", maA, "bieuCam", "👍")).get();
            doi(() -> {
                try {
                    return trenGiaoDien(() -> cacNut(manHinh.lookup("#tinNhan" + maA)).stream()
                            .filter(n -> n.getStyleClass().contains("nutCamXucTin")).count() == 2);
                } catch (Exception loi) {
                    return false;
                }
            });
            chup(goc.resolve("tuong-tac-trong-chat.png"));
            trenGiaoDien(() -> {
                ((Button) timNut(maA, "camXucCuaMinh")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return DuLieu.chu(docTin(b, maA), "camXucCuaToi").isEmpty();
                } catch (Exception loi) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                ContextMenu menu = (ContextMenu) manHinh.lookup("#tinNhan" + maB).getUserData();
                menu.getItems().stream().filter(m -> m.getText().equals("Chuyển tiếp")).findFirst()
                        .orElseThrow().fire();
                return null;
            });
            trenGiaoDien(() -> {
                Parent hop = (Parent) manHinh.getScene().getRoot().lookup("#manHinhChiaSe");
                Parent hang = (Parent) cacNut(hop).stream()
                        .filter(n -> n.getStyleClass().contains("dongChiaSe") && coChu(n, "An Nguyễn"))
                        .findFirst().orElseThrow();
                ((CheckBox) cacNut(hang).stream().filter(n -> n instanceof CheckBox).findFirst()
                        .orElseThrow()).setSelected(true);
                cacNut(hop).stream()
                        .filter(n -> n instanceof ToggleButton bt && bt.getText().equals("Phòng chung"))
                        .map(n -> (ToggleButton) n).findFirst().orElseThrow().fire();
                ((CheckBox) cacNut(hop).stream().filter(n -> n instanceof CheckBox).findFirst()
                        .orElseThrow()).setSelected(true);
                ((TextArea) hop.lookup("#loiNhanChiaSe")).setText("Mình chia sẻ cho hai cuộc trò chuyện.");
                if (!((Button) hop.lookup("#guiChiaSe")).getText().equals("Chia sẻ (2)")) {
                    throw new AssertionError("Không chọn được hai nơi nhận");
                }
                return null;
            });
            chupNoi(goc.resolve("chia-se-hoi-thoai.png"));
            trenGiaoDien(() -> {
                ((Button) manHinh.getScene().getRoot().lookup("#guiChiaSe")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(
                            () -> manHinh.getScene().getRoot().lookup("#manHinhChiaSe") == null);
                } catch (Exception loi) {
                    return false;
                }
            });
            // Tệp cũng trả lời được một tin cụ thể.
            Path tep = tam.resolve("Bài thực hành TCP.txt");
            Files.writeString(tep, "Tài liệu trả lời qua TCP");
            bam(maA, "nutTraLoiTin");
            trenGiaoDien(() -> {
                manHinh.chonTep(tep);
                ((Button) manHinh.lookup(".nutGui")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return timTin("Tệp: " + tep.getFileName()) != null;
                } catch (Exception loi) {
                    return false;
                }
            });
            JsonObject tinTep = timTin("Tệp: " + tep.getFileName());
            String maTep = DuLieu.chu(tinTep, "id");
            if (!DuLieu.chu(tinTep, "traLoiId").equals(maA)) {
                throw new AssertionError("Tệp thiếu trích dẫn");
            }
            doi(() -> hienTin(maTep));
            // Người khác thu hồi: bỏ trả lời đang chọn, giữ bản nháp, xóa nội dung gốc trong mọi trích dẫn.
            bam(maA, "nutTraLoiTin");
            trenGiaoDien(() -> {
                ((TextArea) manHinh.lookup(".text-area")).setText("Bản nháp cần giữ");
                return null;
            });
            a.yeuCau("thuHoiTin", DuLieu.tao("tinNhanId", maA)).get();
            doi(() -> {
                try {
                    return trenGiaoDien(
                            () -> coChu(manHinh.lookup("#tinNhan" + maA), "Tin nhắn đã được thu hồi") &&
                                    !manHinh.lookup(".thanhTraLoi").isVisible());
                } catch (Exception loi) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                if (!((TextArea) manHinh.lookup(".text-area")).getText().equals("Bản nháp cần giữ")) {
                    throw new AssertionError("Mất bản nháp");
                }
                if (coChu(manHinh.lookup("#tinNhan" + maB), "Chiều nay")) {
                    throw new AssertionError("Lộ nội dung trong trích dẫn");
                }
                return null;
            });
            // Phản hồi cũ đến muộn không phục hồi lại tin đã thu hồi.
            trenGiaoDien(() -> {
                var ghi = HoiThoaiView.class.getDeclaredMethod("ghiNhanTin", JsonObject.class);
                ghi.setAccessible(true);
                ghi.invoke(manHinh, tinA);
                var ve = HoiThoaiView.class.getDeclaredMethod("veTin", boolean.class);
                ve.setAccessible(true);
                ve.invoke(manHinh, false);
                if (coChu(manHinh.lookup("#tinNhan" + maA), "Chiều nay")) {
                    throw new AssertionError("Dữ liệu cũ khôi phục tin");
                }
                return null;
            });
            // Xác nhận thu hồi ở cùng bong bóng; chọn giữ lại không gọi server.
            bam(maB, "nutThuHoiTin");
            chupNoi(goc.resolve("xac-nhan-noi.png"));
            bam(maB, "nutHuyThuHoi");
            if (docTin(b, maB).get("daThuHoi").getAsBoolean()) {
                throw new AssertionError("Hủy vẫn thu hồi");
            }
            bam(maB, "nutThuHoiTin");
            bam(maB, "nutXacNhanThuHoi");
            doi(() -> {
                try {
                    return docTin(b, maB).get("daThuHoi").getAsBoolean();
                } catch (Exception loi) {
                    return false;
                }
            });
            bam(maTep, "nutThuHoiTin");
            bam(maTep, "nutXacNhanThuHoi");
            doi(() -> {
                try {
                    return docTin(b, maTep).get("daThuHoi").getAsBoolean();
                } catch (Exception loi) {
                    return false;
                }
            });
            try {
                a.yeuCau("tepDocKhoi", DuLieu.tao("tinNhanId", maTep, "viTri", "0")).get();
                throw new AssertionError("Tải được tệp đã thu hồi");
            } catch (ExecutionException mongDoi) {
            }
            // Lưu qua CSDL: kết nối mới thấy trích dẫn đã thu hồi và cảm xúc đã xóa.
            try (KenhTCP moi = new KenhTCP()) {
                moi.ketNoi("127.0.0.1", mayChu.cong()).get();
                moi.yeuCau("dangNhap", DuLieu.tao("email", emailB, "matKhau", matKhau)).get();
                if (!docTin(moi, maA).get("daThuHoi").getAsBoolean()) {
                    throw new AssertionError("Không lưu thu hồi");
                }
            }
            trenGiaoDien(() -> {
                ContextMenu menu = (ContextMenu) manHinh.lookup("#tinNhan" + maB).getUserData();
                menu.getItems().stream().filter(m -> m.getText().equals("Xóa ở phía tôi")).findFirst()
                        .orElseThrow().fire();
                return null;
            });
            bam(maB, "nutXacNhanThuHoi");
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup("#tinNhan" + maB) == null);
                } catch (Exception loi) {
                    return false;
                }
            });
            chup(goc.resolve("thu-hoi-trong-chat.png"));

            // Tạo nội dung thật cho cả ba mục xem trước.
            truyen = new TruyenTepTCP(a);
            Path anh = tam.resolve("anh-tham-khao.png");
            BufferedImage mau = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
            var coVe = mau.createGraphics();
            coVe.setColor(java.awt.Color.GREEN);
            coVe.fillRect(0, 0, 100, 80);
            coVe.dispose();
            ImageIO.write(mau, "PNG", anh.toFile());
            truyen.gui(anh, idB, "", UUID.randomUUID().toString(), k -> {
            }).get();
            truyen.gui(tep, idB, "", UUID.randomUUID().toString(), k -> {
            }).get();
            a.yeuCau("guiTin",
                    DuLieu.tao("nguoiNhanId", idB, "noiDung", "Tài liệu https://example.org/nexus",
                            "maGui", UUID.randomUUID().toString())).get();
            trenGiaoDien(() -> {
                ((Button) manHinh.lookup("#tenMoThongTin")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> {
                        Node bang = manHinh.lookup("#thongTinHoiThoai");
                        if (bang == null || !coChu(bang, "An Nguyễn") || !coChu(bang, "example.org") ||
                                !coChu(bang, tep.getFileName().toString())) {
                            return false;
                        }
                        return manHinh.lookup("#tinNhan" + maA) != null &&
                                manHinh.lookup(".oAnhThongTin") instanceof Button o &&
                                o.getGraphic() instanceof javafx.scene.image.ImageView;
                    });
                } catch (Exception e) {
                    return false;
                }
            });
            chup(goc.resolve("bang-thong-tin-ben-phai.png"));
            trenGiaoDien(() -> {
                nut("Xem tất cả ảnh").fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> {
                        Node kho = manHinh.lookup("#khoChiaSe");
                        return kho != null && coChu(kho, "Ảnh đã chia sẻ") &&
                                kho.lookup("#luoiAnhChiaSe") instanceof GridPane luoi
                                && luoi.getColumnConstraints().size() == 4 && !luoi.getChildren().isEmpty() &&
                                kho.lookup(".toggle-button") == null;
                    });
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                ((Button) manHinh.lookup("#quayLaiChiTiet")).fire();
                nut("Trang cá nhân").fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> coChu(manHinh, "Tham gia:"));
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                if (coChu(manHinh, emailA)) {
                    throw new AssertionError("Lộ email riêng tư");
                }
                ((Button) manHinh.lookup("#quayLaiChiTiet")).fire();
                if (manHinh.lookup("#thongTinHoiThoai") == null) {
                    throw new AssertionError("Quay lại sai trang");
                }
                nut("Đóng thông tin hội thoại").fire();
                bamMenuHoiThoai(idA, "Lưu trữ cuộc trò chuyện");
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.daLuuTru(idA));
                } catch (Exception e) {
                    return false;
                }
            });
            var hoiA = a.yeuCau("layHoiThoai", DuLieu.tao()).get().getAsJsonArray("hoiThoai");
            if (hoiA.asList().stream().map(JsonElement::getAsJsonObject)
                    .anyMatch(h -> DuLieu.chu(h, "id").equals(idB) && h.get("daLuuTru").getAsBoolean())) {
                throw new AssertionError("Lưu trữ làm thay đổi hộp thư người kia");
            }
            trenGiaoDien(() -> {
                nut("Lưu trữ").fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> chonNguoi(idA));
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                bamMenuHoiThoai(idA, "Bỏ lưu trữ cuộc trò chuyện");
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> !manHinh.daLuuTru(idA));
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                nut("Tin nhắn").fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> chonNguoi(idA));
                } catch (Exception e) {
                    return false;
                }
            });
            doi(() -> hienTin(maA));
            JsonObject hoB = b.yeuCau("layHoSo", new JsonObject()).get().getAsJsonObject("hoSo");
            trenGiaoDien(() -> {
                HoSoView ho = new HoSoView(b, hoB, h -> {
                }, () -> {
                }, () -> {
                });
                cuaSo.getScene().setRoot(ho);
                ho.applyCss();
                ho.layout();
                cacNut(ho).stream()
                        .filter(n -> n instanceof Button bt && bt.getText().equals("Quyền riêng tư"))
                        .map(n -> (Button) n).findFirst().orElseThrow().fire();
                ((RadioButton) ho.lookup("#quyenEmailMoiNguoi")).fire();
                ((RadioButton) ho.lookup("#quyenDienThoaiBanBe")).fire();
                ho.applyCss();
                ho.layout();
                var anhQuyen = ho.snapshot(null, null);
                var docQuyen = anhQuyen.getPixelReader();
                BufferedImage hinhQuyen =
                        new BufferedImage((int) anhQuyen.getWidth(), (int) anhQuyen.getHeight(),
                                BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < hinhQuyen.getHeight(); y++) {
                    for (int x = 0; x < hinhQuyen.getWidth(); x++) {
                        hinhQuyen.setRGB(x, y, docQuyen.getArgb(x, y));
                    }
                }
                ImageIO.write(hinhQuyen, "PNG", goc.resolve("quyen-rieng-tu-moi.png").toFile());
                cacNut(ho).stream()
                        .filter(n -> n instanceof Button bt && bt.getText().equals("Lưu quyền riêng tư"))
                        .map(n -> (Button) n).findFirst().orElseThrow().fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> coChu(cuaSo.getScene().getRoot(), "Đã lưu quyền riêng tư."));
                } catch (Exception e) {
                    return false;
                }
            });
            if (!DuLieu.chu(b.yeuCau("layHoSo", new JsonObject()).get().getAsJsonObject("hoSo"),
                    "quyenEmail").equals("moiNguoi")) {
                throw new AssertionError("Quyền chưa lưu");
            }
            trenGiaoDien(() -> {
                ((HoSoView) cuaSo.getScene().getRoot()).dong();
                if (manHinh.getParent() instanceof Pane cha) {
                    cha.getChildren().remove(manHinh);
                }
                cuaSo.getScene().setRoot(manHinh);
                cuaSo.toFront();
                cuaSo.requestFocus();
                return null;
            });
            // Giữ bong bóng không đổi, giữ vị trí đọc cũ, nút trở về tin mới nhất.
            for (int i = 0; i < 18; i++) {
                a.yeuCau("guiTin", DuLieu.tao("nguoiNhanId", idB, "noiDung",
                        "Đoạn kiểm tra cuộn " + i + "\nDòng hai\nDòng ba", "maGui",
                        UUID.randomUUID().toString())).get();
            }
            doi(() -> {
                try {
                    return trenGiaoDien(() -> coChu(manHinh, "Đoạn kiểm tra cuộn 17"));
                } catch (Exception e) {
                    return false;
                }
            });
            Thread.sleep(500);
            Node giu = trenGiaoDien(() -> manHinh.lookup("#tinNhan" + maA));
            trenGiaoDien(() -> {
                var f = HoiThoaiView.class.getDeclaredField("lichSu");
                f.setAccessible(true);
                ((ScrollPane) f.get(manHinh)).setVvalue(0);
                return null;
            });
            JsonObject tinMoi = a.yeuCau("guiTin",
                    DuLieu.tao("nguoiNhanId", idB, "noiDung", "Tin đến khi đang đọc lịch sử", "maGui",
                            UUID.randomUUID().toString())).get().getAsJsonObject("tinNhan");
            doi(() -> hienTin(DuLieu.chu(tinMoi, "id")));
            Thread.sleep(300);
            chup(goc.resolve("nut-mui-ten-ve-cuoi.png"));
            trenGiaoDien(() -> {
                if (manHinh.lookup("#tinNhan" + maA) != giu) {
                    throw new AssertionError("Dựng lại tin không đổi");
                }
                var f = HoiThoaiView.class.getDeclaredField("lichSu");
                f.setAccessible(true);
                if (((ScrollPane) f.get(manHinh)).getVvalue() > .1) {
                    throw new AssertionError("Nhảy vị trí đọc cũ");
                }
                Button nut = cacNut(manHinh).stream()
                        .filter(n -> n instanceof Button bt && "veCuoiHoiThoai".equals(bt.getId()))
                        .map(n -> (Button) n).findFirst().orElseThrow();
                if (!nut.isVisible()) {
                    throw new AssertionError("Thiếu nút về cuối");
                }
                if (!nut.getText().isEmpty() || nut.getGraphic() == null) {
                    throw new AssertionError("Nút về cuối phải chỉ có icon");
                }
                nut.fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> {
                        var f = HoiThoaiView.class.getDeclaredField("lichSu");
                        f.setAccessible(true);
                        return ((ScrollPane) f.get(manHinh)).getVvalue() > .99;
                    });
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                ((Button) manHinh.lookup("#tenMoThongTin")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> !nut("Chặn").isDisabled());
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                nut("Chặn").fire();
                ((Button) manHinh.getScene().getRoot().lookup(".nutXacNhanThuHoi")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> nut("Bỏ chặn") != null);
                } catch (Exception e) {
                    return false;
                }
            });
            for (int i = 0; i < 2; i++) {
                try {
                    (i == 0 ? a : b).yeuCau("guiTin",
                            DuLieu.tao("nguoiNhanId", i == 0 ? idB : idA, "noiDung", "Tin phải bị chặn",
                                    "maGui", UUID.randomUUID().toString())).get();
                    throw new AssertionError("Vẫn gửi được khi đã chặn");
                } catch (ExecutionException mongDoi) {
                    if (!mongDoi.getCause().getMessage().contains("không thể gửi")) {
                        throw mongDoi;
                    }
                }
            }
            try {
                truyen.gui(tep, idB, "", UUID.randomUUID().toString(), k -> {
                }).get();
                throw new AssertionError("Vẫn gửi được tệp khi đã chặn");
            } catch (ExecutionException mongDoi) {
                if (!mongDoi.getCause().getMessage().contains("không thể gửi")) {
                    throw mongDoi;
                }
            }
            trenGiaoDien(() -> {
                nut("Bỏ chặn").fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> !nut("Chặn").isDisabled());
                } catch (Exception e) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                nut("Xóa cuộc trò chuyện").fire();
                ((Button) manHinh.getScene().getRoot().lookup(".nutXacNhanThuHoi")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(
                            () -> manHinh.lookup("#thongTinHoiThoai") == null && !chonNguoi(idA));
                } catch (Exception e) {
                    return false;
                }
            });
            if (!b.yeuCau("lichSu", DuLieu.tao("nguoiNhanId", idA)).get().getAsJsonArray("tinNhan")
                    .isEmpty()) {
                throw new AssertionError("Lịch sử bên xóa vẫn hiện");
            }
            if (a.yeuCau("lichSu", DuLieu.tao("nguoiNhanId", idB)).get().getAsJsonArray("tinNhan")
                    .isEmpty()) {
                throw new AssertionError("Mất lịch sử ở phía người kia");
            }
            var sauXoa = a.yeuCau("guiTin",
                    DuLieu.tao("nguoiNhanId", idB, "noiDung", "Tin mới sau khi xóa", "maGui",
                            UUID.randomUUID().toString())).get();
            if (b.yeuCau("lichSu", DuLieu.tao("nguoiNhanId", idA)).get().getAsJsonArray("tinNhan")
                    .size() != 1) {
                throw new AssertionError("Không mở lại được bằng tin mới");
            }
            System.out.println(
                    "PASS: bảng bên phải, ảnh/tệp/liên kết, menu lưu trữ, chặn hai chiều và xóa riêng theo tài khoản.");
            System.out.println(
                    "PASS: thong tin chat, ho so rieng tu, luu quyen trong cung cua so, tai su dung bong bong, giu vi tri cuon va nut ve cuoi.");
            System.out.println(
                    "PASS: tra loi van ban/tep, cam xuc truc tiep/bo, trich dan thu hoi, giu ban nhap, phan hoi cu, xac nhan trong chat, chan tai tep, mo lai lich su, chi mot cua so.");
        } finally {
            if (truyen != null) {
                truyen.close();
            }
            trenGiaoDien(() -> {
                if (manHinh != null) {
                    manHinh.dong();
                }
                if (cuaSo != null) {
                    cuaSo.close();
                }
                return null;
            });
            a.close();
            b.close();
            mayChu.close();
            luong.join(5000);
            try (Connection db = kho.moKetNoi();
                 PreparedStatement xoa = db.prepareStatement("DELETE FROM nguoi_dung WHERE email=?")) {
                for (String e : List.of(emailA, emailB)) {
                    xoa.setString(1, e);
                    xoa.executeUpdate();
                }
            }
            Platform.exit();
        }
    }
}
