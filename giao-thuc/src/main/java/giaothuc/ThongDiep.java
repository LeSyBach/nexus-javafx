package giaothuc;

import com.google.gson.JsonObject;

import java.util.UUID;

public record ThongDiep(String loai, String ma, JsonObject duLieu) {
    public static ThongDiep yeuCau(String loai, JsonObject duLieu) {
        return new ThongDiep(loai, UUID.randomUUID().toString(), duLieu);
    }

    public static ThongDiep suKien(String loai, JsonObject duLieu) {
        return new ThongDiep(loai, "", duLieu);
    }
}
