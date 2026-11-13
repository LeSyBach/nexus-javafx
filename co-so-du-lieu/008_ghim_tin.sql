USE nexus_javafx;
CREATE TABLE IF NOT EXISTS tin_ghim (tin_nhan_id BIGINT PRIMARY KEY, nguoi_ghim_id BIGINT NOT NULL, FOREIGN KEY(tin_nhan_id) REFERENCES tin_nhan(id) ON DELETE CASCADE, FOREIGN KEY(nguoi_ghim_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE) ENGINE=InnoDB;
