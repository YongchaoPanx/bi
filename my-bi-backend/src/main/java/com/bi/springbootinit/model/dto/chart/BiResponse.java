package com.bi.springbootinit.model.dto.chart;

import lombok.Data;

/**
 * 返回生成的图表和分析
 */
@Data
public class BiResponse {


    private Long id;
    /**
     * 生成的数据
     */
    private String genchart;

    /**
     * 生成的分析结论
     */
    private String genResult;

}
