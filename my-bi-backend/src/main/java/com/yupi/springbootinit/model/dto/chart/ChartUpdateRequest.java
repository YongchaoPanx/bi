package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;


/**
 * 更新请求
 *
 * @author  
 * @from   
 */
@Data
public class ChartUpdateRequest implements Serializable {

    private Long id;
    /**
     * 图表名称
     */
    private String name;
    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图标类型
     */
    private String chartType;

}