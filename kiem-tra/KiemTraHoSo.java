import server.dao.NguoiDungDao;
import server.dao.TinNhanDao;
import server.hatang.BaoMat;
import server.hatang.CauHinh;
import client.hatang.KenhTCP;
import server.dao.BanBeDao;
import server.dao.CSDLDao;
import server.model.HoSoModel;
import client.model.TaiKhoanModel;
import server.hatang.MayChuChat;
import client.hatang.UngDungNexus;
import client.hatang.XuLyAnh;

import server.hatang.*;
import server.model.*;
import server.controller.*;

import client.hatang.*;
import client.view.*;
import client.model.*;
import client.controller.*;

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
 * Kiem tra app that, dung kho phien tam rieng va tai khoan duoc don sau cung.
 */

public class KiemTraHoSo {

    static UngDungNexus app;

    static Stage cuaSo;

    static TaiKhoanModel khoPhien;

    static final List<String> email = new ArrayList<>();

    static final List<Long> id = new ArrayList<>();

    static final String matKhau = "KiemTraHoSo2026!";

    static <T> T trenGiaoDien(Callable<T> viec) throws Exception {

        FutureTask<T> cho = new FutureTask<>(() -> {
            T ketQua = viec.call();
            if (Window.getWindows().stream().filter(Window::isShowing).count() > 1) {
                throw new AssertionError("App mo them cua so");
            }
            return ketQua;
        });
        Platform.runLater(cho);
        return cho.get(45, TimeUnit.SECONDS);

    }

    static void doi(BooleanSupplier dieuKien) throws Exception {

        long het = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);

