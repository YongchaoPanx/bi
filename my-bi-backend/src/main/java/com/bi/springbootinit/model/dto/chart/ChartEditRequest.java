package com.bi.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑请求
 *
 * @author  
 * @from   
 */
@Data
public class ChartEditRequest implements Serializable {

    private Long id;

    /**
     * 分析目标
     */
    private String goal;
    /**
     * 图表名称
     */
    private String name;
    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图标类型
     */
    private String chartType;

}