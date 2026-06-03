package midas.buylog_backend.service;

import midas.buylog_backend.dto.ProductDto;
import midas.buylog_backend.dto.PriceInfoRes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PriceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String AI_SYSTEM_PROMPT =
            "너는 E-커머스 데이터를 분석하는 기계적인 데이터 추출기야. 오직 아래의 [절대 규칙]에 따라서만 JSON 배열을 작성해.\n\n" +
                    "### [절대 규칙]\n" +
                    "1. 분류 기준:\n" +
                    "   - 단품(Single): 제공된 텍스트에 '수량: 1개'라고 적힌 데이터만 단품 후보.\n" +
                    "   - 세트(Bundle): 제공된 텍스트에 '수량: N개 (N은 2 이상)'라고 적힌 데이터만 세트 후보.\n" +
                    "2. 미끼 상품 영구 제외:\n" +
                    "   - 가격이 비정상적으로 싼 경우(예: 정상 시세 대비 50% 이하) 즉시 탈락시켜.\n" +
                    "3. 최종 선택 로직 (Top 3 추출):\n" +
                    "   - 단품 최저가 Top 3: 미끼를 제외한 '수량: 1개' 후보들 중, [총 가격]이 가장 낮은 순서대로 최대 3개를 선택해.\n" +
                    "   - 세트 최저가 Top 3: '수량: 2개 이상' 후보들 중, [개당 가격]이 가장 낮은 순서대로 최대 3개를 선택해.\n" +
                    "4. 금지 사항:\n" +
                    "   - 단품과 세트에 같은 index 번호를 절대 중복해서 할당하지 마.\n" +
                    "   - 조건에 맞는 상품이 3개가 안 되면 있는 것만 배열에 넣어. 억지로 만들지 마.\n\n" +
                    "이제 내가 주는 실제 데이터를 분석해서 'single_results' 배열과 'bundle_results' 배열로 응답해.";
    @Value("${openai.api.key}")
    private String openapiKey;

    private static final String SCHEMA_JSON = "{\"type\":\"json_schema\",\"json_schema\":{\"name\":\"price_analysis_top3\",\"strict\":true,\"schema\":{\"type\":\"object\",\"properties\":{\"single_results\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\"},\"pure_name\":{\"type\":\"string\"},\"unit_price\":{\"type\":\"integer\"}},\"required\":[\"index\",\"pure_name\",\"unit_price\"],\"additionalProperties\":false}},\"bundle_results\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"index\":{\"type\":\"integer\"},\"pure_name\":{\"type\":\"string\"},\"total_count\":{\"type\":\"integer\"},\"unit_price\":{\"type\":\"integer\"}},\"required\":[\"index\",\"pure_name\",\"total_count\",\"unit_price\"],\"additionalProperties\":false}}},\"required\":[\"single_results\",\"bundle_results\"],\"additionalProperties\":false}}}";

    public List<PriceInfoRes> analyzePricesWithAI(List<ProductDto> products) throws Exception {
        List<PriceInfoRes> resultList = new ArrayList<>();

        if (products.isEmpty()) {
            return resultList;
        }

        // 중복 링크 제거
        List<ProductDto> uniqueProducts = new ArrayList<>();
        Set<String> linkSet = new HashSet<>();

        for (ProductDto p : products) {
            if (!linkSet.contains(p.getLink())) {
                linkSet.add(p.getLink());
                uniqueProducts.add(p);
            }
        }

        StringBuilder compactData = new StringBuilder();
        for (int i = 0; i < uniqueProducts.size(); i++) {
            ProductDto p = uniqueProducts.get(i);

            String nameHint = p.getTitle();
            int quantity = p.getQuantity();

            compactData.append(String.format(
                    "[%d] 쇼핑몰: %s / 상품명: %s / 총 가격: %d원 / 수량: %d개 / 개당 가격: %d원\n",
                    i, p.getSource(), nameHint, p.getTotalPrice(), quantity, p.getUnitPrice()
            ));
        }

        String openaiUrl = "https://api.openai.com/v1/chat/completions";
        HttpHeaders openaiHeaders = new HttpHeaders();
        openaiHeaders.setContentType(MediaType.APPLICATION_JSON);
        openaiHeaders.setBearerAuth(openapiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");

        List<Map<String, String>> messages = new ArrayList<>();

        // 클래스 상단에 선언해둔 상수를 사용
        messages.add(Map.of("role", "system", "content", AI_SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", compactData.toString()));

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.0);

        // 클래스 상단에 선언해둔 상수를 사용
        requestBody.put("response_format", objectMapper.readTree(SCHEMA_JSON));

        HttpEntity<String> openaiEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), openaiHeaders);
        ResponseEntity<String> openaiResponse = restTemplate.postForEntity(openaiUrl, openaiEntity, String.class);

        JsonNode openaiBody = objectMapper.readTree(openaiResponse.getBody());
        String aiResultString = openaiBody.path("choices").get(0).path("message").path("content").asText();
        JsonNode aiResult = objectMapper.readTree(aiResultString);

        if (aiResult.has("single_results")) {
            for (JsonNode node : aiResult.path("single_results")) {
                int sIdx = node.path("index").asInt();
                String sName = node.path("pure_name").asText().trim();

                if (sIdx >= 0 && sIdx < uniqueProducts.size() && !sName.isEmpty()) {
                    ProductDto sItem = uniqueProducts.get(sIdx);
                    resultList.add(new PriceInfoRes(
                            "[" + sItem.getSource() + "] " + sName + " (1개)",
                            sItem.getTotalPrice(),
                            true,
                            sItem.getLink()
                    ));
                }
            }
        }

        if (aiResult.has("bundle_results")) {
            for (JsonNode node : aiResult.path("bundle_results")) {
                int bIdx = node.path("index").asInt();
                String bName = node.path("pure_name").asText().trim();

                if (bIdx >= 0 && bIdx < uniqueProducts.size() && !bName.isEmpty()) {
                    ProductDto bItem = uniqueProducts.get(bIdx);
                    resultList.add(new PriceInfoRes(
                            "[" + bItem.getSource() + "] " + bName +
                                    " (총 " + node.path("total_count").asInt() + "개 / 개당 " + node.path("unit_price").asInt() + "원)",
                            bItem.getTotalPrice(),
                            false,
                            bItem.getLink()
                    ));
                }
            }
        }

        return resultList;
    }
}


// http://localhost:8080/api/prices/compare?keyword=삼다수 2L