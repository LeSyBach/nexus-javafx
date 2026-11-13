package server.model;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;

public record NguoiDungModel(long id, String ten, String email, String matKhauBam) {
    public JsonObject rieng() {
        return DuLieu.tao("id", Long.toString(id), "tenHienThi", ten, "email", email);
    }

    public JsonObject congKhai(boolean dangOnline) {
        return DuLieu.tao("id", Long.toString(id), "tenHienThi", ten, "dangOnline", dangOnline);
    }
}
