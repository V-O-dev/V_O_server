-- 1. User 먼저 생성
INSERT INTO users (id, status, daily_question_notification_enabled, interaction_notification_enabled, created_at, updated_at)
VALUES (1, 'ACTIVE', true, true, now(), now())
ON CONFLICT (id) DO NOTHING;

-- 2. UserProfile 생성 (User를 참조하므로 반드시 그 다음)
INSERT INTO user_profiles (user_id, nickname, profile_image_url, profile_image_object_key, created_at, updated_at)
VALUES (1, '나타샤', 'https://example.com/dummy-profile.jpg', 'dummy/profile-key.jpg', now(), now())
ON CONFLICT (user_id) DO NOTHING;

-- 3. AuthOauthAccount 생성 (User를 참조하므로 User보다 반드시 나중)
INSERT INTO auth_oauth_accounts (id, provider, provider_user_id, provider_email, provider_email_verified, status, linked_at, user_id)
VALUES (1, 'KAKAO', 'dummy-kakao-id-123', 'dummy@example.com', true, 'ACTIVE', now(), 1)
ON CONFLICT (id) DO NOTHING;

-- 시퀀스를 현재 최대 id 값으로 동기화 (직접 id를 지정해서 insert할 경우 필수)
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('auth_oauth_accounts', 'id'), COALESCE((SELECT MAX(id) FROM auth_oauth_accounts), 0) + 1, false);