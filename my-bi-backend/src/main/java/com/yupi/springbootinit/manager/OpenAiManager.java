package com.yupi.springbootinit.manager;

import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.ChatModel;
import org.springframework.stereotype.Component;
import com.openai.models.ResponseFormatJsonSchema.JsonSchema;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.openai.core.JsonValue;
import java.util.Map;
import com.openai.models.ResponseFormatJsonSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static java.util.stream.Collectors.toList;

/**
 * openai 处理excel数据
 */
@Component
public class OpenAiManager {

    @Resource
    private OpenAIClient openAIClient;


    public String generateWithInstruction(String instruction,String prompt) {


        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O)
                .addDeveloperMessage(instruction)
                .addUserMessage(prompt)
                .build();
        ChatCompletion chatCompletion = openAIClient.chat().completions().create(createParams);
        String answer= chatCompletion.choices().stream()
                .map(choice -> choice.message().content().orElse(""))  // 如果 Optional 为空则返回空字符串
                .filter(s -> !s.isEmpty())  // 过滤掉空字符串
                .collect(Collectors.joining("\n"));
        return answer;

    }
    public Map<String, Object> generate(String instruction,String prompt) {

        // TODO: Update this once we support extracting JSON schemas from Java classes
        JsonSchema.Schema schema = JsonSchema.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty(
                        "properties", JsonValue.from(Map.of("jscode",  Map.of("type", "string"),"analysis", Map.of("type", "string"))))
                .build();
        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O)
                .responseFormat(ResponseFormatJsonSchema.builder()
                        .jsonSchema(JsonSchema.builder()
                                .name("chart")
                                .schema(schema)
                                .build())
                        .build())
                .addDeveloperMessage(instruction)
                .addUserMessage(prompt)
                .build();
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resultList=new ArrayList<Map<String, Object>>();
        openAIClient.chat().completions().create(createParams).choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .forEach(jsonContent -> {
                    try {
                        // 将 JSON 字符串解析为 Map<String, Object>
                        Map<String, Object> resultMap = mapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
                        // 这里可以根据需要对 resultMap 进行操作，例如打印或处理数
                        resultList.add(resultMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        return resultList.get(0);
    }

}
