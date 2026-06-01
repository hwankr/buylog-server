package midas.buylog_backend.service.client;

import midas.buylog_backend.dto.ProductDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import midas.buylog_backend.service.client.ShoppingApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class NaverApiClient implements ShoppingApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.client.id}")
    private String clientId;
    @Value("${naver.client.secret}")
    private String clientSecret;

    @Override
    @Async("apiExecutor") // 1단계에서 만든 비동기 스레드 풀 사용
    public CompletableFuture<List<ProductDto>> searchProductsAsync(String keyword) {
        List<ProductDto> products = new ArrayList<>();

        try {
            String url = "https://openapi.naver.com/v1/search/shop.json?query=" + keyword + "&display=10";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("items");

            for (JsonNode item : items) {
                products.add(new ProductDto(
                        item.path("mallName").asText(), // 쇼핑몰 이름 (source)
                        item.path("title").asText(),    // 상품명
                        item.path("lprice").asInt(),    // 가격
                        item.path("link").asText()      // 링크
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(products);
    }
}