package server.model;

import com.google.gson.*;
import giaothuc.DuLieu;
import server.dao.BanBeDao;
import server.dao.CSDLDao;
import server.dao.HoSoDao;

import java.sql.SQLException;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

public final class HoSoModel {
    private final HoSoDao hoSoDao;
    private final BanBeDao banBeDao;

    public HoSoModel(CSDLDao csdl) {
        hoSoDao = new HoSoDao(csdl);
        banBeDao = new BanBeDao(csdl);
    }

    public JsonObject doc(long id) throws SQLException {
        return hoSoDao.doc(id);
    }

    /**
     * Lọc ở máy chủ, không gửi thông tin bị ẩn về máy khách.
     */
    public JsonObject docChoNguoiXem(long chuHoSo, long nguoiXem) throws SQLException {
        JsonObject hoSo = doc(chuHoSo);
        String quanHe = banBeDao.quanHe(nguoiXem, chuHoSo);
        if (chuHoSo != nguoiXem) {
            if (!duocXem(DuLieu.chu(hoSo, "quyenEmail"), quanHe)) {
                hoSo.remove("email");
            }
            if (!duocXem(DuLieu.chu(hoSo, "quyenDienThoai"), quanHe)) {
                hoSo.remove("soDienThoai");
            }
        }
        hoSo.remove("quyenEmail");
        hoSo.remove("quyenDienThoai");
        hoSo.addProperty("quanHe", quanHe);
        return hoSo;
    }

    private boolean duocXem(String quyen, String quanHe) {
        return quyen.equals("moiNguoi") || (quyen.equals("banBe") && quanHe.equals("banBe"));
    }

    public void luuQuyen(long nguoi, JsonObject duLieu) throws SQLException {
        String email = DuLieu.chu(duLieu, "quyenEmail"), dienThoai = DuLieu.chu(duLieu, "quyenDienThoai");
        Set<String> hopLe = Set.of("moiNguoi", "banBe", "chiMinhToi");
        if (!hopLe.contains(email) || !hopLe.contains(dienThoai)) {
            throw new LoiNghiepVuModel("quyenSai", "Quyền riêng tư không hợp lệ.");
        }
        hoSoDao.luuQuyen(nguoi, email, dienThoai);
    }

    public void capNhat(long id, JsonObject duLieu) throws SQLException {
        String ten = DuLieu.chu(duLieu, "tenHienThi").strip(), dienThoai =
                DuLieu.chu(duLieu, "soDienThoai").strip();
        String gioiThieu = DuLieu.chu(duLieu, "gioiThieu").strip(), anhChu =
                DuLieu.chu(duLieu, "anhDaiDien"), phienBan = DuLieu.chu(duLieu, "phienBan");
        if (ten.length() < 2 || ten.length() > 100 || ten.chars().anyMatch(Character::isISOControl)) {
            throw new LoiNghiepVuModel("tenSai", "Tên hiển thị cần 2–100 ký tự.");
        }
        if (!dienThoai.isEmpty() && !dienThoai.matches("[+]?[0-9]{8,15}")) {
            throw new LoiNghiepVuModel("dienThoaiSai",
                    "Số điện thoại cần 8–15 chữ số, có thể bắt đầu bằng +.");
        }
        if (gioiThieu.length() > 240 ||
                gioiThieu.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r')) {
            throw new LoiNghiepVuModel("gioiThieuSai", "Giới thiệu tối đa 240 ký tự.");
        }
        if (!phienBan.matches("[0-9]{1,18}")) {
            throw new IllegalArgumentException();
        }
        byte[] anh = kiemTraAnh(anhChu);
        hoSoDao.capNhat(id, ten, dienThoai, gioiThieu, anh, Long.parseLong(phienBan));
    }

    public byte[] kiemTraAnh(String chu) {
        if (chu.isEmpty()) {
            return null;
        }
        if (chu.length() > 44000) {
            throw new LoiNghiepVuModel("anhSai", "Ảnh đại diện tối đa 32 KB sau xử lý.");
        }
        ImageReader boDoc = null;
        try {
            byte[] anh = Base64.getDecoder().decode(chu);
            if (anh.length > 32768) {
                throw new IOException();
            }
            try (ImageInputStream dauVao = ImageIO.createImageInputStream(new ByteArrayInputStream(anh))) {
                var cacBoDoc = ImageIO.getImageReaders(dauVao);
                if (!cacBoDoc.hasNext()) {
                    throw new IOException();
                }
                boDoc = cacBoDoc.next();
                boDoc.setInput(dauVao);
                String dinhDang = boDoc.getFormatName();
                if (!(dinhDang.equalsIgnoreCase("JPEG") || dinhDang.equalsIgnoreCase("PNG")) ||
                        boDoc.getWidth(0) > 512 || boDoc.getHeight(0) > 512 || boDoc.getWidth(0) < 1 ||
                        boDoc.getHeight(0) < 1 || boDoc.read(0) == null) {
                    throw new IOException();
                }
                return anh;
            }
        } catch (IOException | IllegalArgumentException loi) {
            throw new LoiNghiepVuModel("anhSai",
                    "Ảnh đại diện cần là PNG/JPEG hợp lệ, tối đa 512×512 và 32 KB.");
        } finally {
            if (boDoc != null) {
                boDoc.dispose();
            }
        }
    }

    /**
     * Đổi mật khẩu và thu hồi những phiên khác trong cùng giao dịch.
     */
    public void doiMatKhau(long id, String cu, String moi, String phienHienTai) throws SQLException {
        if (cu.length() > 128 || cu.isEmpty() || moi.length() < 8 || moi.length() > 128 || cu.equals(moi)) {
            throw new LoiNghiepVuModel("matKhauSai",
                    "Mật khẩu mới cần 8–128 ký tự và khác mật khẩu hiện tại.");
        }
        hoSoDao.doiMatKhau(id, cu, moi, phienHienTai);
    }

}
