USE nexus_javafx;
CREATE TABLE IF NOT EXISTS nhom_chat (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nguoi_tao_id BIGINT NOT NULL,
 ma_tao CHAR(36) NOT NULL,
 ten VARCHAR(80) NOT NULL,
 tao_luc TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE (nguoi_tao_id,ma_tao),
 FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS thanh_vien_nhom (
 nhom_id BIGINT NOT NULL,
 nguoi_dung_id BIGINT NOT NULL,
 PRIMARY KEY (nhom_id,nguoi_dung_id),
 INDEX (nguoi_dung_id,nhom_id),
 FOREIGN KEY (nhom_id) REFERENCES nhom_chat(id) ON DELETE CASCADE,
 FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS tin_nhan_nhom (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nhom_id BIGINT NOT NULL,
 nguoi_gui_id BIGINT NOT NULL,
 ma_gui CHAR(36) NOT NULL,
 noi_dung TEXT NOT NULL,
 tao_luc TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE (nhom_id,nguoi_gui_id,ma_gui),
 INDEX (nhom_id,id),
 FOREIGN KEY (nhom_id) REFERENCES nhom_chat(id) ON DELETE CASCADE,
 FOREIGN KEY (nguoi_gui_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
