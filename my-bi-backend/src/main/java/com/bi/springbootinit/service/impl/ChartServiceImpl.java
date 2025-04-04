package com.bi.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bi.springbootinit.constant.CommonConstant;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.mapper.ChartMapper;
import com.bi.springbootinit.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
* @author Yongc
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2025-03-05 00:03:15
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{
    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        String name = chartQueryRequest.getName();
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
;

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(name),"name", name);
        queryWrapper.eq(id !=null&&id>0  ,"id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType),"chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}




