package server.hatang;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public record CauHinh(String diaChi, int cong, String urlCSDL, String taiKhoanCSDL, String matKhauCSDL) {
    public static CauHinh doc(Path duongDan) throws IOException {
        Properties cacGiaTri = new Properties();
        try (Reader boDoc = Files.newBufferedReader(duongDan)) {
            cacGiaTri.load(boDoc);
        }
        int cong = Integer.parseInt(cacGiaTri.getProperty("may_chu.cong", "4040"));
        if (cong < 1 || cong > 65535) {
            throw new IOException("Cổng máy chủ chưa hợp lệ.");
        }
        String url = cacGiaTri.getProperty("csdl.url", "");
        if (!url.startsWith("jdbc:mariadb:")) {
            throw new IOException("Chưa cấu hình CSDL MariaDB.");
        }
        return new CauHinh(cacGiaTri.getProperty("may_chu.dia_chi", "127.0.0.1"), cong, url,
                cacGiaTri.getProperty("csdl.tai_khoan", ""), cacGiaTri.getProperty("csdl.mat_khau", ""));
    }
}
