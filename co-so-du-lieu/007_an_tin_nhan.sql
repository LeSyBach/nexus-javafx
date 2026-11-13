USE nexus_javafx;
CREATE TABLE IF NOT EXISTS tin_da_an (
 nguoi_dung_id BIGINT NOT NULL,
 tin_nhan_id BIGINT NOT NULL,
 PRIMARY KEY(nguoi_dung_id,tin_nhan_id),
 FOREIGN KEY(nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
 FOREIGN KEY(tin_nhan_id) REFERENCES tin_nhan(id) ON DELETE CASCADE
) ENGINE=InnoDB;
