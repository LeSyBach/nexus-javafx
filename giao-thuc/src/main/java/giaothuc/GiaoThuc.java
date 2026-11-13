package giaothuc;

import com.google.gson.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;

/**
 * TCP là luồng byte. Mỗi khung: độ dài 4 byte big-endian + JSON UTF-8.
 */
public final class GiaoThuc {
    public static final int gioiHanKhung = 65536;
    private static final Gson JSON = new Gson();

    private GiaoThuc() {
    }

    public static ThongDiep doc(DataInputStream inputStream) throws IOException {
        int doDai = inputStream.readInt();
        if (doDai < 1 || doDai > gioiHanKhung) {
            throw new IOException("Độ dài khung TCP không hợp lệ.");
        }

        byte[] cacByte = new byte[doDai];
        inputStream.readFully(cacByte);
        try {
            String chuoi = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(cacByte)).toString();

            JsonObject goc = JsonParser.parseString(chuoi).getAsJsonObject();
            String loai = DuLieu.chu(goc, "loai");
            String ma = DuLieu.chu(goc, "ma");
            if (!loai.matches("[a-z][a-zA-Z0-9]{1,31}") || !(ma.isEmpty() || ma.matches("[a-zA-Z0-9_-]{16,64}"))) {
                throw new IllegalArgumentException();
            }

            if (!goc.has("duLieu") || !goc.get("duLieu").isJsonObject()) {
                throw new IllegalArgumentException();
            }
            return new ThongDiep(loai, ma, goc.getAsJsonObject("duLieu"));
        } catch (RuntimeException loi) {
            throw new IOException("Thông điệp TCP không đúng cấu trúc JSON.", loi);
        }
    }

    public static void ghi(DataOutputStream outputStream, ThongDiep thongDiep) throws IOException {
        byte[] cacByte = JSON.toJson(thongDiep).getBytes(StandardCharsets.UTF_8);
        if (cacByte.length > gioiHanKhung) {
            throw new IOException("Thông điệp vượt giới hạn.");
        }
        // Nhiều luồng có thể gửi cùng socket: phải giữ nguyên cả header lẫn payload.
        synchronized (outputStream) {
            outputStream.writeInt(cacByte.length);
            outputStream.write(cacByte);
            outputStream.flush();
        }
    }
}
