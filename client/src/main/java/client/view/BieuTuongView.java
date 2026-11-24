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

    static StackPane dienThoai() {
        return tao("M22 16.92 V19.92 A2 2 0 0 1 19.82 21.92 A19.79 19.79 0 0 1 11.19 18.85 "
                + "A19.5 19.5 0 0 1 5.19 12.85 A19.79 19.79 0 0 1 2.09 4.18 "
                + "A2 2 0 0 1 4.08 2 H7.08 A2 2 0 0 1 9.08 3.72 A12.84 12.84 0 0 0 9.78 6.53 "
                + "A2 2 0 0 1 9.33 8.64 L8.06 9.91 A16 16 0 0 0 14.06 15.91 L15.33 14.64 "
                + "A2 2 0 0 1 17.44 14.19 A12.84 12.84 0 0 0 20.25 14.89 "
                + "A2 2 0 0 1 22 16.92 Z");
    }

    static StackPane cupMay() {
        return tao("M3 16 L1 12 Q12 2 23 12 L21 16 L16 14 V11 Q12 9 8 11 V14 Z");
    }

    static StackPane micro(boolean tat) {
        if (tat) {
            return tao("M9 9 V12 A3 3 0 0 0 14.12 14.12", "M15 9.34 V5 A3 3 0 0 0 9.2 3.9",
                    "M5 10 V12 A7 7 0 0 0 17 16.9", "M19 10 V12", "M12 19 V22", "M8 22 H16",
                    "M2 2 L22 22");
        }
        return tao("M9 5 A3 3 0 0 1 15 5 V12 A3 3 0 0 1 9 12 Z",
                "M5 10 V12 A7 7 0 0 0 19 12 V10", "M12 19 V22", "M8 22 H16");
    }

    static StackPane video(boolean tat) {
        return tat ? tao("M3 3 H15 V15 L3 3 M3 7 V19 H15 V19 M15 9 L22 5 V19 L19 17", "M2 2 L22 22")
                : tao("M3 5 H15 V19 H3 Z", "M15 9 L22 5 V19 L15 15");
    }
}
