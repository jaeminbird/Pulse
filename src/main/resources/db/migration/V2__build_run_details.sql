-- Track the GitHub workflow run behind each build so syncs can update
-- in-progress runs instead of duplicating or freezing them.

ALTER TABLE builds ADD COLUMN IF NOT EXISTS github_run_id BIGINT;
ALTER TABLE builds ADD COLUMN IF NOT EXISTS workflow_name VARCHAR(255);
ALTER TABLE builds ADD COLUMN IF NOT EXISTS branch VARCHAR(255);
ALTER TABLE builds ADD COLUMN IF NOT EXISTS commit_message VARCHAR(1000);
ALTER TABLE builds ADD COLUMN IF NOT EXISTS author_name VARCHAR(255);
ALTER TABLE builds ADD COLUMN IF NOT EXISTS html_url VARCHAR(500);

CREATE UNIQUE INDEX IF NOT EXISTS uq_builds_github_run_id ON builds(github_run_id);
