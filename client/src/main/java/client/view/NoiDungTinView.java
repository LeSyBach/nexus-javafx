package client.view;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/** Hiển thị link HTTP/HTTPS trong nội dung tin và mở bằng trình duyệt mặc định. */
final class NoiDungTinView {
    private static final Pattern DIA_CHI_WEB = Pattern.compile(
            "(?<![\\p{L}\\p{N}@:/._-])(?:https?://|www\\.)[^\\s<>\"\\p{Z}\\p{Cntrl}]+",
            Pattern.CASE_INSENSITIVE);

    private NoiDungTinView() {
    }

    static Node tao(String noiDung, String lop, double rong, boolean nhanDangLienKet) {
        if (!nhanDangLienKet) {
            return chu(noiDung, lop, rong);
        }
        var tim = DIA_CHI_WEB.matcher(noiDung);
        if (!tim.find()) {
            return chu(noiDung, lop, rong);
        }
        TextFlow dongChu = new TextFlow();
        double rongLienKet = lop.startsWith("bongTin") ? rong - 28 : rong;
        Label thongBao = chu("Chưa mở được liên kết trong trình duyệt.", "loiMoLienKet", rongLienKet);
        thongBao.managedProperty().bind(thongBao.visibleProperty());
        thongBao.setVisible(false);
        int daDoc = 0;
        do {
            String diaChi = catDauCau(tim.group());
            URI uri = docDiaChi(diaChi);
            if (uri == null) {
                continue;
            }
            themChu(dongChu, noiDung.substring(daDoc, tim.start()));
            Hyperlink lienKet = new Hyperlink(diaChi);
            lienKet.getStyleClass().add("lienKetTinNhan");
            lienKet.setWrapText(true);
            lienKet.setMaxWidth(rongLienKet);
            if (noiDung.strip().equals(diaChi)) {
                lienKet.setGraphic(BieuTuongView.lienKet());
                lienKet.getStyleClass().add("lienKetRieng");
            }
            lienKet.setAccessibleText("Mở liên kết " + diaChi);
            lienKet.setTooltip(new Tooltip("Mở trong trình duyệt · " + uri.getHost()));
            lienKet.setOnAction(e -> moTrinhDuyet(uri, lienKet, thongBao));
            dongChu.getChildren().add(lienKet);
            daDoc = tim.start() + diaChi.length();
        } while (tim.find());
        if (daDoc == 0) {
            return chu(noiDung, lop, rong);
        }
        themChu(dongChu, noiDung.substring(daDoc));
        VBox khung = new VBox(4, dongChu, thongBao);
        khung.getStyleClass().addAll(lop, "tinCoLienKet");
        khung.setMaxWidth(rong);
        return khung;
    }

    private static void themChu(TextFlow khung, String noiDung) {
        if (!noiDung.isEmpty()) {
            Text chu = new Text(noiDung);
            chu.getStyleClass().add("chuTinCoLienKet");
            khung.getChildren().add(chu);
        }
    }

    private static Label chu(String noiDung, String lop, double rong) {
        Label nhan = DoHoaView.nhan(noiDung, lop);
        nhan.setWrapText(true);
        nhan.setMaxWidth(rong);
        return nhan;
    }

    private static String catDauCau(String diaChi) {
        int cuoi = diaChi.length();
        while (cuoi > 0 && ".,;:!?…'’”".indexOf(diaChi.charAt(cuoi - 1)) >= 0) {
            cuoi--;
        }
        while (cuoi > 0) {
            char dauDong = diaChi.charAt(cuoi - 1);
            int viTri = ")]}".indexOf(dauDong);
            if (viTri < 0) {
                break;
            }
            char dauMo = "([{".charAt(viTri);
            String phanConLai = diaChi.substring(0, cuoi);
            long soMo = phanConLai.chars().filter(kyTu -> kyTu == dauMo).count();
            long soDong = phanConLai.chars().filter(kyTu -> kyTu == dauDong).count();
            if (soDong <= soMo) {
                break;
            }
            cuoi--;
        }
        return diaChi.substring(0, cuoi);
    }

    private static URI docDiaChi(String chu) {
        String diaChi = chu.toLowerCase(Locale.ROOT).startsWith("www.") ? "https://" + chu : chu;
        try {
            URI uri = URI.create(diaChi);
            if (uri.getHost() == null || uri.getUserInfo() != null) {
                return null;
            }
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) ? uri : null;
        } catch (IllegalArgumentException loi) {
            return null;
        }
    }

    private static void moTrinhDuyet(URI uri, Hyperlink lienKet, Label thongBao) {
        thongBao.setVisible(false);
        CompletableFuture.runAsync(() -> {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException | RuntimeException loi) {
                Platform.runLater(() -> {
                    if (lienKet.getScene() != null) {
                        thongBao.setVisible(true);
                    }
                });
            }
        });
    }
}
