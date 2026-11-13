package giaothuc;

import com.google.gson.*;

public final class DuLieu {
    private DuLieu() {
    }

    public static JsonObject tao(Object... cacCap) {
        JsonObject ketQua = new JsonObject();
        for (int viTri = 0; viTri < cacCap.length; viTri += 2) {
            Object giaTri = cacCap[viTri + 1];
            String ten = (String) cacCap[viTri];
            if (giaTri == null) {
                ketQua.add(ten, JsonNull.INSTANCE);
            } else if (giaTri instanceof JsonElement phanTu) {
                ketQua.add(ten, phanTu);
            } else if (giaTri instanceof Boolean dungSai) {
                ketQua.addProperty(ten, dungSai);
            } else if (giaTri instanceof Number so) {
                ketQua.addProperty(ten, so);
            } else {
                ketQua.addProperty(ten, giaTri.toString());
            }
        }
        return ketQua;
    }

    public static String chu(JsonObject duLieu, String ten) {
        JsonElement giaTri = duLieu.get(ten);
        if (giaTri == null || giaTri.isJsonNull()) {
            return "";
        }
        if (!giaTri.isJsonPrimitive() || !giaTri.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Trường " + ten + " phải là chuỗi.");
        }
        return giaTri.getAsString();
    }

    public static Long id(JsonObject duLieu, String ten, boolean choPhepRong) {
        String chuoi = chu(duLieu, ten);
        if (chuoi.isEmpty() && choPhepRong) {
            return null;
        }
        if (!chuoi.matches("[1-9][0-9]{0,17}")) {
            throw new IllegalArgumentException("Mã người dùng/tin nhắn chưa hợp lệ.");
        }
        return Long.valueOf(chuoi);
    }
}
