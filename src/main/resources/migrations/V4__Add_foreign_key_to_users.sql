ALTER TABLE users
    ADD COLUMN role_id INT,
ADD CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id);