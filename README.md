# AI Analysis Visualization Platform

## Introduction
Based on Spring Boot + AI APIs, this AI analysis and visualization platform only requires the user to import the raw data and analysis requirements. It then automatically generates visual charts and analysis conclusions.

1. **Business Process**  
   The backend defines custom prompt templates, encapsulates the user’s input table JSON configuration and analysis conclusions, and returns them to the frontend for rendering.

2. **Token Limitation**  
   Due to the API’s input token limit, Easy Excel is used to parse the user-uploaded XLSX spreadsheets, thereby reducing costs.

3. **Security Measures**  
   To ensure system security, multiple checks are performed on the uploaded data files, including file extension, size, and content.

4. **Distributed Rate Limiting**  
   To prevent malicious resource occupation by certain users, a distributed rate limit is implemented using Redisson’s RateLimiter, controlling the access frequency of individual users.

6. **Asynchronous Execution**  
   Because the AI API response time can be relatively long, a custom IO-intensive thread pool and task queue are used to achieve concurrent execution and asynchronization (AGC). Once tasks are submitted, the frontend immediately receives a response.
