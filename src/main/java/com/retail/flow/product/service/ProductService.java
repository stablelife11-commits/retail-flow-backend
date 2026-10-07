package com.retail.flow.product.service;

import com.retail.flow.product.dto.ProductRequestDto;
import com.retail.flow.product.dto.ProductResponseDto;
import com.retail.flow.product.dto.StockUpdateRequestDto;
import com.retail.flow.product.entity.Product;
import com.retail.flow.product.entity.ProductImage;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest; // 🟢 NAYA
import org.springframework.data.domain.Pageable; // 🟢 NAYA
import org.springframework.data.domain.Sort; // 🟢 NAYA
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        productRepository.findBySellerIdAndProductCode(requestDto.getSellerId(), requestDto.getProductCode())
                .ifPresent(p -> {
                    throw new RuntimeException("Product code already exists for this seller.");
                });

        Product product = Product.builder()
                .sellerId(requestDto.getSellerId())
                .productCode(requestDto.getProductCode())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .category(requestDto.getCategory())
                .brand(requestDto.getBrand())
                .active(true)
                .build();

        List<ProductVariant> variants = requestDto.getVariants().stream()
                .map(vDto -> ProductVariant.builder()
                        .product(product)
                        .sku(vDto.getSku())
                        .size(vDto.getSize())
                        .color(vDto.getColor())
                        .sellingPrice(vDto.getSellingPrice())
                        .purchasePrice(vDto.getPurchasePrice())
                        .stock(vDto.getStock())
                        .build())
                .collect(Collectors.toList());

        product.setVariants(variants);

        if (requestDto.getImageUrls() != null && !requestDto.getImageUrls().isEmpty()) {
            List<ProductImage> images = requestDto.getImageUrls().stream()
                    .map(url -> ProductImage.builder()
                            .product(product)
                            .imageUrl(url)
                            .isPrimary(false)
                            .build())
                    .collect(Collectors.toList());
            images.get(0).setIsPrimary(true);
            product.setImages(images);
        }

        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    // 🟢 FIX: Backend now handles exact Pages & Sizes (Sorted by newest first)
    public List<ProductResponseDto> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepository.findAll(pageable).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponseDto(product);
    }

    @Transactional
    public ProductResponseDto.VariantResponseDto updateVariantStock(Long variantId, StockUpdateRequestDto requestDto) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + variantId));

        variant.setStock(requestDto.getStock());
        ProductVariant savedVariant = variantRepository.save(variant);

        return ProductResponseDto.VariantResponseDto.builder()
                .id(savedVariant.getId())
                .sku(savedVariant.getSku())
                .size(savedVariant.getSize())
                .color(savedVariant.getColor())
                .sellingPrice(savedVariant.getSellingPrice())
                .purchasePrice(savedVariant.getPurchasePrice())
                .stock(savedVariant.getStock())
                .build();
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        List<ProductResponseDto.VariantResponseDto> variantDtos = product.getVariants() == null ? List.of() :
                product.getVariants().stream()
                        .map(v -> ProductResponseDto.VariantResponseDto.builder()
                                .id(v.getId())
                                .sku(v.getSku())
                                .size(v.getSize())
                                .color(v.getColor())
                                .sellingPrice(v.getSellingPrice())
                                .purchasePrice(v.getPurchasePrice())
                                .stock(v.getStock())
                                .build())
                        .collect(Collectors.toList());

        List<String> imageUrls = product.getImages() == null ? List.of() :
                product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.toList());

        return ProductResponseDto.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .active(product.getActive())
                .variants(variantDtos)
                .imageUrls(imageUrls)
                .build();
    }
}