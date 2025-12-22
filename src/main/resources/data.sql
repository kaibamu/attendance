-- 初期データとしてユーザーを挿入する。
INSERT INTO users (username, password, role, email, enabled) VALUES
-- 社員用ユーザーの初期データ
('employee1', 'password', 'EMPLOYEE', 'employee1@example.com', TRUE),
-- 管理者用ユーザーの初期データ
('admin1', 'adminpass', 'ADMIN', 'admin1@example.com', TRUE);