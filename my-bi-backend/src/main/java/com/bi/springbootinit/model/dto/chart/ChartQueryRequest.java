package com.bi.springbootinit.model.dto.chart;

import com.bi.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author  
 * @from   
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

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
     * 图标类型
     */
    private String chartType;
    /**
     * 创建者
     */
    private Long userId;

}