USE nexus_javafx;
ALTER TABLE tin_nhan
    MODIFY COLUMN loai_tin ENUM('van_ban','tep','anh','cuoc_goi') NOT NULL DEFAULT 'van_ban',
    ADD COLUMN IF NOT EXISTS cuoc_goi_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN IF NOT EXISTS ket_qua_goi VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS thoi_luong_goi BIGINT NULL,
    ADD UNIQUE INDEX IF NOT EXISTS cuoc_goi_theo_nguoi (nguoi_gui_id, cuoc_goi_id);
