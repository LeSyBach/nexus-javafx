USE nexus_javafx;
CREATE TABLE IF NOT EXISTS ban_be (
  nguoi_nho_id BIGINT NOT NULL,
  nguoi_lon_id BIGINT NOT NULL,
  nguoi_gui_id BIGINT NOT NULL,
  trang_thai ENUM('cho', 'ban_be') NOT NULL DEFAULT 'cho',
  tao_luc DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (nguoi_nho_id, nguoi_lon_id),
  KEY theo_nguoi_lon (nguoi_lon_id, nguoi_nho_id),
  FOREIGN KEY (nguoi_nho_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
  FOREIGN KEY (nguoi_lon_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
  FOREIGN KEY (nguoi_gui_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
