package midas.buylog_backend.dto; // 패키지명은 재현님 프로젝트에 맞게 유지해 주세요!

import lombok.Getter;
import lombok.ToString;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@ToString
public class ProductDto {
    private final String source;
    private final String title;
    private final int totalPrice;
    private final int quantity;
    private final int unitPrice;
    private final boolean isSet;
    private final String link;

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)\\s*(개입|개입세트|개세트|개|묶음|팩|box|박스)");

    public ProductDto(String source, String title, int totalPrice, String link) {
        this.source = source;
        this.title = title.replaceAll("<[^>]*>", "");
        this.totalPrice = totalPrice;
        this.link = link;

        int parsedQuantity = parseQuantity(this.title);
        this.quantity = parsedQuantity;
        this.isSet = parsedQuantity > 1;
        this.unitPrice = totalPrice / parsedQuantity;
    }

    private int parseQuantity(String title) {
        Matcher matcher = QUANTITY_PATTERN.matcher(title.toLowerCase());
        if (matcher.find()) {
            try {
                int q = Integer.parseInt(matcher.group(1));
                return q > 0 ? q : 1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
}