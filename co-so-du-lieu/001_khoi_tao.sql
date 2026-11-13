-- CSDL rieng cho ban JavaFX. Khong thao tac CSDL nexus cua ban web.
CREATE DATABASE IF NOT EXISTS nexus_javafx CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nexus_javafx;
CREATE TABLE IF NOT EXISTS nguoi_dung (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  ten_hien_thi VARCHAR(100) NOT NULL,
  mat_khau_bam VARCHAR(255) NOT NULL,
  tao_luc DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS tin_nhan (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_gui CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  nguoi_gui_id BIGINT NOT NULL,
  nguoi_nhan_id BIGINT NULL,
  noi_dung VARCHAR(4000) NOT NULL,
  tao_luc DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY ma_gui_theo_nguoi (nguoi_gui_id, ma_gui),
  KEY lich_su_rieng (nguoi_nhan_id, id),
  FOREIGN KEY (nguoi_gui_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
  FOREIGN KEY (nguoi_nhan_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
