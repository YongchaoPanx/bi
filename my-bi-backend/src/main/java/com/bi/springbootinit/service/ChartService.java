package com.bi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Yongc
*/
public interface ChartService extends IService<Chart> {

    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);
}
