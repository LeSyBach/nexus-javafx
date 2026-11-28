USE nexus_javafx;
ALTER TABLE tep_dinh_kem
    MODIFY COLUMN loai_tep ENUM('tep', 'anh', 'thoai') NOT NULL;
