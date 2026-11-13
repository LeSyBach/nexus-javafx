USE nexus_javafx;
CREATE TABLE IF NOT EXISTS moc_doc (
  nguoi_dung_id BIGINT NOT NULL,
  doi_phuong_id BIGINT NOT NULL COMMENT '0 la phong chung',
  tin_cuoi_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (nguoi_dung_id, doi_phuong_id),
  FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
) ENGINE=InnoDB;
