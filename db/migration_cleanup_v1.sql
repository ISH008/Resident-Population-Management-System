-- Cleanup script for removing legacy v1 rule residues
USE resident_mgmt;

-- 1) Remove obsolete rule table (no longer used by runtime logic)
DROP TABLE IF EXISTS resident_judge_rule;

-- 2) Normalize historical version tags to current rule version
UPDATE resident
SET judge_version = 'v2'
WHERE judge_version = 'v1';

UPDATE resident_judge_log
SET judge_version = 'v2'
WHERE judge_version = 'v1';

UPDATE resident_judge_application
SET judge_version = 'v2'
WHERE judge_version = 'v1';