        while (System.nanoTime() < het) {
            if (dieuKien.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }

        throw new AssertionError("Qua thoi gian cho giao dien");

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

    static Parent cuaHop(String tieuDe) {

        return Window.getWindows().stream().filter(w -> w instanceof Stage st && st.getTitle().equals(tieuDe))
                .findFirst().orElseThrow().getScene().getRoot();

    }

    static Parent hoSo() {
        return (Parent) cuaSo.getScene().lookup("#manHinhHoSo");
    }

    static Parent taiKhoan() {
        return (Parent) cuaSo.getScene().lookup("#manHinhTaiKhoan");
    }

    static void bam(Parent goc, String chu) {

        cacNut(goc).stream().filter(n -> n instanceof ButtonBase nut && nut.getText().equals(chu))
                .map(n -> (ButtonBase) n).findFirst()
                .orElseThrow(() -> new AssertionError("Khong thay nut " + chu)).fire();

    }

    static boolean coChu(Parent goc, String chu) {

        return cacNut(goc).stream().anyMatch(n -> n instanceof Label nhan && nhan.getText().contains(chu));

    }

    static void choChuHoSo(String chu) throws Exception {

        doi(() -> {
            try {
                return trenGiaoDien(() -> coChu(hoSo(), chu));
            } catch (Exception loi) {
                throw new RuntimeException(loi);
            }
        });

    }

    static void chuyenSang(String email) throws Exception {

        Parent cu = trenGiaoDien(() -> cuaSo.getScene().getRoot());

        trenGiaoDien(() -> {
            bam(dongTaiKhoan(email), "Chuyển");
            return null;
        });

        doi(() -> {
            try {
                return trenGiaoDien(() -> cuaSo.getScene().getRoot() != cu &&
                        cuaSo.getScene().lookup(".nutAnhHoSo") != null);
            } catch (Exception loi) {
                return false;
            }
        });

        moHoSo();
        choChuHoSo(email);

    }

    static void choChat() throws Exception {

        doi(() -> {
            try {
                return trenGiaoDien(() -> cuaSo.getScene().lookup(".nutAnhHoSo") != null);
            } catch (Exception loi) {
                throw new RuntimeException(loi);
            }
        });

    }

    static void moHoSo() throws Exception {
        trenGiaoDien(() -> {
            int soCuaSo = Window.getWindows().size();
            ((Button) cuaSo.getScene().lookup(".nutAnhHoSo")).fire();
            if (Window.getWindows().size() != soCuaSo) {
                throw new AssertionError("Ho so mo them cua so");
            }
            return null;
        });
    }

    static void nhapHoSo(String ten, String sdt, String gt) throws Exception {

        trenGiaoDien(() -> {

            ((TextField) hoSo().lookup("#tenHoSo")).setText(ten);

            ((TextField) hoSo().lookup("#dienThoaiHoSo")).setText(sdt);

            ((TextArea) hoSo().lookup("#gioiThieuHoSo")).setText(gt);
            return null;

        });

    }

    static void dangNhap(int so, int cong) throws Exception {

        doi(() -> {
            try {
                return trenGiaoDien(() -> cuaSo.getScene().lookup(".trangDangNhap") != null &&
                        !cuaSo.getScene().getRoot().isDisabled());
            } catch (Exception loi) {
                throw new RuntimeException(loi);
            }
        });

        trenGiaoDien(() -> {

            Parent goc = cuaSo.getScene().getRoot();

            for (Node nut : cacNut(goc)) {
                if (nut instanceof TextField truong) {

                    if (truong.getPromptText().equals("ban@example.com")) {
                        truong.setText(email.get(so));
                    }

                    if (truong.getPromptText().equals("Nhập mật khẩu")) {
                        truong.setText(matKhau);
                    }

                    if (truong.getText().equals("4040")) {
                        truong.setText(Integer.toString(cong));
                    }

                }
            }

            bam(goc, "Đăng nhập   →");
            return null;

        });
        choChat();

    }

    static void chup(Parent goc, String tep) throws Exception {

        trenGiaoDien(() -> {

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

            ImageIO.write(png, "png", Path.of(tep).toFile());
            return null;

        });

    }

    static Parent dongTaiKhoan(String email) {
        taiKhoan().applyCss();
        taiKhoan().layout();

        return cacNut(taiKhoan()).stream()
                .filter(n -> n instanceof HBox && n.getStyleClass().contains("dongBanBe") &&
                        coChu((Parent) n, email)).map(n -> (Parent) n).findFirst().orElseThrow();

    }

    static void kiemTraBanBeVaBieuCam(CSDLDao kho, MayChuChat server, String anh) throws Exception {
        trenGiaoDien(() -> {
            bam(cuaSo.getScene().getRoot(), "Bạn bè");
            return null;
        });
        trenGiaoDien(() -> {
            ((TextField) cuaSo.getScene().lookup("#emailBanBe")).setText(email.get(1));
            bam(cuaSo.getScene().getRoot(), "Tìm tài khoản");
            return null;
        });
        doi(() -> {
            try {
                return trenGiaoDien(() -> coChu(cuaSo.getScene().getRoot(), "Đã tìm thấy tài khoản"));
            } catch (Exception loi) {
                return false;
            }
        });
        trenGiaoDien(() -> {
            bam(cuaSo.getScene().getRoot(), "Kết bạn");
            return null;
        });
        doi(() -> {
            try {
                return trenGiaoDien(() -> coChu(cuaSo.getScene().getRoot(), "Đã gửi lời mời"));
            } catch (Exception loi) {
                return false;
            }
        });
        try (KenhTCP benB = new KenhTCP()) {
            benB.ketNoi("127.0.0.1", server.cong()).get();
            benB.yeuCau("dangNhap", DuLieu.tao("email", email.get(1), "matKhau", matKhau)).get();
            benB.yeuCau("thaoTacBanBe",
                    DuLieu.tao("doiPhuongId", Long.toString(id.get(0)), "thaoTac", "chapNhan")).get();
            doi(() -> {
                try {
                    return trenGiaoDien(() -> coChu(cuaSo.getScene().getRoot(), "Đã kết bạn ·"));
                } catch (Exception loi) {
                    return false;
                }
            });
            trenGiaoDien(() -> {
                bam(cuaSo.getScene().getRoot(), "Hủy kết bạn");
                return null;
            });
            chup(trenGiaoDien(() -> cuaSo.getScene().getRoot()), anh + "-ban-be.png");
            trenGiaoDien(() -> {
                ((Button) cuaSo.getScene().lookup(".nutHuyThuHoi")).fire();
                return null;
            });
            if (!new BanBeDao(kho).quanHe(id.get(0), id.get(1)).equals("banBe")) {
                throw new AssertionError("Huy nham quan he");
            }
            trenGiaoDien(() -> {
                bam(cuaSo.getScene().getRoot(), "Hủy kết bạn");
                ((Button) cuaSo.getScene().lookup(".nutXacNhanThuHoi")).fire();
                return null;
            });
            doi(() -> {
                try {
                    return trenGiaoDien(() -> coChu(cuaSo.getScene().getRoot(), "Đã hủy kết bạn"));
                } catch (Exception loi) {
                    return false;
                }
            });
        }
        trenGiaoDien(() -> {
            bam(cuaSo.getScene().getRoot(), "← Quay về trò chuyện");
            ((ToggleButton) cuaSo.getScene().lookup(".nutBieuCam")).fire();
            bam(cuaSo.getScene().getRoot(), "😊");
            TextArea soan = (TextArea) cuaSo.getScene().lookup(".text-area");
            if (!soan.getText().contains("😊")) {
                throw new AssertionError("Chua chen bieu cam");
            }
            soan.clear();
            return null;
        });
    }

    public static void main(String[] args) throws Exception {

        CSDLDao kho =
                new CSDLDao(CauHinh.doc(Path.of("D:/nexus-javafx/cau-hinh/may-chu.properties")));

        HoSoModel hoSoDB = new HoSoModel(kho);
        MayChuChat server = new MayChuChat(kho, "127.0.0.1", 0);

        Thread luong = new Thread(() -> {
            try {
                server.phucVu();
            } catch (Exception loi) {
            }
        });
        luong.start();

        Path thuMuc = Files.createTempDirectory("nexus-qa-ho-so-");
        khoPhien = new TaiKhoanModel(thuMuc.resolve("tai-khoan.dat"));

        CountDownLatch sanSang = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            sanSang.countDown();
        });
        sanSang.await();

