package com.bi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bi.springbootinit.annotation.AuthCheck;
import com.bi.springbootinit.bizmq.BiMqConstant;
import com.bi.springbootinit.bizmq.MyMessageProducer;
import com.bi.springbootinit.common.BaseResponse;
import com.bi.springbootinit.common.DeleteRequest;
import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.common.ResultUtils;
import com.bi.springbootinit.constant.UserConstant;
import com.bi.springbootinit.exception.BusinessException;
import com.bi.springbootinit.exception.ThrowUtils;
import com.bi.springbootinit.manager.CosManager;
import com.bi.springbootinit.manager.OpenAiManager;
import com.bi.springbootinit.manager.RedisLimiterManager;
import com.bi.springbootinit.model.dto.chart.*;
import com.bi.springbootinit.model.dto.chart.*;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.entity.User;
import com.bi.springbootinit.model.enums.OrderStatus;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.service.UserService;
import com.bi.springbootinit.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
/**
 * 帖子接口
 *
 * @author  
 * @from   
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private OpenAiManager openAIManager;
    // region 增删改查
    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);


        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());

        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart >> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        chartPage.getRecords().forEach(chart -> {
            String filePath = chart.getGenchart();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    // 读取文件内容作为 JSON 字符串（Java 11 以上版本）
                    String jsonContent = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
                    chart.setGenchart(jsonContent);
                } catch (IOException e) {
                    // 读取失败时，可以选择记录日志或者设置为空字符串、默认值等
                    e.printStackTrace();
                    chart.setGenchart("{}");
                }
            }
        });
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                         HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        chartPage.getRecords().forEach(chart -> {
            String filePath = chart.getGenchart();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    // 读取文件内容作为 JSON 字符串（Java 11 以上版本）
                    String jsonContent = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
                    chart.setGenchart(jsonContent);
                } catch (IOException e) {
                    // 读取失败时，可以选择记录日志或者设置为空字符串、默认值等
                    e.printStackTrace();
                    chart.setGenchart("{}");
                }
            }
        });
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }


    @Resource
    private CosManager cosManager;

    @Value("${filedir.charts_code}")
    private String uploadDirForCharts;
    @Value("${filedir.datas}")
    private String uploadDirForDatas;
    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private MyMessageProducer myMessageProducer;
    /**
     * excel异步文件上传
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/genChart/async/mq")
    public BaseResponse<BiResponse> genChartAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                  GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimiter("genChart_"+loginUser.getId());
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&& name.length()>100,ErrorCode.PARAMS_ERROR,"名字为空或过长");
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename =multipartFile.getOriginalFilename();
        //文件大小
        final long one_mb=5*1024*1024;
        ThrowUtils.throwIf(size>one_mb,ErrorCode.PARAMS_ERROR,"文件超过5MB");
        //文件后缀
        String suffix=FileUtil.getSuffix(originalFilename);
        String prefix=FileUtil.getPrefix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xls","xlsx","csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式不规范");


        String csvData ="";
        if (suffix.equals("csv")) {
            csvData = ExcelUtils.readCsv(multipartFile);
        }else {
            csvData = ExcelUtils.excel2Csv(multipartFile);
        }

        //先把图表保存到数据库中
        Long chartId = IdWorker.getId();
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setName(name);
        chart.setGoal(goal);
        try {
            Path datasDirPath = Paths.get(uploadDirForDatas);
            String Chart_id= String.valueOf(chart.getId());
            if (!Files.exists(datasDirPath)) {
                Files.createDirectories(datasDirPath);
            }
            Path DataFilePath = Paths.get(uploadDirForDatas,prefix+"_"+Chart_id+".csv");
            if(Files.notExists(DataFilePath)) {
                Files.write(DataFilePath,csvData.getBytes());
            }
            chart.setChartData(DataFilePath.toString());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表保存失败");
        }
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        boolean saveresult=chartService.save(chart);
        ThrowUtils.throwIf(!saveresult, ErrorCode.SYSTEM_ERROR,"图表保存失败");

        //传递消息
        myMessageProducer.sendMessage(BiMqConstant.BI_EXCHANGE_NAME,BiMqConstant.BI_ROUTING_KEY,String.valueOf(chartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setId(chart.getId());
        return ResultUtils.success(biResponse);


    }

    /**
     * excel异步
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/genChart/async")
    public BaseResponse<BiResponse> genChartAsync(@RequestPart("file") MultipartFile multipartFile,
                                                  GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimiter("genChart_"+loginUser.getId());
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&& name.length()>100,ErrorCode.PARAMS_ERROR,"名字为空或过长");
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename =multipartFile.getOriginalFilename();
        //文件大小
        final long one_mb=5*1024*1024;
        ThrowUtils.throwIf(size>one_mb,ErrorCode.PARAMS_ERROR,"文件超过5MB");
        //文件后缀
        String suffix=FileUtil.getSuffix(originalFilename);
        String prefix=FileUtil.getPrefix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xls","xlsx","csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式不规范");


        String csvData ="";
        if (suffix.equals("csv")) {
            csvData = ExcelUtils.readCsv(multipartFile);
        }else {
           csvData = ExcelUtils.excel2Csv(multipartFile);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("分析需求：\n").append(goal).append("   \n");
        if (StringUtils.isNotBlank(chartType)) {
            stringBuilder.append("图表类型:").append(chartType).append("\n");
        }
        stringBuilder.append("原始数据").append(csvData);
        String Prompt=stringBuilder.toString();
        String instruction="你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "jscode:"+
                "{前端 Echarts V5 的 option 配置对象json对象，" +
             "}\n" +
                "analysis:"+
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n";
        //先把图表保存到数据库中
        Long chartId = IdWorker.getId();
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setName(name);
        chart.setGoal(goal);
        try {
            Path datasDirPath = Paths.get(uploadDirForDatas);
            String Chart_id= String.valueOf(chart.getId());
            if (!Files.exists(datasDirPath)) {
                Files.createDirectories(datasDirPath);
            }
            Path DataFilePath = Paths.get(uploadDirForDatas,prefix+"_"+Chart_id+"."+suffix);
            if(Files.notExists(DataFilePath)) {
                multipartFile.transferTo(DataFilePath.toFile());
            }
            chart.setChartData(DataFilePath.toString());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表保存失败");
        }
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        boolean saveresult=chartService.save(chart);
        ThrowUtils.throwIf(!saveresult, ErrorCode.SYSTEM_ERROR,"图表保存失败");

        //异步AI生成
        CompletableFuture.runAsync(()->{
            Chart updateChart = new Chart();
            updateChart.setId(chartId);
            updateChart.setStatus(OrderStatus.PROCESSING);
            boolean b = chartService.updateById(updateChart);
           if(!b){
                handleChartUpdateError(chartId," 更新图表执行中状态失败");
           }
            Map<String, Object> result=openAIManager.generate(instruction,Prompt);
            String genChart="";
            String genResult="";
            try{
                genChart=result.get("jscode").toString();
                genResult=result.get("analysis").toString();
            }catch (Exception e){
                handleChartUpdateError(chartId, "AI 生成失败");
            }

            try {
                // 创建目标文件
                Path chartsDirPath = Paths.get(uploadDirForCharts);
                if (!Files.exists(chartsDirPath)) {
                    Files.createDirectories(chartsDirPath);
                }
                String Chart_id= String.valueOf(chart.getId());
                Path ChartsPath = Paths.get(uploadDirForCharts,prefix+"_"+Chart_id+".json");
                if (Files.notExists(ChartsPath)) {
                    Files.write(ChartsPath, genChart.getBytes(StandardCharsets.UTF_8));
                }
                Chart updatechart=new Chart();
                updatechart.setId(chartId);
                updatechart.setGenchart(ChartsPath.toString());
                updatechart.setGenResult(genResult);
                updatechart.setStatus(OrderStatus.COMPLETED);
                boolean updateResult=chartService.updateById(updatechart);
                if(!updateResult){
                    handleChartUpdateError(chartId,"更新图表成功状态失败");
                }

            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
            }
        },threadPoolExecutor);


        BiResponse biResponse = new BiResponse();
        biResponse.setId(chart.getId());
        return ResultUtils.success(biResponse);


    }
    @PostMapping("/genChart")
    public BaseResponse<BiResponse> genChart(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimiter("genChart_"+loginUser.getId());
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&& name.length()>100,ErrorCode.PARAMS_ERROR,"名字为空或过长");
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename =multipartFile.getOriginalFilename();
        //文件大小
        final long one_mb=5*1024*1024;
        ThrowUtils.throwIf(size>one_mb,ErrorCode.PARAMS_ERROR,"文件超过5MB");
        //文件后缀
        String suffix=FileUtil.getSuffix(originalFilename);
        String prefix=FileUtil.getPrefix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xls","xlsx","csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件格式不规范");


        String csvData ="";
        if (suffix.equals("csv")) {
            csvData = ExcelUtils.readCsv(multipartFile);
        }else {
            csvData = ExcelUtils.excel2Csv(multipartFile);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("分析需求：\n").append(goal).append("   \n");
        if (StringUtils.isNotBlank(chartType)) {
            stringBuilder.append("图表类型:").append(chartType).append("\n");
        }
        stringBuilder.append("原始数据").append(csvData);
        String Prompt=stringBuilder.toString();
        String instruction="你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "jscode:"+
                "{前端 Echarts V5 的 option 配置对象json对象，" +
                "}\n" +
                "analysis:"+
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n";
        //先把图表保存到数据库中
        Long chartId = IdWorker.getId();
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setName(name);
        chart.setGoal(goal);
        try {
            Path datasDirPath = Paths.get(uploadDirForDatas);
            String Chart_id= String.valueOf(chart.getId());
            if (!Files.exists(datasDirPath)) {
                Files.createDirectories(datasDirPath);
            }
            Path DataFilePath = Paths.get(uploadDirForDatas,prefix+"_"+Chart_id+"."+suffix);
            if(Files.notExists(DataFilePath)) {
                multipartFile.transferTo(DataFilePath.toFile());
            }
            chart.setChartData(DataFilePath.toString());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表保存失败");
        }
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        Map<String, Object> result=openAIManager.generate(instruction,Prompt);
        String genChart="";
        String genResult="";
        try{
            genChart=result.get("jscode").toString();
            genResult=result.get("analysis").toString();
        }catch (Exception e){
            handleChartUpdateError(chartId, "AI 生成失败");
        }
        try {
            // 创建目标文件
            Path chartsDirPath = Paths.get(uploadDirForCharts);
            if (!Files.exists(chartsDirPath)) {
                Files.createDirectories(chartsDirPath);
            }
            String Chart_id= String.valueOf(chart.getId());
            Path ChartsPath = Paths.get(uploadDirForCharts,prefix+"_"+Chart_id+".json");
            if (Files.notExists(ChartsPath)) {
                Files.write(ChartsPath, genChart.getBytes(StandardCharsets.UTF_8));
            }
            chart.setGenchart(ChartsPath.toString());
            chart.setGenResult(genResult);
            chart.setStatus(OrderStatus.COMPLETED);
            boolean b= chartService.save(chart);
            if(!b){
                handleChartUpdateError(chartId,"图表保存失败");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
        BiResponse biResponse = new BiResponse();
        biResponse.setId(chart.getId());
        biResponse.setGenResult(genResult);
        biResponse.setGenchart(genChart);
        return ResultUtils.success(biResponse);


    }
    private void handleChartUpdateError(long chartId,String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(OrderStatus.FAILED);
        updateChartResult.setExecMessage(execMessage);
        boolean b = chartService.updateById(updateChartResult);
        if(!b){
            log.error("更新图表失败状态失败"+chartId+","+execMessage);
        }

    }


}
