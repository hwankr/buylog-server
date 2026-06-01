package midas.buylog_backend.service.client;

import midas.buylog_backend.dto.ProductDto;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ShoppingApiClient {
    CompletableFuture<List<ProductDto>> searchProductsAsync(String keyword);
}