package midas.buylog_backend.config; // 패키지명은 재현님의 실제 프로젝트 경로에 맞게 수정해 주세요.

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "apiExecutor")
    public Executor apiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);   // 평소에 대기하고 있을 기본 스레드 개수
        executor.setMaxPoolSize(10);   // 요청이 폭주할 때 최대로 늘릴 스레드 개수
        executor.setQueueCapacity(100); // 스레드가 다 찼을 때 대기시킬 큐의 크기
        executor.setThreadNamePrefix("API-Call-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}