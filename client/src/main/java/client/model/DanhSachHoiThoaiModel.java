package client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.time.Instant;
import java.util.*;

/** Dữ liệu danh sách hội thoại, bạn bè, nhóm và người đang online. */
public final class DanhSachHoiThoaiModel {
    private final List<JsonObject> cacNguoi = new ArrayList<>();
    private final Map<String, JsonObject> cacBan = new HashMap<>();
    private final Map<String, JsonObject> cacHoiThoai = new HashMap<>();
    private final Map<String, JsonObject> cacNhom = new HashMap<>();

    public void capNhatNguoi(JsonArray danhSach, String idCuaMinh) {
        cacNguoi.clear();
        for (JsonElement phanTu : danhSach) {
            JsonObject nguoi = phanTu.getAsJsonObject();
            if (!DuLieu.chu(nguoi, "id").equals(idCuaMinh)) {
                cacNguoi.add(nguoi);
            }
        }
    }

    public long capNhatBanBe(Map<String, JsonObject> cacQuanHe) {
        cacBan.clear();
        cacQuanHe.forEach((id, nguoi) -> {
            if (DuLieu.chu(nguoi, "quanHe").equals("banBe")) {
                cacBan.put(id, nguoi);
            }
        });
        return cacQuanHe.values().stream()
                .filter(nguoi -> DuLieu.chu(nguoi, "quanHe").equals("daNhan"))
                .count();
    }

    public Optional<JsonObject> timNguoi(String id) {
        return cacNguoi.stream().filter(nguoi -> DuLieu.chu(nguoi, "id").equals(id)).findFirst();
    }

    public boolean laBanBe(String id) {
        return cacBan.containsKey(id);
    }

    public Collection<JsonObject> banBe() {
        return Collections.unmodifiableCollection(cacBan.values());
    }

    public Map<String, JsonObject> banSaoBanBe() {
        return new HashMap<>(cacBan);
    }

    public Map<String, JsonObject> banSaoHoiThoai() {
        return new HashMap<>(cacHoiThoai);
    }

    public List<String> banBeGanDay() {
        return cacHoiThoai.keySet().stream()
                .filter(cacBan::containsKey)
                .sorted(Comparator.comparing(this::thoiDiemHoiThoai).reversed())
                .limit(8)
                .toList();
    }

    public JsonObject timHoiThoai(String id) {
        return cacHoiThoai.get(id);
    }

    public void capNhatHoiThoai(Map<String, JsonObject> duLieu) {
        cacHoiThoai.clear();
        cacHoiThoai.putAll(duLieu);
    }

    public void xoaHoiThoai(String id) {
        cacHoiThoai.remove(id);
    }

    public boolean daLuuTru(String id) {
        JsonObject hoiThoai = timHoiThoai(id);
        return hoiThoai != null && hoiThoai.has("daLuuTru") && hoiThoai.get("daLuuTru").getAsBoolean();
    }

    public JsonObject timNhom(String idDanhSach) {
        return cacNhom.get(idDanhSach);
    }

    public void themNhomNeuThieu(JsonObject nhom) {
        cacNhom.putIfAbsent("nhom:" + DuLieu.chu(nhom, "id"), nhom);
    }

    public void capNhatNhom(JsonObject nhom) {
        cacNhom.put("nhom:" + DuLieu.chu(nhom, "id"), nhom);
    }

    public List<JsonObject> loc(String timKiem, boolean xemLuuTru, String doiPhuong) {
        String chuoi = timKiem.strip().toLowerCase(Locale.ROOT);
        List<JsonObject> nguoiHienThi = new ArrayList<>(cacNguoi);
        for (JsonObject ban : cacBan.values()) {
            if (nguoiHienThi.stream()
                    .noneMatch(nguoi -> DuLieu.chu(nguoi, "id").equals(DuLieu.chu(ban, "id")))) {
                nguoiHienThi.add(ban);
            }
        }
        for (JsonObject hoiThoai : cacHoiThoai.values()) {
            String id = DuLieu.chu(hoiThoai, "id");
            if (!id.equals("0") &&
                    nguoiHienThi.stream().noneMatch(nguoi -> DuLieu.chu(nguoi, "id").equals(id))) {
                nguoiHienThi.add(
                        DuLieu.tao("id", id, "tenHienThi", DuLieu.chu(hoiThoai, "tenHienThi"),
                                "dangOnline", false));
            }
        }
        if (!xemLuuTru) {
            for (var muc : cacNhom.entrySet()) {
                nguoiHienThi.add(DuLieu.tao("id", muc.getKey(), "tenHienThi",
                        DuLieu.chu(muc.getValue(), "tenHienThi")));
            }
        }
        return nguoiHienThi.stream()
                .filter(nguoi -> {
                    String id = DuLieu.chu(nguoi, "id");
                    return cacNhom.containsKey(id) || cacHoiThoai.containsKey(id) ||
                            Objects.equals(doiPhuong, id);
                })
                .filter(nguoi -> daLuuTru(DuLieu.chu(nguoi, "id")) == xemLuuTru)
                .filter(nguoi -> DuLieu.chu(nguoi, "tenHienThi").toLowerCase(Locale.ROOT)
                        .contains(chuoi))
                .sorted(Comparator.<JsonObject, Instant>comparing(
                        nguoi -> thoiDiemHoiThoai(DuLieu.chu(nguoi, "id"))).reversed())
                .toList();
    }

    private Instant thoiDiemHoiThoai(String id) {
        JsonObject nhom = cacNhom.get(id), hoiThoai = cacHoiThoai.get(id);
        if (nhom != null) {
            return Instant.parse(DuLieu.chu(nhom, "hoatDong"));
        }
        if (hoiThoai != null) {
            return Instant.parse(DuLieu.chu(hoiThoai.getAsJsonObject("tinCuoi"), "taoLuc"));
        }
        return Instant.EPOCH;
    }
}
