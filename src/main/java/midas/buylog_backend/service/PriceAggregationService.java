package midas.buylog_backend.service;

import midas.buylog_backend.dto.ProductDto;
import midas.buylog_backend.service.client.ShoppingApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceAggregationService {

    private final List<ShoppingApiClient> apiClients;

    public CompletableFuture<List<ProductDto>> getAggregatedPrices(String keyword) {
        List<CompletableFuture<List<ProductDto>>> futures = apiClients.stream()
                .map(client -> client.searchProductsAsync(keyword))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .flatMap(future -> future.join().stream())
                        .sorted(Comparator.comparingInt(ProductDto::getUnitPrice))
                        .collect(Collectors.toList())
                );
    }
}