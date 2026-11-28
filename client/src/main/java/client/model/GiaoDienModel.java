package client.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lưu giao diện theo người dùng Windows trên máy hiện tại. */
public final class GiaoDienModel {
    private final Path tep = tepTuyChon();
    private boolean toi = doc();

    private static Path tepTuyChon() {
        String thuMuc = System.getenv("LOCALAPPDATA");
        if (thuMuc == null || thuMuc.isBlank()) {
            thuMuc = System.getProperty("user.home");
        }
        return Path.of(thuMuc, "NexusJavaFX", "giao-dien.txt");
    }

    private boolean doc() {
        try {
            return Files.exists(tep) && Files.readString(tep).strip().equals("toi");
        } catch (IOException | SecurityException loi) {
            return false;
        }
    }

    public boolean toi() {
        return toi;
    }

    public boolean luu(boolean toi) {
        this.toi = toi;
        try {
            Files.createDirectories(tep.getParent());
            Files.writeString(tep, toi ? "toi" : "sang");
            return true;
        } catch (IOException | SecurityException loi) {
            return false;
        }
    }
}
