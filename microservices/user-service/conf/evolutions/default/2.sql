# Insert seed users

# --- !Ups
INSERT INTO users (name, email, age) VALUES
    ('Alice Cooper', 'alice@example.com', 28),
    ('Charlie Brown', 'charlie@example.com', 22),
    ('Diana Prince', 'diana@example.com', 32),
    ('Eve Adams', 'eve@example.com', 27),
    ('Frank Wilson', 'frank@example.com', 45)
ON CONFLICT (email) DO NOTHING;

# --- !Downs
DELETE FROM users WHERE email IN (
    'alice@example.com',
    'charlie@example.com',
    'diana@example.com',
    'eve@example.com',
    'frank@example.com'
);