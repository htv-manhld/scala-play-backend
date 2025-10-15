# Create users table

# --- !Ups
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 0,
    role SMALLINT NOT NULL DEFAULT 0,
    birthdate DATE,
    last_login_at TIMESTAMP,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);

-- Add check constraint for status
ALTER TABLE users ADD CONSTRAINT check_users_status
    CHECK (status IN (0, 1));

-- Add check constraint for role
ALTER TABLE users ADD CONSTRAINT check_users_role
    CHECK (role IN (0, 1, 2));

-- Comments for documentation
COMMENT ON TABLE users IS 'User management table storing basic user information';
COMMENT ON COLUMN users.id IS 'Primary key - auto incrementing user ID';
COMMENT ON COLUMN users.name IS 'User full name';
COMMENT ON COLUMN users.email IS 'User email address - must be unique';
COMMENT ON COLUMN users.password IS 'Hashed password for user authentication';
COMMENT ON COLUMN users.status IS 'User account status - 0: inactive, 1: active';
COMMENT ON COLUMN users.role IS 'User role - 0: user, 1: admin, 2: moderator';
COMMENT ON COLUMN users.birthdate IS 'User date of birth';
COMMENT ON COLUMN users.last_login_at IS 'Timestamp of user last login';
COMMENT ON COLUMN users.verified_at IS 'Timestamp when user email was verified';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user was last updated';

-- Insert seed users with default password: 12345678 (BCrypt hashed)
INSERT INTO users (name, email, password, status, role, birthdate) VALUES
    ('Admin User', 'admin@gmail.com', '$2a$10$aEpBioHI.KbxqCplT76t..8sk4cUA8P.LvcI/CkttGmcp2dJqfVdy', 1, 1, '1990-01-01'),
    ('Alice Cooper', 'alice@example.com', '$2a$10$aEpBioHI.KbxqCplT76t..8sk4cUA8P.LvcI/CkttGmcp2dJqfVdy', 1, 0, '1996-05-15'),
    ('Charlie Brown', 'charlie@example.com', '$2a$10$aEpBioHI.KbxqCplT76t..8sk4cUA8P.LvcI/CkttGmcp2dJqfVdy', 0, 0, '2002-08-20'),
    ('Diana Prince', 'diana@example.com', '$2a$10$aEpBioHI.KbxqCplT76t..8sk4cUA8P.LvcI/CkttGmcp2dJqfVdy', 1, 2, '1992-12-10'),
    ('Eve Adams', 'eve@example.com', '$2a$10$aEpBioHI.KbxqCplT76t..8sk4cUA8P.LvcI/CkttGmcp2dJqfVdy', 0, 0, '1997-03-25'),
    ('Frank Wilson', 'frank@example.com', '$2a$10$aEpBioHI.KbxqCplT76t..8sk4cUA8P.LvcI/CkttGmcp2dJqfVdy', 1, 0, '1979-11-08')
ON CONFLICT (email) DO NOTHING;

# --- !Downs
DROP TABLE users;
