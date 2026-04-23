-- Resident Management System V1
-- MySQL 8.0+
-- Charset: utf8mb4

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS resident_mgmt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE resident_mgmt;

-- =============================
-- 1) RBAC
-- =============================
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt encrypted password',
  real_name VARCHAR(64) NULL,
  phone VARCHAR(20) NULL,
  resident_id BIGINT NULL COMMENT '绑定居民档案ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1-enabled,0-disabled',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_resident_id (resident_id),
  KEY idx_sys_user_phone (phone),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System users';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL COMMENT 'ADMIN/USER',
  role_name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System roles';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sur_role_id (role_id),
  CONSTRAINT fk_sur_user_id FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_sur_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-role relation';

-- =============================
-- 2) Resident core
-- =============================
DROP TABLE IF EXISTS resident_judge_log;
DROP TABLE IF EXISTS resident_judge_rule;
DROP TABLE IF EXISTS resident_judge_application_attachment;
DROP TABLE IF EXISTS resident_judge_application;
DROP TABLE IF EXISTS resident_mobility_log;
DROP TABLE IF EXISTS resident;
DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE resident (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  id_card VARCHAR(18) NOT NULL,
  gender CHAR(1) NOT NULL COMMENT 'M/F',
  birthday DATE NULL,
  phone VARCHAR(20) NULL,
  actual_address VARCHAR(255) NULL,
  address_province VARCHAR(32) NULL,
  address_city VARCHAR(32) NULL,
  address_district VARCHAR(32) NULL,
  address_detail VARCHAR(255) NULL,
  residence_type VARCHAR(32) NULL COMMENT 'PERMANENT/TEMPORARY',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/MOVED_OUT/DEACTIVATED',

  -- Residence judgement fields
  residence_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'RESIDENT/NON_RESIDENT/PENDING',
  residence_score INT NOT NULL DEFAULT 0,
  stay_start_date DATE NULL,
  stay_end_date DATE NULL,
  is_local_hukou TINYINT NOT NULL DEFAULT 0,
  proof_type VARCHAR(64) NULL COMMENT 'HOUSE_CERT/RENT_CONTRACT/RESIDENCE_PERMIT/OTHER',
  last_judge_time DATETIME NULL,
  judge_version VARCHAR(32) NULL,
  judge_reason VARCHAR(500) NULL,

  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_resident_id_card (id_card),
  KEY idx_resident_name (name),
  KEY idx_resident_phone (phone),
  KEY idx_resident_status (status),
  KEY idx_resident_residence_status (residence_status),
  KEY idx_resident_stay_start (stay_start_date),
  KEY idx_resident_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resident archive';

CREATE TABLE resident_judge_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL COMMENT 'STAY_180_DAYS/VALID_PROOF/LOCAL_ACTIVITY_90D etc',
  rule_name VARCHAR(128) NOT NULL,
  weight INT NOT NULL DEFAULT 0,
  threshold_value VARCHAR(64) NULL COMMENT 'e.g. 180 days',
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  version VARCHAR(32) NOT NULL DEFAULT 'v1',
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rule_code_version (rule_code, version),
  KEY idx_rule_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Residence judgement rules';

CREATE TABLE resident_judge_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resident_id BIGINT NOT NULL,
  rule_id BIGINT NULL,
  rule_code VARCHAR(64) NULL,
  hit_flag TINYINT NOT NULL COMMENT '1-hit,0-not hit',
  score_delta INT NOT NULL DEFAULT 0,
  final_score INT NOT NULL DEFAULT 0,
  final_status VARCHAR(32) NOT NULL COMMENT 'RESIDENT/NON_RESIDENT/PENDING',
  judge_reason VARCHAR(500) NULL,
  judge_version VARCHAR(32) NOT NULL DEFAULT 'v1',
  judge_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  operator_id BIGINT NULL COMMENT 'null means system job',
  KEY idx_rjl_resident_id (resident_id),
  KEY idx_rjl_judge_time (judge_time),
  KEY idx_rjl_final_status (final_status),
  CONSTRAINT fk_rjl_resident_id FOREIGN KEY (resident_id) REFERENCES resident(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Residence judgement logs';

ALTER TABLE sys_user
ADD CONSTRAINT fk_sys_user_resident_id FOREIGN KEY (resident_id) REFERENCES resident(id);

CREATE TABLE resident_judge_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resident_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  apply_reason VARCHAR(500) NOT NULL,
  evidence_text VARCHAR(1000) NULL,
  local_employ_social TINYINT NULL COMMENT '1-yes,0-no,null-unknown',
  local_activity_90d TINYINT NULL COMMENT '1-yes,0-no,null-unknown',
  judge_version VARCHAR(32) NOT NULL DEFAULT 'v1',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  review_comment VARCHAR(500) NULL,
  reviewer_id BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_rja_resident_id (resident_id),
  KEY idx_rja_applicant_id (applicant_id),
  KEY idx_rja_status (status),
  KEY idx_rja_created_at (created_at),
  CONSTRAINT fk_rja_resident_id FOREIGN KEY (resident_id) REFERENCES resident(id),
  CONSTRAINT fk_rja_applicant_id FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
  CONSTRAINT fk_rja_reviewer_id FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resident judgement applications';

CREATE TABLE resident_judge_application_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NULL,
  file_size BIGINT NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploader_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rjaa_application_id (application_id),
  KEY idx_rjaa_created_at (created_at),
  CONSTRAINT fk_rjaa_application_id FOREIGN KEY (application_id) REFERENCES resident_judge_application(id),
  CONSTRAINT fk_rjaa_uploader_id FOREIGN KEY (uploader_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Judge application attachments';

CREATE TABLE resident_mobility_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resident_id BIGINT NOT NULL,
  change_type VARCHAR(16) NOT NULL COMMENT 'INFLOW/OUTFLOW',
  change_date DATE NOT NULL,
  from_region VARCHAR(120) NOT NULL,
  to_region VARCHAR(120) NOT NULL,
  reason VARCHAR(200) NOT NULL,
  remark VARCHAR(500) NULL,
  operator_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rml_resident_id (resident_id),
  KEY idx_rml_change_date (change_date),
  CONSTRAINT fk_rml_resident_id FOREIGN KEY (resident_id) REFERENCES resident(id),
  CONSTRAINT fk_rml_operator_id FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resident mobility logs';

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NULL,
  operator_username VARCHAR(64) NULL,
  module VARCHAR(64) NOT NULL,
  action VARCHAR(128) NOT NULL,
  target_id VARCHAR(64) NULL,
  request_method VARCHAR(16) NULL,
  request_path VARCHAR(255) NULL,
  result VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAIL',
  message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sol_operator_id (operator_id),
  KEY idx_sol_module (module),
  KEY idx_sol_result (result),
  KEY idx_sol_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation audit logs';

-- =============================
-- 3) Seed data
-- =============================
INSERT INTO sys_role (role_code, role_name, status)
VALUES
  ('ADMIN', CONVERT(0xE7AEA1E79086E59198 USING utf8mb4), 1),
  ('USER', CONVERT(0xE699AEE9809AE794A8E688B7 USING utf8mb4), 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status);

-- Development only: plain text password "123456". Must switch to BCrypt hash in production.
INSERT INTO sys_user (username, password, real_name, phone, status)
VALUES ('admin', '123456', '系统管理员', '13800000000', 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), phone = VALUES(phone), status = VALUES(status);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), role_id = VALUES(role_id);

-- v1 judgement rules
INSERT INTO resident_judge_rule (rule_code, rule_name, weight, threshold_value, enabled, sort_order, version, description)
VALUES
  ('STAY_180_DAYS', '连续居住>=180天', 50, '180', 1, 1, 'v1', 'stay_end_date - stay_start_date >= 180'),
  ('VALID_PROOF', '有有效居住证明', 20, '1', 1, 2, 'v1', 'proof_type not null'),
  ('LOCAL_EMPLOY_SOCIAL', '有本地就业/社保/学籍证明', 20, '1', 1, 3, 'v1', '外部证明项，后端布尔入参'),
  ('LOCAL_ACTIVITY_90D', '近90天有本地活动记录', 10, '1', 1, 4, 'v1', '外部行为证据项')
ON DUPLICATE KEY UPDATE
  rule_name = VALUES(rule_name),
  weight = VALUES(weight),
  threshold_value = VALUES(threshold_value),
  enabled = VALUES(enabled),
  sort_order = VALUES(sort_order),
  description = VALUES(description);

SET FOREIGN_KEY_CHECKS = 1;
