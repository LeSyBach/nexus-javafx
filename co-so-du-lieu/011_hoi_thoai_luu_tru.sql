USE nexus_javafx;
CREATE TABLE IF NOT EXISTS hoi_thoai_luu_tru (
 nguoi_dung_id BIGINT NOT NULL,
 doi_phuong_id BIGINT NOT NULL,
 PRIMARY KEY (nguoi_dung_id,doi_phuong_id),
 FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
 FOREIGN KEY (doi_phuong_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
