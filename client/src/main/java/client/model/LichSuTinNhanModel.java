package client.model;

import com.google.gson.JsonObject;
import giaothuc.DuLieu;

import java.util.*;

/** Dữ liệu tin đang mở, phiên bản cập nhật gần đây và mốc đọc. */
public final class LichSuTinNhanModel {
    private final Map<String, JsonObject> tinTheoId = new LinkedHashMap<>();
    private final Map<String, JsonObject> tinDoiGanDay = new LinkedHashMap<>();
    private final Map<String, Long> mocDaGui = new HashMap<>();
    private long mocDoiPhuong;

    public JsonObject timTin(String id) {
        return tinTheoId.get(id);
    }

    public boolean coTin(String id) {
        return tinTheoId.containsKey(id);
    }

    public boolean rong() {
        return tinTheoId.isEmpty();
    }

    public List<String> maCacTin() {
        return List.copyOf(tinTheoId.keySet());
    }

    public List<JsonObject> cacTinTangDan() {
        return tinTheoId.values().stream()
                .sorted(Comparator.comparingLong(tin -> Long.parseLong(DuLieu.chu(tin, "id"))))
                .toList();
    }

    public String maTinCuNhat() {
        return tinTheoId.keySet().stream().min(Comparator.comparingLong(Long::parseLong)).orElse(null);
    }

    public long maTinMoiNhat() {
        return tinTheoId.keySet().stream().mapToLong(Long::parseLong).max().orElse(0);
    }

    public void xoaTin(String id) {
        tinTheoId.remove(id);
        tinDoiGanDay.remove(id);
    }

    public void xoaTrangDangMo() {
        tinTheoId.clear();
        mocDoiPhuong = 0;
    }

    public void xoaCapNhatGanDay() {
        tinDoiGanDay.clear();
    }

    public boolean thuocHoiThoai(JsonObject tin, String doiPhuong, String idCuaMinh) {
        String nguoiNhan = DuLieu.chu(tin, "nguoiNhanId");
        String nguoiGui = DuLieu.chu(tin, "nguoiGuiId");
        return doiPhuong == null ? nguoiNhan.isEmpty() :
                (nguoiGui.equals(idCuaMinh) && nguoiNhan.equals(doiPhuong)) ||
                (nguoiGui.equals(doiPhuong) && nguoiNhan.equals(idCuaMinh));
    }

    public void capNhatMocDoiPhuong(long moc) {
        mocDoiPhuong = Math.max(mocDoiPhuong, moc);
    }

    public boolean doiPhuongDaXem(String id) {
        return Long.parseLong(id) <= mocDoiPhuong;
    }

    public boolean ghiMocDaGui(String doiPhuong, long tinCuoi) {
        String khoa = khoaMocDoc(doiPhuong);
        if (tinCuoi <= mocDaGui.getOrDefault(khoa, 0L)) {
            return false;
        }
        mocDaGui.put(khoa, tinCuoi);
        return true;
    }

    public void boMocDaGui(String doiPhuong, long tinCuoi) {
        mocDaGui.remove(khoaMocDoc(doiPhuong), tinCuoi);
    }

    private String khoaMocDoc(String doiPhuong) {
        return doiPhuong == null ? "0" : doiPhuong;
    }

    public boolean nhanTinDoi(JsonObject tin) {
        String id = DuLieu.chu(tin, "id");
        JsonObject cu = tinDoiGanDay.get(id);
        if (cu != null && phienBan(cu) > phienBan(tin)) {
            return false;
        }

        tinDoiGanDay.put(id, tin);
        if (tinDoiGanDay.size() > 256) {
            tinDoiGanDay.remove(tinDoiGanDay.keySet().iterator().next());
        }
        if (coTin(id)) {
            ghiNhanTin(tin);
        }
        capNhatCacTrichDan();
        return true;
    }

    public void capNhatCacTrichDan() {
        tinTheoId.values().forEach(this::capNhatTrichDan);
    }

    public static boolean daThuHoi(JsonObject tin) {
        return tin.has("daThuHoi") && tin.get("daThuHoi").getAsBoolean();
    }

    private static String rutGon(String chu) {
        return chu.substring(0,
                chu.offsetByCodePoints(0, Math.min(120, chu.codePointCount(0, chu.length()))));
    }

    private long phienBan(JsonObject tin) {
        String chu = DuLieu.chu(tin, "phienBan");
        return chu.isEmpty() ? 0 : Long.parseLong(chu);
    }

    public void ghiNhanTin(JsonObject tin) {
        String id = DuLieu.chu(tin, "id");
        JsonObject cu = tinTheoId.get(id), moi = tinDoiGanDay.get(id);
        if (moi != null && phienBan(moi) > phienBan(tin)) {
            tin = moi;
        }
        if (cu != null && phienBan(cu) > phienBan(tin)) {
            tin = cu;
        }
        tin = tin.deepCopy();
        capNhatTrichDan(tin);
        tinTheoId.put(id, tin);
    }

    private void capNhatTrichDan(JsonObject tin) {
        if (!tin.has("trichDan")) {
            return;
        }
        String id = DuLieu.chu(tin, "traLoiId");
        JsonObject goc = tinDoiGanDay.get(id);
        if (goc == null) {
            goc = tinTheoId.get(id);
        }
        if (goc != null && phienBan(goc) >= phienBan(tin.getAsJsonObject("trichDan"))) {
            tin.add("trichDan",
                    DuLieu.tao("id", id, "tenNguoiGui", DuLieu.chu(goc, "tenNguoiGui"), "nguoiGuiId",
                            DuLieu.chu(goc, "nguoiGuiId"), "noiDung", rutGon(DuLieu.chu(goc, "noiDung")),
                            "loaiTin", DuLieu.chu(goc, "loaiTin"), "daThuHoi",
                            daThuHoi(goc), "phienBan", Long.toString(phienBan(goc))));
        }
    }
}
