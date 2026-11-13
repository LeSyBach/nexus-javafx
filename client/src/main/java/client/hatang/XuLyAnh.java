package client.hatang;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;

/**
 * Cắt giữa ảnh thành hình vuông 256px trước khi gửi qua TCP.
 */
public final class XuLyAnh {
    private XuLyAnh() {
    }

    public static java.util.concurrent.CompletableFuture<String> docTrongNen(java.nio.file.Path tep) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return doc(tep);
            } catch (java.io.IOException sai) {
                throw new java.util.concurrent.CompletionException(sai);
            }
        });
    }

    public static String doc(Path tep) throws IOException {
        if (Files.size(tep) > 10 * 1024 * 1024) {
            throw new IOException("Chọn ảnh PNG/JPEG tối đa 10 MB.");
        }
        BufferedImage anh;
        ImageReader boDoc = null;
        try (ImageInputStream vao = ImageIO.createImageInputStream(tep.toFile())) {
            var cacBoDoc = ImageIO.getImageReaders(vao);
            if (!cacBoDoc.hasNext()) {
                throw new IOException("Không đọc được ảnh này.");
            }
            boDoc = cacBoDoc.next();
            boDoc.setInput(vao);
            String dinhDang = boDoc.getFormatName();
            int rong = boDoc.getWidth(0), cao = boDoc.getHeight(0);
            if (!(dinhDang.equalsIgnoreCase("PNG") || dinhDang.equalsIgnoreCase("JPEG")) || rong < 1 ||
                    cao < 1 || rong > 12000 || cao > 12000) {
                throw new IOException("Chọn ảnh PNG/JPEG có kích thước tối đa 12.000px.");
            }
            ImageReadParam cauHinh = boDoc.getDefaultReadParam();
            int buoc = Math.max(1, Math.max(rong, cao) / 1024);
            cauHinh.setSourceSubsampling(buoc, buoc, 0, 0);
            anh = boDoc.read(0, cauHinh);
        } finally {
            if (boDoc != null) {
                boDoc.dispose();
            }
        }
        BufferedImage vuong = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D ve = vuong.createGraphics();
        try {
            ve.setColor(Color.WHITE);
            ve.fillRect(0, 0, 256, 256);
            ve.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int canh = Math.min(anh.getWidth(), anh.getHeight()), x = (anh.getWidth() - canh) / 2, y =
                    (anh.getHeight() - canh) / 2;
            ve.drawImage(anh, 0, 0, 256, 256, x, y, x + canh, y + canh, null);
        } finally {
            ve.dispose();
        }
        for (float chatLuong : new float[]{0.85f, 0.65f, 0.45f}) {
            ByteArrayOutputStream cacByte = new ByteArrayOutputStream();
            ImageWriter boGhi = ImageIO.getImageWritersByFormatName("JPEG").next();
            try (ImageOutputStream ra = ImageIO.createImageOutputStream(cacByte)) {
                boGhi.setOutput(ra);
                ImageWriteParam cauHinh = boGhi.getDefaultWriteParam();
                cauHinh.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                cauHinh.setCompressionQuality(chatLuong);
                boGhi.write(null, new IIOImage(vuong, null, null), cauHinh);
            } finally {
                boGhi.dispose();
            }
            if (cacByte.size() <= 32768) {
                return Base64.getEncoder().encodeToString(cacByte.toByteArray());
            }
        }
        throw new IOException("Chưa nén được ảnh. Hãy chọn ảnh khác.");
    }
}
