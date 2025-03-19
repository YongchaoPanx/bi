package com.yupi.springbootinit.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.yupi.springbootinit.common.ResultUtils;
import io.swagger.models.auth.In;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelUtils {
    /**
     * excel to csv
     * @param multipartFile
     * @return
     */
    public static String excel2Csv(MultipartFile multipartFile) {
//
//        try{
//            file= ResourceUtils.getFile("classpath:test_excel.xlsx");
//
//        }catch (FileNotFoundException e){
//            e.printStackTrace();
//        }
        List<Map<Integer,String>> list =null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //数据为空
        if (CollUtil.isEmpty(list)) {
            return "" ;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0;i<list.size();i++){
            LinkedHashMap<Integer,String> map = (LinkedHashMap) list.get(i);
            List<String> dataList =  map.values().stream().collect(Collectors.toList());
            stringBuilder.append(String.join(",",dataList)).append("\n");
        }
        return stringBuilder.toString();
    }
    public static String readCsv(MultipartFile multipartFile){
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
}
