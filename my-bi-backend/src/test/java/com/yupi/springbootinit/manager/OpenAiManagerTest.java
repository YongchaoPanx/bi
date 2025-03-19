package com.yupi.springbootinit.manager;

import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class OpenAiManagerTest {

    @Resource
    private OpenAiManager openAiManager;


}