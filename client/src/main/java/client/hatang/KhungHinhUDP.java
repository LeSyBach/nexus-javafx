package client.hatang;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

/** Chia JPEG thành gói nhỏ và ghép lại, bỏ khung cũ khi gói mất hoặc đến trễ. */
final class KhungHinhUDP {
    static final int kichThuocHeader = 32;
    static final int kichThuocManh = 1152;
    static final int gioiHanAnh = 131072;
    private long maDangNhan = -1;
    private long maDaHien = -1;
    private byte[] anh;
    private final BitSet daNhan = new BitSet();

    static List<byte[]> tach(byte[] anh, UUID token, long maKhung) {
        if (anh.length > gioiHanAnh || maKhung < 0) {
            throw new IllegalArgumentException("Khung hình vượt giới hạn.");
        }
        int tongManh = Math.max(1, (anh.length + kichThuocManh - 1) / kichThuocManh);
        List<byte[]> cacGoi = new ArrayList<>(tongManh);
        for (int i = 0; i < tongManh; i++) {
            int batDau = i * kichThuocManh;
            int soByte = Math.min(kichThuocManh, anh.length - batDau);
            ByteBuffer goi = ByteBuffer.allocate(kichThuocHeader + soByte);
            goi.putLong(token.getMostSignificantBits());
            goi.putLong(token.getLeastSignificantBits());
            goi.putLong(maKhung);
            goi.putInt(anh.length);
            goi.putShort((short) i);
            goi.putShort((short) tongManh);
            goi.put(anh, batDau, soByte);
            cacGoi.add(goi.array());
        }
        return cacGoi;
    }

    byte[] nhan(DatagramPacket packet, UUID token) {
        if (packet.getLength() < kichThuocHeader || packet.getLength() > kichThuocHeader + kichThuocManh) {
            return null;
        }
        ByteBuffer goi = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
        if (goi.getLong() != token.getMostSignificantBits() || goi.getLong() != token.getLeastSignificantBits()) {
            return null;
        }
        long maKhung = goi.getLong();
        int doDai = goi.getInt();
        int viTri = Short.toUnsignedInt(goi.getShort());
        int tongManh = Short.toUnsignedInt(goi.getShort());
        int soManhDung = Math.max(1, (doDai + kichThuocManh - 1) / kichThuocManh);
        if (maKhung < 0 || maKhung <= maDaHien || maKhung < maDangNhan
                || doDai < 0 || doDai > gioiHanAnh || tongManh != soManhDung || viTri >= tongManh) {
            return null;
        }
        int batDau = viTri * kichThuocManh;
        int soByte = Math.min(kichThuocManh, doDai - batDau);
        if (goi.remaining() != soByte) {
            return null;
        }
        if (maKhung != maDangNhan) {
            maDangNhan = maKhung;
            anh = new byte[doDai];
            daNhan.clear();
        }
        if (anh == null || anh.length != doDai || daNhan.get(viTri)) {
            return null;
        }
        goi.get(anh, batDau, soByte);
        daNhan.set(viTri);
        if (daNhan.cardinality() != tongManh) {
            return null;
        }
        byte[] ketQua = anh;
        maDaHien = maKhung;
        anh = null;
        return ketQua;
    }
}
