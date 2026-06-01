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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PriceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openapiKey;

    // 네이버 API 호출 로직은 NaverApiClient로 넘어갔으므로 여기서는 AI 분석만 진행!
    public List<PriceInfoRes> analyzePricesWithAI(List<ProductDto> products) throws Exception {
        List<PriceInfoRes> resultList = new ArrayList<>();

        if (products.isEmpty()) {
            return resultList;
        }

        StringBuilder compactData = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            ProductDto p = products.get(i);
            compactData.append(String.format("[%d] 쇼핑몰: %s / 상품명: %s / 가격: %d\n",
                    i, p.getSource(), p.getTitle(), p.getTotalPrice()));
        }

        String openaiUrl = "https://api.openai.com/v1/chat/completions";
        HttpHeaders openaiHeaders = new HttpHeaders();
        openaiHeaders.setContentType(MediaType.APPLICATION_JSON);
        openaiHeaders.setBearerAuth(openapiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "너는 E-커머스 최저가 분석 AI야. 검색된 모든 쇼핑몰 데이터를 통틀어 '가장 싼 단품 1개'와 '가장 싼 세트 1개' 딱 2개만 최종적으로 골라내.\n\n" +
                        "### [최저가 분석 알고리즘]\n" +
                        "1. 단품(Single) 최저가: 상품명에 묶음 표시가 없는 상품 중, 가격이 '가장 저렴한' 1개만 찾아(has_single: true).\n" +
                        "2. 세트(Bundle) 최저가: 2개 이상 묶여 있는 상품 중, '개당 단가'가 '가장 저렴한' 1개만 찾아(has_bundle: true).\n" +
                        "3. 도메인 상식: '2500매', '100T' 등은 내부 용량이니 세트 수량(total_count)으로 절대 치지 마. 1만 원이 넘는데 1개라고 우기지 마.\n" +
                        "4. 억지 할당 금지: 명확한 단품이나 세트가 없다면 억지로 만들지 말고 해당 has_ 속성을 false로 해.\n" +
                        "5. 중복 할당 금지: 동일한 index 번호를 단품과 세트에 동시에 쓰지 마."
        ));

        messages.add(Map.of("role", "user", "content", compactData.toString()));
        requestBody.put("messages", messages);

        String schemaJson = "{\"type\":\"json_schema\",\"json_schema\":{\"name\":\"price_analysis_final\",\"strict\":true,\"schema\":{\"type\":\"object\",\"properties\":{\"has_single\":{\"type\":\"boolean\"},\"single_index\":{\"type\":\"integer\"},\"single_pure_name\":{\"type\":\"string\"},\"single_unit_price\":{\"type\":\"integer\"},\"has_bundle\":{\"type\":\"boolean\"},\"bundle_index\":{\"type\":\"integer\"},\"bundle_pure_name\":{\"type\":\"string\"},\"bundle_total_count\":{\"type\":\"integer\"},\"bundle_unit_price\":{\"type\":\"integer\"}},\"required\":[\"has_single\",\"single_index\",\"single_pure_name\",\"single_unit_price\",\"has_bundle\",\"bundle_index\",\"bundle_pure_name\",\"bundle_total_count\",\"bundle_unit_price\"],\"additionalProperties\":false}}}";
        requestBody.put("response_format", objectMapper.readTree(schemaJson));

        HttpEntity<String> openaiEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), openaiHeaders);
        ResponseEntity<String> openaiResponse = restTemplate.postForEntity(openaiUrl, openaiEntity, String.class);

        JsonNode openaiBody = objectMapper.readTree(openaiResponse.getBody());
        String aiResultString = openaiBody.path("choices").get(0).path("message").path("content").asText();
        JsonNode aiResult = objectMapper.readTree(aiResultString);

        if (aiResult.path("has_single").asBoolean()) {
            int sIdx = aiResult.path("single_index").asInt();
            String sName = aiResult.path("single_pure_name").asText().trim();
            if (sIdx >= 0 && sIdx < products.size() && !sName.isEmpty()) {
                ProductDto sItem = products.get(sIdx);
                resultList.add(new PriceInfoRes(
                        "[" + sItem.getSource() + "] " + sName + " (1개)",
                        sItem.getTotalPrice(),
                        true,
                        sItem.getLink()
                ));
            }
        }

        if (aiResult.path("has_bundle").asBoolean()) {
            int bIdx = aiResult.path("bundle_index").asInt();
            String bName = aiResult.path("bundle_pure_name").asText().trim();
            if (bIdx >= 0 && bIdx < products.size() && !bName.isEmpty()) {
                ProductDto bItem = products.get(bIdx);
                resultList.add(new PriceInfoRes(
                        "[" + bItem.getSource() + "] " + bName +
                                " (총 " + aiResult.path("bundle_total_count").asInt() + "개 / 개당 " + aiResult.path("bundle_unit_price").asInt() + "원)",
                        bItem.getTotalPrice(),
                        false,
                        bItem.getLink()
                ));
            }
        }

        return resultList;
    }
}


// http://localhost:8080/api/prices/compare?keyword=삼다수 2L