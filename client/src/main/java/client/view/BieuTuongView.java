package client.view;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.*;

/**
 * Icon nét vector, không phụ thuộc font ký hiệu của máy.
 */
final class BieuTuongView {
    private BieuTuongView() {
    }

    private static StackPane tao(String... cacNet) {
        Group nhom = new Group();
        for (String net : cacNet) {
            SVGPath hinh = new SVGPath();
            hinh.setContent(net);
            hinh.getStyleClass().add("netBieuTuong");
            hinh.setStrokeLineCap(StrokeLineCap.ROUND);
            hinh.setStrokeLineJoin(StrokeLineJoin.ROUND);
            nhom.getChildren().add(hinh);
        }
        nhom.setScaleX(0.83);
        nhom.setScaleY(0.83);
        nhom.setMouseTransparent(true);
        StackPane khung = new StackPane(nhom);
        khung.setMinSize(22, 22);
        khung.setPrefSize(22, 22);
        khung.setMaxSize(22, 22);
        khung.setMouseTransparent(true);
        return khung;
    }

    static StackPane kepGiay() {
        return tao(
                "M21.44 11.05 L12.25 20.24 A6 6 0 0 1 3.76 11.75 L12.95 2.56 A4 4 0 0 1 18.61 8.22 L9.41 17.41 A2 2 0 0 1 6.58 14.58 L15.07 6.09");
    }

    static StackPane anh() {
        return tao("M4 3 H20 A1 1 0 0 1 21 4 V20 A1 1 0 0 1 20 21 H4 A1 1 0 0 1 3 20 V4 A1 1 0 0 1 4 3 Z",
                "M3 16 L9 10 L14 15 L17 12 L21 16", "M8 7 A1 1 0 1 0 8 9 A1 1 0 1 0 8 7");
    }

    static StackPane matCuoi() {
        return tao("M22 12 A10 10 0 1 1 2 12 A10 10 0 0 1 22 12", "M8 14 Q12 18 16 14", "M8 9 H8.01",
                "M16 9 H16.01");
    }

    static StackPane mayAnh() {
        return tao("M3 7 H7 L9 4 H15 L17 7 H21 V20 H3 Z", "M16 13 A4 4 0 1 1 8 13 A4 4 0 1 1 16 13");
    }

    static StackPane luuTru() {
        return tao("M3 4 H21 V9 H3 Z", "M5 9 V21 H19 V9", "M10 13 H14");
    }

    static StackPane lienKet() {
        return tao("M10 13 L14 9", "M9 16 L7 18 A4 4 0 0 1 1 12 L6 7 A4 4 0 0 1 12 7",
                "M15 8 L17 6 A4 4 0 0 1 23 12 L18 17 A4 4 0 0 1 12 17");
    }

    static StackPane ghim() {
        return tao("M8 3 H16 L15 9 L19 13 V15 H5 V13 L9 9 Z", "M12 15 V22");
    }

    static StackPane quayLai() {
        return tao("M19 12 H5", "M12 5 L5 12 L12 19");
    }

    static StackPane xuongCuoi() {
        return tao("M12 4 V20", "M5 13 L12 20 L19 13");
    }

    static StackPane gui() {
        return tao("M22 2 L15 22 L11 13 L2 9 Z", "M22 2 L11 13");
    }

    static StackPane traLoi() {
        return tao("M9 10 L4 5 L9 0", "M4 5 H14 A6 6 0 0 1 20 11 V19");
    }

    static StackPane thuHoi() {
        return tao("M3 10 A9 9 0 1 1 5 19", "M3 3 V10 H10", "M12 7 V12 L15 14");
    }

    static StackPane tinNhan() {
        return tao("M21 11 A8 8 0 0 1 13 19 H8 L3 22 V16 A8 8 0 0 1 3 6 A8 8 0 0 1 21 11 Z", "M8 10 H16",
                "M8 14 H13");
    }

    static StackPane banBe() {
        return tao("M9 3 A4 4 0 1 0 9 11 A4 4 0 1 0 9 3",
                "M2 21 V18 A5 5 0 0 1 7 13 H11 A5 5 0 0 1 16 18 V21", "M17 3 A4 4 0 0 1 17 11",
                "M19 14 A5 5 0 0 1 22 19 V21");
    }

    static StackPane dangXuat() {
        return tao("M9 3 H4 V21 H9", "M9 12 H22", "M17 7 L22 12 L17 17");
    }
}
