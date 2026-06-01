package midas.buylog_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceInfoRes {

    private String store;       // 쇼핑몰 이름 + 상품 요약 (예: [쿠팡] 코멧 화장지)
    private int price;          // 가격
    private boolean isSingle;   // 단품 최저가 여부 (true/false)
    private String buyLink;     // 구매 링크
}