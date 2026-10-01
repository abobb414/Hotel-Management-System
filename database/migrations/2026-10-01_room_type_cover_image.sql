USE hotel_management;

-- 房型封面图：可选字段，前端按相对路径或完整 URL 渲染
ALTER TABLE room_type
    ADD COLUMN cover_image VARCHAR(255) DEFAULT NULL AFTER amenities;

UPDATE room_type SET cover_image = '/room-types/urban-queen.jpg' WHERE name = 'Urban Queen';
UPDATE room_type SET cover_image = '/room-types/garden-twin.jpg' WHERE name = 'Garden Twin';
UPDATE room_type SET cover_image = '/room-types/executive-suite.jpg' WHERE name = 'Executive Suite';
