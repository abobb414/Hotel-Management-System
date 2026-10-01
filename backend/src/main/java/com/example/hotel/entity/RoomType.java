package com.example.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("room_type")
public class RoomType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal basePrice;

    private Integer maxGuests;

    private String bedType;

    private Integer area;

    private String description;

    private String amenities;

    /** 房型封面图（站点内相对路径或完整 URL） */
    private String coverImage;
}
