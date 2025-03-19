package com.yupi.springbootinit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import javax.annotation.Resource;

@Configuration
public class OpenAIConfig {
    @Value("${spring.openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
