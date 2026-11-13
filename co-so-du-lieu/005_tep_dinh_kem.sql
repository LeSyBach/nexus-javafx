USE nexus_javafx;
ALTER TABLE tin_nhan ADD COLUMN IF NOT EXISTS loai_tin ENUM('van_ban','tep','anh') NOT NULL DEFAULT 'van_ban';
CREATE TABLE IF NOT EXISTS tep_dinh_kem (
 tin_nhan_id BIGINT NOT NULL PRIMARY KEY,
 ma_luu CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 ten_tep VARCHAR(180) NOT NULL,
 kich_thuoc BIGINT NOT NULL,
 sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 loai_tep ENUM('tep','anh') NOT NULL,
 anh_thu MEDIUMBLOB NULL,
 FOREIGN KEY(tin_nhan_id) REFERENCES tin_nhan(id) ON DELETE CASCADE
) ENGINE=InnoDB;
