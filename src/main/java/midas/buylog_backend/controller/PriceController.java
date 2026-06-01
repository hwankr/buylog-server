package midas.buylog_backend.controller;

import midas.buylog_backend.dto.PriceInfoRes;
import midas.buylog_backend.service.PriceAggregationService;
import midas.buylog_backend.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceAggregationService priceAggregationService;
    private final PriceService priceService;

    @GetMapping("/compare")
    public CompletableFuture<ResponseEntity<List<PriceInfoRes>>> comparePrices(@RequestParam String keyword) {
        return priceAggregationService.getAggregatedPrices(keyword)
                .thenApply(products -> {
                    try {
                        List<PriceInfoRes> result = priceService.analyzePricesWithAI(products);
                        return ResponseEntity.ok(result);
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().build();
                    }
                });
    }
}