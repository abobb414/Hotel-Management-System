CREATE DATABASE IF NOT EXISTS hotel_management
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE hotel_management;

DROP TABLE IF EXISTS notification_message;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS financial_transaction;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS customer_user;
DROP TABLE IF EXISTS admin_user;
DROP TABLE IF EXISTS guest;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS room_type;

CREATE TABLE room_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    max_guests INT NOT NULL,
    bed_type VARCHAR(64) NOT NULL,
    area INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    amenities VARCHAR(255) NOT NULL,
    cover_image VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(32) NOT NULL UNIQUE,
    room_type_id BIGINT NOT NULL,
    floor INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    clean_status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_room_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE guest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    id_card VARCHAR(32) NOT NULL,
    member_level VARCHAR(32) NOT NULL,
    remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    member_level VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_no VARCHAR(40) NOT NULL UNIQUE,
    guest_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guest_count INT NOT NULL,
    room_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    breakfast_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    extra_bed_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    deposit_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    coupon_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    special_request VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_check_in_time DATETIME NULL,
    actual_check_out_time DATETIME NULL,
    CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id) REFERENCES guest(id),
    CONSTRAINT fk_reservation_room FOREIGN KEY (room_id) REFERENCES room(id),
    INDEX idx_reservation_room_dates (room_id, check_in_date, check_out_date),
    INDEX idx_reservation_status_dates (status, check_in_date, check_out_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE financial_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL,
    reservation_no VARCHAR(40) NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_financial_transaction_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    INDEX idx_financial_transaction_reservation (reservation_id, created_at),
    INDEX idx_financial_transaction_type (transaction_type, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT NULL,
    room_id BIGINT NULL,
    operator_username VARCHAR(64) NOT NULL,
    operator_role VARCHAR(32) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    before_snapshot VARCHAR(500),
    after_snapshot VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_operation_log_reservation FOREIGN KEY (reservation_id) REFERENCES reservation(id),
    CONSTRAINT fk_operation_log_room FOREIGN KEY (room_id) REFERENCES room(id),
    INDEX idx_operation_log_reservation (reservation_id, created_at),
    INDEX idx_operation_log_operator (operator_username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(255) NOT NULL,
    related_type VARCHAR(32) NOT NULL,
    related_id BIGINT NOT NULL,
    target_role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
    scheduled_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    INDEX idx_notification_target (target_role, status, scheduled_at),
    INDEX idx_notification_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO room_type (name, base_price, max_guests, bed_type, area, description, amenities, cover_image) VALUES
('Urban Queen', 498.00, 2, '1.8m Queen Bed', 32, 'Suitable for business trips and city short stays.', 'Breakfast, WiFi, Smart TV', '/room-types/urban-queen.jpg'),
('Garden Twin', 568.00, 2, '2 x 1.2m Twin Bed', 36, 'Quiet floor with courtyard-facing windows.', 'Breakfast, WiFi, Tea Set', '/room-types/garden-twin.jpg'),
('Executive Suite', 968.00, 4, '1.8m Queen Bed + Sofa', 62, 'Living room layout for family or VIP guests.', 'Breakfast, Mini Bar, Bathtub', '/room-types/executive-suite.jpg');

INSERT INTO room (room_number, room_type_id, floor, status, clean_status) VALUES
('801', 1, 8, 'AVAILABLE', 'READY'),
('802', 1, 8, 'AVAILABLE', 'READY'),
('901', 2, 9, 'AVAILABLE', 'READY'),
('902', 2, 9, 'OCCUPIED', 'CLEANING'),
('1001', 3, 10, 'AVAILABLE', 'READY'),
('1002', 3, 10, 'MAINTENANCE', 'BLOCKED');

INSERT INTO guest (full_name, phone, id_card, member_level, remark) VALUES
('林若川', '13800000001', '330102199110101234', 'GOLD', 'Late check-in'),
('周清禾', '13800000002', '330102199305052456', 'REGULAR', 'Corporate booking'),
('沈嘉屿', '13800000003', '330102199512128888', 'PLATINUM', 'Needs airport pickup');

INSERT INTO customer_user (username, phone, password, display_name, member_level, status) VALUES
('13900000001', '13900000001', '$2a$10$VvN31onlQ0j5W1D2Laj0zuzQO2S4M0nB6fTP3D6JrIoYNewc0hXtS', '住客示例', 'REGULAR', 'ACTIVE');

INSERT INTO reservation (
    reservation_no, guest_id, room_id, check_in_date, check_out_date, guest_count,
    room_fee, breakfast_fee, extra_bed_fee, deposit_amount, coupon_amount, total_amount,
    status, channel, special_request, created_at
) VALUES
('RES20260422080001', 1, 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 2, 996.00, 68.00, 0.00, 300.00, 50.00, 1314.00, 'BOOKED', 'DIRECT', 'Window side preferred', NOW()),
('RES20260421093015', 2, 4, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 1136.00, 88.00, 0.00, 300.00, 0.00, 1524.00, 'CHECKED_IN', 'OTA', 'Need extra towel', NOW()),
('RES20260420114530', 3, 5, DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 3, 1936.00, 128.00, 160.00, 500.00, 100.00, 2624.00, 'BOOKED', 'DIRECT', 'Family crib', NOW());

-- 财务流水与操作日志：种子订单对应的初始凭证，保证流水页 / 日志页有可核对的数据
INSERT INTO financial_transaction (reservation_id, reservation_no, transaction_type, amount, direction, remark) VALUES
(1, 'RES20260422080001', 'ROOM_FEE', 996.00, 'CHARGE', '房费入账 · 2 晚'),
(1, 'RES20260422080001', 'BREAKFAST_FEE', 68.00, 'CHARGE', '早餐加购'),
(1, 'RES20260422080001', 'DEPOSIT', 300.00, 'CHARGE', '押金预收'),
(1, 'RES20260422080001', 'COUPON', 50.00, 'DISCOUNT', '优惠券抵扣'),
(2, 'RES20260421093015', 'ROOM_FEE', 1136.00, 'CHARGE', '房费入账 · 2 晚'),
(2, 'RES20260421093015', 'BREAKFAST_FEE', 88.00, 'CHARGE', '早餐加购'),
(2, 'RES20260421093015', 'DEPOSIT', 300.00, 'CHARGE', '押金预收'),
(3, 'RES20260420114530', 'ROOM_FEE', 1936.00, 'CHARGE', '房费入账 · 2 晚'),
(3, 'RES20260420114530', 'BREAKFAST_FEE', 128.00, 'CHARGE', '早餐加购'),
(3, 'RES20260420114530', 'EXTRA_BED_FEE', 160.00, 'CHARGE', '加床费用'),
(3, 'RES20260420114530', 'DEPOSIT', 500.00, 'CHARGE', '押金预收'),
(3, 'RES20260420114530', 'COUPON', 100.00, 'DISCOUNT', '优惠券抵扣');

INSERT INTO operation_log (
    reservation_id, room_id, operator_username, operator_role,
    action_type, description, before_snapshot, after_snapshot
) VALUES
(1, 2, 'admin', 'ADMIN', 'CREATE_RESERVATION', '创建订单 RES20260422080001', NULL,
 'reservationNo=RES20260422080001,status=BOOKED,roomId=2,checkIn=2026-10-01,checkOut=2026-10-03,total=1314.00'),
(2, 4, 'admin', 'ADMIN', 'CREATE_RESERVATION', '创建订单 RES20260421093015', NULL,
 'reservationNo=RES20260421093015,status=BOOKED,roomId=4,checkIn=2026-09-30,checkOut=2026-10-02,total=1524.00'),
(2, 4, 'frontdesk', 'FRONT_DESK', 'STATUS_CHANGE', '订单状态 BOOKED -> CHECKED_IN',
 'reservationNo=RES20260421093015,status=BOOKED,roomId=4,checkIn=2026-09-30,checkOut=2026-10-02,total=1524.00',
 'reservationNo=RES20260421093015,status=CHECKED_IN,roomId=4,checkIn=2026-09-30,checkOut=2026-10-02,total=1524.00'),
(3, 5, 'admin', 'ADMIN', 'CREATE_RESERVATION', '创建订单 RES20260420114530', NULL,
 'reservationNo=RES20260420114530,status=BOOKED,roomId=5,checkIn=2026-10-04,checkOut=2026-10-06,total=2624.00');
