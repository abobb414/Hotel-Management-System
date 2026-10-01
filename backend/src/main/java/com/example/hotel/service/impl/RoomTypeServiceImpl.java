package com.example.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.hotel.common.PageResult;
import com.example.hotel.dto.RoomTypeQueryRequest;
import com.example.hotel.dto.RoomTypeRequest;
import com.example.hotel.entity.RoomType;
import com.example.hotel.exception.BusinessException;
import com.example.hotel.mapper.RoomMapper;
import com.example.hotel.mapper.RoomTypeMapper;
import com.example.hotel.service.RoomTypeService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomTypeServiceImpl extends ServiceImpl<RoomTypeMapper, RoomType> implements RoomTypeService {

    private final RoomMapper roomMapper;

    public RoomTypeServiceImpl(RoomMapper roomMapper) {
        this.roomMapper = roomMapper;
    }

    @Override
    public List<RoomType> listAll() {
        return list(new LambdaQueryWrapper<RoomType>().orderByAsc(RoomType::getId));
    }

    @Override
    public RoomType getDetail(Long id) {
        RoomType roomType = getById(id);
        if (roomType == null) {
            throw new BusinessException("room type does not exist");
        }
        return roomType;
    }

    @Override
    @Transactional
    public RoomType create(RoomTypeRequest request) {
        RoomType roomType = new RoomType();
        apply(roomType, request);
        save(roomType);
        return roomType;
    }

    @Override
    @Transactional
    public RoomType update(Long id, RoomTypeRequest request) {
        RoomType roomType = getDetail(id);
        apply(roomType, request);
        updateById(roomType);
        return roomType;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getDetail(id);
        Long usedCount = roomMapper.countByRoomTypeId(id);
        if (usedCount != null && usedCount > 0) {
            throw new BusinessException("room type is referenced by rooms and cannot be deleted");
        }
        removeById(id);
    }

    @Override
    public PageResult<RoomType> pageRoomTypes(RoomTypeQueryRequest request) {
        Page<RoomType> page = new Page<>(request.pageQuery().safePageNo(), request.pageQuery().safePageSize());
        LambdaQueryWrapper<RoomType> wrapper = new LambdaQueryWrapper<RoomType>()
                .like(request.keyword() != null && !request.keyword().isBlank(), RoomType::getName, request.keyword())
                .orderByAsc(RoomType::getId);
        return PageResult.from(page(page, wrapper));
    }

    private void apply(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(request.name());
        roomType.setBasePrice(request.basePrice());
        roomType.setMaxGuests(request.maxGuests());
        roomType.setBedType(request.bedType());
        roomType.setArea(request.area());
        roomType.setDescription(request.description());
        roomType.setAmenities(request.amenities());
        roomType.setCoverImage(request.coverImage());
    }
}
