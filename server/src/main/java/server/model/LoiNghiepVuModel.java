package server.model;

public final class LoiNghiepVuModel extends RuntimeException {
    private final String ma;

    public LoiNghiepVuModel(String ma, String thongBao) {
        super(thongBao);
        this.ma = ma;
    }

    public String ma() {
        return ma;
    }
}
