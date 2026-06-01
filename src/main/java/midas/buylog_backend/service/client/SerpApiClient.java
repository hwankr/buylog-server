package midas.buylog_backend.service.client;

import midas.buylog_backend.dto.ProductDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class SerpApiClient implements ShoppingApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${serp.api.key}")
    private String serpApiKey;

    @Override
    @Async("apiExecutor")
    public CompletableFuture<List<ProductDto>> searchProductsAsync(String keyword) {
        List<ProductDto> products = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromUriString("https://serpapi.com/search.json")
                    .queryParam("engine", "google_shopping")
                    .queryParam("q", keyword)
                    .queryParam("hl", "ko")
                    .queryParam("gl", "kr")
                    .queryParam("api_key", serpApiKey)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode shoppingResults = root.path("shopping_results");

            if (shoppingResults != null && shoppingResults.isArray()) {
                for (JsonNode item : shoppingResults) {
                    String title = item.path("title").asText();
                    String source = item.path("source").asText();
                    String link = item.path("link").asText(); // ✨ 이 줄이 빠져있어서 났던 에러입니다!

                    int price = 0;
                    if (item.has("extracted_price")) {
                        price = item.path("extracted_price").asInt();
                    } else if (item.has("price")) {
                        String priceStr = item.path("price").asText().replaceAll("[^0-9]", "");
                        if (!priceStr.isEmpty()) {
                            price = Integer.parseInt(priceStr);
                        }
                    }

                    if (price > 0) {
                        products.add(new ProductDto(source, title, price, link));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(products);
    }
}