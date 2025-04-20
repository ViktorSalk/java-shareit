-- Создание таблиц для тестов
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS item_requests;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE item_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(1000) NOT NULL,
    requestor_id BIGINT NOT NULL,
    created TIMESTAMP NOT NULL,
    FOREIGN KEY (requestor_id) REFERENCES users(id)
);

CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(512) NOT NULL,
    available BOOLEAN NOT NULL,
    owner_id BIGINT NOT NULL,
    request_id BIGINT,
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (request_id) REFERENCES item_requests(id)
);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    item_id BIGINT NOT NULL,
    booker_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (booker_id) REFERENCES users(id)
);

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text VARCHAR(1000) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    created TIMESTAMP NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_bookings_item ON bookings(item_id);
CREATE INDEX IF NOT EXISTS idx_bookings_booker ON bookings(booker_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_dates ON bookings(start_date, end_date);