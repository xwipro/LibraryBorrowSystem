package com.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Reservation Mapper
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