        try {

            String bam = BaoMat.bam(matKhau);

            for (int i = 0; i < 2; i++) {
                String e = "qa-ho-so-" + UUID.randomUUID() + "@example.invalid";
                email.add(e);
                id.add(new NguoiDungDao(kho).themNguoi(i == 0 ? "An QA" : "Bình QA", e, bam).id());
            }

            BufferedImage anh = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
            var ve = anh.createGraphics();

            ve.setColor(new java.awt.Color(40, 108, 82));
            ve.fillRect(0, 0, 600, 400);
            ve.setColor(new java.awt.Color(239, 205, 130));
            ve.fillOval(190, 90, 220, 220);
            ve.dispose();

            Path anhMau = thuMuc.resolve("anh-mau.png");
            ImageIO.write(anh, "png", anhMau.toFile());

            JsonObject duLieu = hoSoDB.doc(id.get(0));
            duLieu.addProperty("phienBan", DuLieu.chu(duLieu, "phienBan"));
            duLieu.addProperty("anhDaiDien", XuLyAnh.doc(anhMau));
            hoSoDB.capNhat(id.get(0), duLieu);

            trenGiaoDien(() -> {
                cuaSo = new Stage();
                app = new UngDungNexus(khoPhien);
                app.start(cuaSo);
                return null;
            });

            dangNhap(0, server.cong());
            kiemTraBanBeVaBieuCam(kho, server, args[0]);
            trenGiaoDien(() -> {
                cuaSo.requestFocus();
                return null;
            });
            doi(() -> {
                try {
                    return new TinNhanDao(kho).hoiThoai(id.get(0), 0).asList().stream()
                            .filter(e -> DuLieu.chu(e.getAsJsonObject(), "id").equals("0"))
                            .allMatch(e -> e.getAsJsonObject().get("chuaDoc").getAsInt() == 0);
                } catch (Exception loi) {
                    return false;
                }
            });
            moHoSo();
            choChuHoSo(email.get(0));
            try (KenhTCP benB = new KenhTCP()) {
                benB.ketNoi("127.0.0.1", server.cong()).get();
                benB.yeuCau("dangNhap", DuLieu.tao("email", email.get(1), "matKhau", matKhau)).get();
                benB.yeuCau("guiTin", DuLieu.tao("noiDung", "Tin tới khi đang xem hồ sơ", "maGui",
                        UUID.randomUUID().toString())).get();
                doi(() -> {
                    try {
                        return trenGiaoDien(() -> cacNut(cuaSo.getScene().getRoot()).stream()
                                .anyMatch(n -> n instanceof Button b && b.getText().contains("1 chưa đọc")));
                    } catch (Exception loi) {
                        return false;
                    }
                });
                if (new TinNhanDao(kho).hoiThoai(id.get(0), 0).asList().stream()
                        .filter(e -> DuLieu.chu(e.getAsJsonObject(), "id").equals("0"))
                        .noneMatch(e -> e.getAsJsonObject().get("chuaDoc").getAsInt() == 1)) {
                    throw new AssertionError("Ho so danh dau nham tin chat");
                }
                trenGiaoDien(() -> {
                    bam(hoSo(), "← Quay về trò chuyện");
                    return null;
                });
                doi(() -> {
                    try {
                        return new TinNhanDao(kho).hoiThoai(id.get(0), 0).asList().stream()
                                .filter(e -> DuLieu.chu(e.getAsJsonObject(), "id").equals("0"))
                                .allMatch(e -> e.getAsJsonObject().get("chuaDoc").getAsInt() == 0);
                    } catch (Exception loi) {
                        return false;
                    }
                });
                moHoSo();
                choChuHoSo(email.get(0));
            }

            if (trenGiaoDien(() -> hoSo().lookup("#tenHoSo") != null)) {
                throw new AssertionError("Bieu mau sua hien ngay khi xem ho so");
            }

            trenGiaoDien(() -> {
                bam(hoSo(), "Chỉnh sửa hồ sơ");
                return null;
            });

            nhapHoSo("Tên không lưu", "0912345678", "Bản nháp");
            trenGiaoDien(() -> {
                bam(hoSo(), "Hủy");
                return null;
            });

            if (!DuLieu.chu(hoSoDB.doc(id.get(0)), "tenHienThi").equals("An QA")) {
                throw new AssertionError("Huy van ghi CSDL");
            }

            trenGiaoDien(() -> {
                bam(hoSo(), "Chỉnh sửa hồ sơ");
                return null;
            });
            nhapHoSo("An Nguyễn", "0912345678",
                    "Yêu những cuộc trò chuyện nhỏ.\nĐang học lập trình mạng với Java.");

            trenGiaoDien(() -> {
                bam(hoSo(), "Lưu thay đổi");
                return null;
            });
            choChuHoSo("Đã lưu hồ sơ");

            if (!DuLieu.chu(hoSoDB.doc(id.get(0)), "tenHienThi").equals("An Nguyễn")) {
                throw new AssertionError("Chua luu ten");
            }

            chup(trenGiaoDien(() -> cuaSo.getScene().getRoot()), args[0] + "-xem.png");

            trenGiaoDien(() -> {
                bam(hoSo(), "Chỉnh sửa hồ sơ");
                return null;
            });
            chup(trenGiaoDien(() -> cuaSo.getScene().getRoot()), args[0] + "-sua.png");

            trenGiaoDien(() -> {
                bam(hoSo(), "Hủy");
                bam(hoSo(), "Đổi mật khẩu");

                ((PasswordField) hoSo().lookup("#matKhauCu")).setText("KhongDung!");
                ((PasswordField) hoSo().lookup("#matKhauMoi")).setText("MatKhauDaDoi2026!");
                ((PasswordField) hoSo().lookup("#xacNhanMatKhau")).setText("MatKhauDaDoi2026!");
                bam(hoSo(), "Lưu mật khẩu");
                return null;
            });

            choChuHoSo("Mật khẩu hiện tại chưa đúng");

            trenGiaoDien(() -> {
                ((PasswordField) hoSo().lookup("#matKhauCu")).setText(matKhau);
                ((PasswordField) hoSo().lookup("#matKhauMoi")).setText("MatKhauDaDoi2026!");
                ((PasswordField) hoSo().lookup("#xacNhanMatKhau")).setText("MatKhauDaDoi2026!");
                bam(hoSo(), "Lưu mật khẩu");
                return null;
            });

            choChuHoSo("Đã đổi mật khẩu");

            trenGiaoDien(() -> {
                bam(hoSo(), "Chuyển / thêm tài khoản");
                bam(taiKhoan(), "Thêm tài khoản");
                return null;
            });

            dangNhap(1, server.cong());
            moHoSo();
            choChuHoSo(email.get(1));

            trenGiaoDien(() -> {
                bam(hoSo(), "Chuyển / thêm tài khoản");
                return null;
            });

            chup(trenGiaoDien(() -> taiKhoan()), args[0] + "-tai-khoan.png");

            chuyenSang(email.get(0));

            // Đóng và mở app: phiên A phải còn, không cần mật khẩu đã đổi.

            trenGiaoDien(() -> {
                app.stop();
                cuaSo.close();
                return null;
            });

            if (khoPhien.doc().size() != 2) {
                throw new AssertionError("Thieu tai khoan da luu");
            }

            trenGiaoDien(() -> {
                cuaSo = new Stage();
                app = new UngDungNexus(khoPhien);
                app.start(cuaSo);
                return null;
            });

            choChat();
            moHoSo();
            choChuHoSo(email.get(0));

            trenGiaoDien(() -> {
                bam(hoSo(), "Chuyển / thêm tài khoản");
                return null;
            });
            chuyenSang(email.get(1));

            trenGiaoDien(() -> {
                bam(hoSo(), "Chuyển / thêm tài khoản");
                bam(dongTaiKhoan(email.get(0)), "Đăng xuất");
                ((Button) cuaSo.getScene().lookup(".nutXacNhanThuHoi")).fire();
                return null;
            });

            doi(() -> {
                try {
                    return trenGiaoDien(() -> coChu(taiKhoan(), email.get(1)) && cacNut(taiKhoan()).stream()
                            .noneMatch(n -> n instanceof Label l && l.getText().equals(email.get(0))));
                } catch (Exception loi) {
                    return false;
                }
            });

            trenGiaoDien(() -> {
                bam(taiKhoan(), "← Quay lại");
                return null;
            });
            moHoSo();
            choChuHoSo(email.get(1));

            trenGiaoDien(() -> {
                bam(hoSo(), "← Quay về trò chuyện");
                return null;
            });

            trenGiaoDien(() -> {
                cacNut(cuaSo.getScene().getRoot()).stream()
                        .filter(n -> n instanceof Button b && b.getTooltip() != null &&
                                b.getTooltip().getText().equals("Đăng xuất")).map(n -> (Button) n).findFirst()
                        .orElseThrow().fire();
                ((Button) cuaSo.getScene().lookup(".nutXacNhanThuHoi")).fire();
                return null;
            });

            doi(() -> {
                try {
                    return trenGiaoDien(() -> cuaSo.getScene().lookup(".trangDangNhap") != null);
                } catch (Exception loi) {
                    return false;
                }
            });

            trenGiaoDien(() -> {
                app.stop();
                cuaSo.close();
                return null;
            });

            if (!khoPhien.doc().isEmpty()) {
                throw new AssertionError("Dang xuat van luu phien");
            }

            System.out.println(
                    "PASS: mot cua so cho ban be/xac nhan huy/ho so/sua/mat khau/tai khoan/bieu cam; tin chua doc va khoi phuc phien dung");

        } finally {

            trenGiaoDien(() -> {
                if (app != null) {
                    app.stop();
                }
                if (cuaSo != null) {
                    cuaSo.close();
                }
                return null;
            });
            server.close();
            luong.join(3000);

            try (Connection db = kho.moKetNoi();
                 PreparedStatement xoa = db.prepareStatement("DELETE FROM nguoi_dung WHERE email=?")) {
                for (String e : email) {
                    xoa.setString(1, e);
                    xoa.executeUpdate();
                }
            }

            Platform.exit();

        }

    }

}
