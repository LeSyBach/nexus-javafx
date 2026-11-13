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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class KiemTraTep {
    static Stage cuaSo;
    static HoiThoaiView manHinh;
    static CSDLDao kho;
    static String idA, idB;

    static <T> T trenGiaoDien(Callable<T> viec) throws Exception {
        FutureTask<T> cho = new FutureTask<>(viec);
        Platform.runLater(cho);
        return cho.get(30, TimeUnit.SECONDS);
    }

    static void doi(BooleanSupplier dieuKien) throws Exception {
        long het = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
        while (System.nanoTime() < het) {
            if (dieuKien.getAsBoolean()) {
                return;
            }
            Thread.sleep(80);
        }
        throw new AssertionError("Qua thoi gian cho");
    }

    static List<Node> cacNut(Node nut) {
        List<Node> kq = new ArrayList<>();
        kq.add(nut);
        if (nut instanceof Parent cha) {
            for (Node con : cha.getChildrenUnmodifiable()) {
                kq.addAll(cacNut(con));
            }
        }
        return kq;
    }

    static boolean coTen(Parent goc, String ten) {
        return cacNut(goc).stream().anyMatch(n -> n instanceof Label l && l.getText().equals(ten));
    }

    static JsonObject timTin(String ten) throws Exception {
        for (JsonElement phanTu : new TinNhanDao(kho).lichSu(Long.parseLong(idA), Long.parseLong(idB), null)) {
            JsonObject tin = phanTu.getAsJsonObject();
            if (tin.has("tep") && DuLieu.chu(tin.getAsJsonObject("tep"), "tenTep").equals(ten)) {
                return tin;
            }
        }
        return null;
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

    public static void main(String[] args) throws Exception {
        kho = new CSDLDao(CauHinh.doc(Path.of("D:/nexus-javafx/cau-hinh/may-chu.properties")));
        Path goc = Path.of("D:/nexus-javafx/nhat-ky/kiem-tra");
        Files.createDirectories(goc);
        Path tam = Files.createTempDirectory(goc, "qa-tep-");
        MayChuChat mayChu = new MayChuChat(kho, "127.0.0.1", 0, tam.resolve("server"));
        Thread luong = new Thread(() -> {
            try {
                mayChu.phucVu();
            } catch (Exception loi) {
            }
        });
        luong.start();
        String emailA = "qa-tep-ui-" + UUID.randomUUID() + "@example.invalid", emailB =
                "qa-tep-ui-" + UUID.randomUUID() + "@example.invalid", matKhau = "TruyenTepUI2026!";
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
            a.yeuCau("guiTin", DuLieu.tao("nguoiNhanId", idB, "noiDung",
                    "Mình gửi tài liệu trong cuộc trò chuyện này nhé.", "maGui",
                    UUID.randomUUID().toString())).get();
            trenGiaoDien(() -> {
                cuaSo = new Stage();
                manHinh = new HoiThoaiView(b, nguoiB, () -> {
                });
                Scene canh = new Scene(manHinh, 1120, 800);
                canh.getStylesheets().add(Path.of(
                                "D:/nexus-javafx/client/src/main/resources/giao-dien.css").toUri()
                        .toString());
                cuaSo.setScene(canh);
                cuaSo.show();
                manHinh.batDau();
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
            byte[] cacByte = new byte[300017];
            new Random(1).nextBytes(cacByte);
            Path tep = tam.resolve("Bài tập lập trình mạng.zip");
            Files.write(tep, cacByte);
            trenGiaoDien(() -> {
                manHinh.chonTep(tep);
                ((TextArea) manHinh.lookup(".text-area")).setText("Mình gửi bài thực hành TCP.");
                ((Button) manHinh.lookup(".nutGui")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return timTin(tep.getFileName().toString()) != null;
                } catch (Exception loi) {
                    return false;
                }
            });
            JsonObject tin = timTin(tep.getFileName().toString());
            doi(() -> {
                try {
                    return trenGiaoDien(() -> coTen(manHinh, tep.getFileName().toString()));
                } catch (Exception loi) {
                    return false;
                }
            });
            truyen = new TruyenTepTCP(a);
            List<Double> tienTrinh = new java.util.concurrent.CopyOnWriteArrayList<>();
            Path taiVe = tam.resolve("nhan-duoc.zip");
            truyen.tai(DuLieu.chu(tin, "id"), taiVe, tienTrinh::add).get(20, TimeUnit.SECONDS);
            if (!Arrays.equals(cacByte, Files.readAllBytes(taiVe))) {
                throw new AssertionError("Tep khong khop");
            }
            if (tienTrinh.stream().noneMatch(so -> so > 0 && so < 1)) {
                throw new AssertionError("Thieu tien trinh theo khoi");
            }
            TruyenTepTCP boTruyen = truyen;
            Path tepCu = tam.resolve("giu-tep-cu.zip");
            Files.writeString(tepCu, "GIỮ TỆP CŨ");
            try {
                truyen.tai(DuLieu.chu(tin, "id"), tepCu, so -> {
                    if (so > 0) {
                        boTruyen.huy();
                    }
                }).get(20, TimeUnit.SECONDS);
                throw new AssertionError("Huy van bao thanh cong");
            } catch (ExecutionException mongDoi) {
            }
            if (!Files.readString(tepCu).equals("GIỮ TỆP CŨ")) {
                throw new AssertionError("Huy ghi de tep dich");
            }
            String maHuy = UUID.randomUUID().toString();
            try {
                truyen.gui(tep, idB, "", maHuy, so -> {
                    if (so > 0) {
                        boTruyen.huy();
                    }
                }).get(20, TimeUnit.SECONDS);
                throw new AssertionError("Huy gui van thanh cong");
            } catch (ExecutionException mongDoi) {
            }
            try (Connection db = kho.moKetNoi();
                 PreparedStatement tim = db.prepareStatement("SELECT id FROM tin_nhan WHERE ma_gui=?")) {
                tim.setString(1, maHuy);
                try (ResultSet dong = tim.executeQuery()) {
                    if (dong.next()) {
                        throw new AssertionError("Tep huy van vao lich su");
                    }
                }
            }
            var hinh = new BufferedImage(700, 440, BufferedImage.TYPE_INT_RGB);
            var ve = hinh.createGraphics();
            ve.setColor(new java.awt.Color(40, 108, 82));
            ve.fillRect(0, 0, 700, 440);
            ve.setColor(new java.awt.Color(239, 205, 130));
            ve.fillOval(230, 90, 240, 240);
            ve.dispose();
            Path anh = tam.resolve("Kỷ niệm nhóm.png");
            ImageIO.write(hinh, "PNG", anh.toFile());
            String maAnh = UUID.randomUUID().toString();
            JsonObject tinAnh = truyen.gui(anh, idB, "Ảnh nhóm cho buổi báo cáo.", maAnh, so -> {
            }).get(20, TimeUnit.SECONDS);
            JsonObject lap = truyen.gui(anh, idB, "Ảnh nhóm cho buổi báo cáo.", maAnh, so -> {
            }).get(20, TimeUnit.SECONDS);
            if (!lap.get("id").equals(tinAnh.get("id"))) {
                throw new AssertionError("Gui lai tao trung");
            }
            String idAnh = DuLieu.chu(tinAnh, "id");
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup("#tinNhan" + idAnh) != null &&
                            cacNut(manHinh.lookup("#tinNhan" + idAnh)).stream()
                                    .anyMatch(n -> n instanceof ImageView));
                } catch (Exception loi) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                if (coTen(manHinh, anh.getFileName().toString())) {
                    throw new AssertionError("Ảnh vẫn hiện tên tệp");
                }
                if (cacNut(manHinh).stream()
                        .anyMatch(n -> n instanceof Button nut && nut.getText().equals("Xem ảnh"))) {
                    throw new AssertionError("Vẫn cần bấm xem ảnh");
                }
                if (javafx.stage.Window.getWindows().stream().filter(javafx.stage.Window::isShowing)
                        .count() != 1) {
                    throw new AssertionError("Ảnh mở cửa sổ khác");
                }
                return null;
            });
            trenGiaoDien(() -> {
                cuaSo.toFront();
                cuaSo.requestFocus();
                return null;
            });
            trenGiaoDien(() -> {
                Node nut = manHinh.lookup("#tinNhan" + idAnh);
                ContextMenu menu = (ContextMenu) nut.getUserData();
                if (menu.getItems().stream().noneMatch(m -> m.getText().equals("Tải xuống"))) {
                    throw new AssertionError("Thiếu tải trong menu");
                }
                if (cacNut(nut).stream().anyMatch(n -> n instanceof Button bt &&
                        (bt.getText().equals("Tải về") || bt.getText().equals("Tải ảnh gốc")))) {
                    throw new AssertionError("Nút tải vẫn hiện sẵn");
                }
                menu.getItems().stream().filter(m -> m.getText().equals("Sao chép")).findFirst().orElseThrow()
                        .fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> javafx.scene.input.Clipboard.getSystemClipboard().hasImage());
                } catch (Exception loi) {
                    return false;
                }
            });
            // Thả cảm xúc vẽ lại tin: ảnh vẫn tự hiện từ bộ nhớ đệm.
            a.yeuCau("datCamXuc", DuLieu.tao("tinNhanId", idAnh, "bieuCam", "❤️")).get();
            doi(() -> {
                try {
                    return trenGiaoDien(() -> cacNut(manHinh.lookup("#tinNhan" + idAnh)).stream()
                            .anyMatch(n -> n instanceof ImageView));
                } catch (Exception loi) {
                    return false;
                }
            });
            // Đổi sang phòng chung rồi mở lại: ảnh trong lịch sử tự hiển thị.
            trenGiaoDien(() -> {
                ((Button) manHinh.lookup(".nutPhongChung")).fire();
                return null;
            });
            trenGiaoDien(() -> {
                var ds = (ListView<JsonObject>) manHinh.lookup(".danhSachNguoi");
                ds.getItems().stream().filter(n -> DuLieu.chu(n, "id").equals(idA)).findFirst()
                        .ifPresent(n -> ds.getSelectionModel().select(n));
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup("#tinNhan" + idAnh) != null &&
                            cacNut(manHinh.lookup("#tinNhan" + idAnh)).stream()
                                    .anyMatch(n -> n instanceof ImageView));
                } catch (Exception loi) {
                    return false;
                }
            });
            chup(goc.resolve("anh-tu-hien-trong-chat.png"));
            // Ảnh do chính mình gửi cũng tự hiện, không cần thao tác xem.
            Path anhMinh = tam.resolve("Ảnh mình gửi.png");
            Files.copy(anh, anhMinh);
            trenGiaoDien(() -> {
                ContextMenu menu = (ContextMenu) manHinh.lookup("#tinNhan" + idAnh).getUserData();
                menu.getItems().stream().filter(m -> m.getText().equals("Trả lời")).findFirst().orElseThrow()
                        .fire();
                manHinh.chonTep(anhMinh);
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> cacNut(manHinh.lookup(".thanhTraLoi")).stream()
                            .anyMatch(n -> n instanceof ImageView anhXem && anhXem.getImage() != null) &&
                            cacNut(manHinh.lookup(".khungSoan")).stream().anyMatch(
                                    n -> n instanceof ImageView anhXem &&
                                            "Ảnh đang chọn để gửi".equals(n.getAccessibleText()) &&
                                            anhXem.getImage() != null));
                } catch (Exception loi) {
                    return false;
                }
            });
            chup(goc.resolve("anh-ban-nhap-va-trich-dan.png"));
            trenGiaoDien(() -> {
                ((Button) manHinh.lookup(".nutGui")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return timTin(anhMinh.getFileName().toString()) != null;
                } catch (Exception loi) {
                    return false;
                }
            });
            String idAnhMinh = DuLieu.chu(timTin(anhMinh.getFileName().toString()), "id");
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup("#tinNhan" + idAnhMinh) != null &&
                            cacNut(manHinh.lookup("#tinNhan" + idAnhMinh)).stream()
                                    .anyMatch(n -> n instanceof ImageView));
                } catch (Exception loi) {
                    return false;
                }
            });
            JsonObject lich = b.yeuCau("lichSu", DuLieu.tao("nguoiNhanId", idA)).get();
            JsonObject tinCoTrich = null;
            for (JsonElement dong : lich.getAsJsonArray("tinNhan")) {
                if (DuLieu.chu(dong.getAsJsonObject(), "id").equals(idAnhMinh)) {
                    tinCoTrich = dong.getAsJsonObject();
                }
            }
            if (tinCoTrich == null ||
                    !DuLieu.chu(tinCoTrich.getAsJsonObject("trichDan"), "loaiTin").equals("anh")) {
                throw new AssertionError("Server không trả loại ảnh trong trích dẫn");
            }
            doi(() -> {
                try {
                    return trenGiaoDien(() -> cacNut(manHinh.lookup("#tinNhan" + idAnhMinh)).stream()
                            .filter(n -> n.getStyleClass().contains("trichDanTin")).anyMatch(
                                    n -> cacNut(n).stream().anyMatch(con -> con instanceof ImageView anhXem &&
                                            anhXem.getImage() != null)));
                } catch (Exception loi) {
                    return false;
                }
            });
            // Tin bị thu hồi phải bỏ cả ảnh đang hiện và ảnh trong bộ nhớ đệm.
            a.yeuCau("thuHoiTin", DuLieu.tao("tinNhanId", idAnh)).get();
            doi(() -> {
                try {
                    return trenGiaoDien(() -> manHinh.lookup("#tinNhan" + idAnh) != null &&
                            coTen((Parent) manHinh.lookup("#tinNhan" + idAnh), "Tin nhắn đã được thu hồi") &&
                            cacNut(manHinh.lookup("#tinNhan" + idAnh)).stream()
                                    .noneMatch(n -> n instanceof ImageView));
                } catch (Exception loi) {
                    return false;
                }
            });
            try (KenhTCP moi = new KenhTCP(); TruyenTepTCP boTai = new TruyenTepTCP(moi)) {
                moi.ketNoi("127.0.0.1", mayChu.cong()).get();
                moi.yeuCau("dangNhap", DuLieu.tao("email", emailA, "matKhau", matKhau)).get();
                Path sau = tam.resolve("mo-lai.zip");
                boTai.tai(DuLieu.chu(tin, "id"), sau, so -> {
                }).get(20, TimeUnit.SECONDS);
                if (!Arrays.equals(cacByte, Files.readAllBytes(sau))) {
                    throw new AssertionError("Mo lai khong tai duoc");
                }
            }
            System.out.println(
                    "PASS: gui tep tu giao dien, tai khop bytes, tien trinh, huy gui/tai giu tep cu, anh tu hien cho nguoi gui/nhan va lich su, khong hien ten anh/nut xem, ve lai van co anh, thu hoi an anh, chi mot cua so, gui lai khong trung, ket noi moi tai duoc");
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
                for (String email : List.of(emailA, emailB)) {
                    xoa.setString(1, email);
                    xoa.executeUpdate();
                }
            }
            Platform.exit();
        }
    }
}
