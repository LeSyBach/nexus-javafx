USE nexus_javafx;
ALTER TABLE tin_nhan
 ADD COLUMN IF NOT EXISTS tra_loi_id BIGINT NULL,
 ADD COLUMN IF NOT EXISTS da_thu_hoi BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN IF NOT EXISTS phien_ban_tin BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS bam_noi_dung CHAR(64) NOT NULL DEFAULT '',
 ADD INDEX IF NOT EXISTS theo_tin_tra_loi (tra_loi_id);
UPDATE tin_nhan SET bam_noi_dung=SHA2(noi_dung,256) WHERE bam_noi_dung='';
CREATE TABLE IF NOT EXISTS cam_xuc_tin_nhan (
 tin_nhan_id BIGINT NOT NULL,
 nguoi_dung_id BIGINT NOT NULL,
 bieu_cam VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
 PRIMARY KEY(tin_nhan_id,nguoi_dung_id),
 FOREIGN KEY(tin_nhan_id) REFERENCES tin_nhan(id) ON DELETE CASCADE,
 FOREIGN KEY(nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
