package com.bi.springbootinit.bizmq;

import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.exception.BusinessException;
import com.bi.springbootinit.manager.OpenAiManager;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.enums.OrderStatus;
import com.bi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
@Slf4j
public class MyMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private OpenAiManager openAIManager;

    @Value("${filedir.charts_code}")
    private String uploadDirForCharts;

    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive message:{}", message);
        if (StringUtils.isBlank(message)) {
        channel.basicNack(deliveryTag, false, false);
        throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }

        Long chartId = Long.parseLong(message);
        Chart chart=chartService.getById(chartId);
        chart.setStatus(OrderStatus.PROCESSING);
        boolean b = chartService.updateById(chart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chartId, " 更新图表执行中状态失败");
        }
        String goal= chart.getGoal();
        String chartType = chart.getChartType();
        String chartData = chart.getChartData();
        String name=chart.getName();

        String csvData = Files.readString(Paths.get(chartData), StandardCharsets.UTF_8);
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
        Map<String, Object> result = openAIManager.generate(instruction, Prompt);
        String genChart = "";
        String genResult = "";
        try {
            genChart = result.get("jscode").toString();
            genResult = result.get("analysis").toString();
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chartId, "AI 生成失败");

        }

        try {
            // 创建目标文件
            Path chartsDirPath = Paths.get(uploadDirForCharts);
            if (!Files.exists(chartsDirPath)) {
                Files.createDirectories(chartsDirPath);
            }
            String Chart_id = String.valueOf(chart.getId());
            Path ChartsPath = Paths.get(uploadDirForCharts, name + "_" + Chart_id + ".json");
            if (Files.notExists(ChartsPath)) {
                Files.write(ChartsPath, genChart.getBytes(StandardCharsets.UTF_8));
            }
            Chart updatechart = new Chart();
            updatechart.setId(chartId);
            updatechart.setGenchart(ChartsPath.toString());
            updatechart.setGenResult(genResult);
            updatechart.setStatus(OrderStatus.COMPLETED);
            boolean updateResult = chartService.updateById(updatechart);
            if (!updateResult) {
                channel.basicNack(deliveryTag, false, false);
                handleChartUpdateError(chartId, "更新图表成功状态失败");

            }

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
        //消息确认
        channel.basicAck(deliveryTag, false);
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
