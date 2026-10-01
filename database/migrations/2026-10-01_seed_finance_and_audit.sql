USE hotel_management;

-- 补齐种子订单对应的财务流水与操作日志（原先两张表是空的，流水页 / 日志页永远无数据）。
-- 幂等：先清掉本次种子会写入的 reservation_id，再插入。
DELETE FROM financial_transaction WHERE reservation_id IN (1, 2, 3);
DELETE FROM operation_log WHERE reservation_id IN (1, 2, 3);

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
