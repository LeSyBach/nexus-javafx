package client.model;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.util.*;

/** Danh sách quan hệ và kết quả tra cứu bạn bè của màn hình. */
public final class BanBeModel {
    private final Map<String, JsonObject> cacQuanHe = new LinkedHashMap<>();
    private JsonObject nguoiTimDuoc;

    public void capNhatDanhSach(Map<String, JsonObject> duLieu) {
        cacQuanHe.clear();
        cacQuanHe.putAll(duLieu);
    }

    public Map<String, JsonObject> danhSach() {
        return Collections.unmodifiableMap(cacQuanHe);
    }

    public void capNhatKetQuaTim(JsonObject nguoi) {
        nguoiTimDuoc = nguoi;
        if (nguoi == null) {
            return;
        }
        String id = DuLieu.chu(nguoi, "id");
        if (DuLieu.chu(nguoi, "quanHe").equals("chuaKetBan")) {
            cacQuanHe.remove(id);
        } else {
            cacQuanHe.put(id, nguoi);
        }
    }

    public JsonObject ketQuaTim() {
        if (nguoiTimDuoc == null) {
            return null;
        }
        JsonObject nguoi = nguoiTimDuoc.deepCopy();
        JsonObject quanHe = cacQuanHe.get(DuLieu.chu(nguoi, "id"));
        nguoi.addProperty("quanHe", quanHe == null ? "chuaKetBan" : DuLieu.chu(quanHe, "quanHe"));
        return nguoi;
    }

    public long demQuanHe(String quanHe) {
        return cacQuanHe.values().stream()
                .filter(nguoi -> DuLieu.chu(nguoi, "quanHe").equals(quanHe))
                .count();
    }

    public int tongSoQuanHe() {
        return cacQuanHe.size();
    }

    public List<JsonObject> loc(String quanHe) {
        return cacQuanHe.values().stream()
                .filter(nguoi -> DuLieu.chu(nguoi, "quanHe").equals(quanHe))
                .sorted(Comparator.comparing(nguoi -> DuLieu.chu(nguoi, "tenHienThi"),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
