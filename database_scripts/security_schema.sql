-- Tables for Application Security
CREATE TABLE app_user (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL, -- To store hashed passwords
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE app_role (
    role_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Junction table for many-to-many relationship between users and roles
CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    FOREIGN KEY (role_id) REFERENCES app_role(role_id)
);