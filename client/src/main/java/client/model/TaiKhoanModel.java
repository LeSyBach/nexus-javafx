package client.model;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Mã phiên được bảo vệ bằng Windows DPAPI CurrentUser, không lưu mật khẩu.
 */
public final class TaiKhoanModel {
    public record TaiKhoan(String mayChu, int cong, String id, String ten, String email, String maPhien) {
        public String khoa() {
            return mayChu.toLowerCase(Locale.ROOT) + ":" + cong + "/" + id;
        }

        @Override
        public String toString() {
            return ten + " (" + email + ")";
        }
    }

    private final Path tep;
    private static final Gson JSON = new Gson();

    public TaiKhoanModel(Path tep) {
        this.tep = tep;
    }

    public static TaiKhoanModel macDinh() {
        return new TaiKhoanModel(Path.of(System.getenv("LOCALAPPDATA"), "NexusJavaFX", "tai-khoan.dat"));
    }

    public synchronized List<TaiKhoan> doc() throws IOException {
        if (!Files.exists(tep)) {
            return new ArrayList<>();
        }
        if (Files.size(tep) > 1024 * 1024) {
            throw new IOException("Tệp phiên đăng nhập vượt giới hạn.");
        }
        try {
            String chu = baoVe(Files.readString(tep, StandardCharsets.US_ASCII), false);
            TaiKhoan[] cacTaiKhoan = JSON.fromJson(chu, TaiKhoan[].class);
            List<TaiKhoan> ketQua = new ArrayList<>();
            if (cacTaiKhoan != null) {
                for (TaiKhoan taiKhoan : cacTaiKhoan) {
                    if (taiKhoan == null || taiKhoan.mayChu() == null || taiKhoan.mayChu().isBlank() ||
                            taiKhoan.cong() < 1 || taiKhoan.cong() > 65535 || taiKhoan.id() == null ||
                            !taiKhoan.id().matches("[1-9][0-9]{0,17}") || taiKhoan.maPhien() == null ||
                            !taiKhoan.maPhien().matches("[A-Za-z0-9_-]{43}")) {
                        throw new IOException("Tệp phiên đăng nhập không hợp lệ.");
                    }
                    ketQua.add(taiKhoan);
                }
            }
            return ketQua;
        } catch (JsonParseException loi) {
            throw new IOException("Không đọc được danh sách tài khoản đã lưu.");
        }
    }

    public synchronized void ghi(List<TaiKhoan> cacTaiKhoan) throws IOException {
        Files.createDirectories(tep.toAbsolutePath().getParent());
        String chu = baoVe(JSON.toJson(cacTaiKhoan), true);
        Path tam = Files.createTempFile(tep.toAbsolutePath().getParent(), "phien-", ".tmp");
        try {
            Files.writeString(tam, chu, StandardCharsets.US_ASCII);
            try {
                Files.move(tam, tep, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException loi) {
                Files.move(tam, tep, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tam);
        }
    }

    private String baoVe(String chu, boolean maHoa) throws IOException {
        String xuLy = maHoa ?
                "$ra=[Security.Cryptography.ProtectedData]::Protect([Text.Encoding]::UTF8.GetBytes($vao),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);[Console]::Out.Write([Convert]::ToBase64String($ra))" :
                "$ra=[Security.Cryptography.ProtectedData]::Unprotect([Convert]::FromBase64String($vao),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser);[Console]::Out.Write([Convert]::ToBase64String($ra))";
        String lenh =
                "$ErrorActionPreference='Stop';[Console]::InputEncoding=[Text.Encoding]::UTF8;Add-Type -AssemblyName System.Security;$vao=[Console]::In.ReadToEnd();" +
                        xuLy;
        Path powershell = Path.of(System.getenv("SystemRoot"), "System32", "WindowsPowerShell", "v1.0",
                "powershell.exe");
        Process tienTrinh =
                new ProcessBuilder(powershell.toString(), "-NoProfile", "-NonInteractive", "-WindowStyle",
                        "Hidden", "-Command", lenh).start();
        try {
            try (OutputStream vao = tienTrinh.getOutputStream()) {
                vao.write(chu.getBytes(StandardCharsets.UTF_8));
            }
            // Dữ liệu luôn nhỏ; đọc đầu ra song song để không làm đầy pipe.
            var cho = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return tienTrinh.getInputStream().readAllBytes();
                } catch (IOException loi) {
                    throw new java.util.concurrent.CompletionException(loi);
                }
            });
            if (!tienTrinh.waitFor(15, TimeUnit.SECONDS)) {
                tienTrinh.destroyForcibly();
                throw new IOException("Chưa mở được kho phiên Windows.");
            }
            if (tienTrinh.exitValue() != 0) {
                throw new IOException("Không bảo vệ/đọc được phiên bằng tài khoản Windows hiện tại.");
            }
            String ketQua = new String(cho.get(3, TimeUnit.SECONDS), StandardCharsets.US_ASCII).strip();
            return maHoa ? ketQua : new String(Base64.getDecoder().decode(ketQua), StandardCharsets.UTF_8);
        } catch (InterruptedException loi) {
            Thread.currentThread().interrupt();
            throw new IOException("Đã dừng lưu phiên.");
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException |
                 IllegalArgumentException loi) {
            throw new IOException("Không đọc được kho phiên Windows.");
        } finally {
            tienTrinh.destroy();
        }
    }
}
