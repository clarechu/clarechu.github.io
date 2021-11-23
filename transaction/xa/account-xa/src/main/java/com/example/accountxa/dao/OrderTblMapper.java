package com.example.accountxa.dao;

import com.example.api.OrderTbl;
import com.example.api.OrderTblExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface OrderTblMapper {
    long countByExample(OrderTblExample example);

    int deleteByExample(OrderTblExample example);

    int insert(OrderTbl record);

    int insertSelective(OrderTbl record);

    List<OrderTbl> selectByExample(OrderTblExample example);

    int updateByExampleSelective(@Param("record") OrderTbl record, @Param("example") OrderTblExample example);

    int updateByExample(@Param("record") OrderTbl record, @Param("example") OrderTblExample example);
}