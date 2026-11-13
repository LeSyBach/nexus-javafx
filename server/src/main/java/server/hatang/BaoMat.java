package server.hatang;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;

public final class BaoMat {
    private static final int soLanLap = 210000;
    private static final SecureRandom ngauNhien = new SecureRandom();

    private BaoMat() {
    }

    public static String bam(String matKhau) {
        byte[] muoi = new byte[16];
        ngauNhien.nextBytes(muoi);
        return "pbkdf2$" + soLanLap + "$" + Base64.getEncoder().encodeToString(muoi) + "$"
                + Base64.getEncoder().encodeToString(tinh(matKhau, muoi, soLanLap));
    }

    public static boolean doiChieu(String matKhau, String bamDaLuu) {
        if (bamDaLuu == null) {
            tinh(matKhau, new byte[16], soLanLap);
            return false;
        }
        try {
            String[] cacPhan = bamDaLuu.split("\\$");
            if (cacPhan.length != 4 || !cacPhan[0].equals("pbkdf2") ||
                    Integer.parseInt(cacPhan[1]) != soLanLap) {
                return false;
            }
            return MessageDigest.isEqual(Base64.getDecoder().decode(cacPhan[3]),
                    tinh(matKhau, Base64.getDecoder().decode(cacPhan[2]), soLanLap));
        } catch (IllegalArgumentException loi) {
            return false;
        }
    }

    private static byte[] tinh(String matKhau, byte[] muoi, int soLan) {
        PBEKeySpec cauHinh = new PBEKeySpec(matKhau.toCharArray(), muoi, soLan, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(cauHinh).getEncoded();
        } catch (GeneralSecurityException loi) {
            throw new IllegalStateException("Chưa khởi tạo được bộ băm mật khẩu.", loi);
        } finally {
            cauHinh.clearPassword();
        }
    }
}
