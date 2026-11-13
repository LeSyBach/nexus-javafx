USE nexus_javafx;
CREATE TABLE IF NOT EXISTS quyen_rieng_tu (
 nguoi_dung_id BIGINT PRIMARY KEY,
 email VARCHAR(20) NOT NULL DEFAULT 'chi_minh_toi',
 so_dien_thoai VARCHAR(20) NOT NULL DEFAULT 'chi_minh_toi',
 FOREIGN KEY(nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
