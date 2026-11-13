package client.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cache ảnh xem trước theo ID tin nhắn, ưu tiên giữ ảnh vừa truy cập. */
public final class AnhTinNhanModel {
    private static final int gioiHanAnh = 128;
    private final Map<String, byte[]> daTai = new LinkedHashMap<>(gioiHanAnh, 0.75f, true);

    public byte[] layAnh(String id) {
        return daTai.get(id);
    }

    public void luuAnh(String id, byte[] anh) {
        daTai.put(id, anh);
        if (daTai.size() > gioiHanAnh) {
            daTai.remove(daTai.keySet().iterator().next());
        }
    }

    public void xoaAnh(String id) {
        daTai.remove(id);
    }

    public void xoaTatCa() {
        daTai.clear();
    }
}
