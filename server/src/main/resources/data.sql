-- Проверка существования перед вставкой
INSERT INTO users (name, email)
SELECT 'User 1', 'user1@example.com'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user1@example.com');

INSERT INTO users (name, email)
SELECT 'User 2', 'user2@example.com'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'user2@example.com');
