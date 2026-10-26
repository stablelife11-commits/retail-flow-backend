package com.retail.flow.product.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.product.dto.ProductRequestDto;
import com.retail.flow.product.dto.ProductResponseDto;
import com.retail.flow.product.dto.StockUpdateRequestDto;
import com.retail.flow.product.entity.Product;
import com.retail.flow.product.entity.ProductImage;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        // Enforce seller ownership: A SELLER can only create products for their own User ID
        Long authoritativeSellerId = (caller.getRole() == User.Role.ADMIN && requestDto.getSellerId() != null)
                ? requestDto.getSellerId()
                : caller.getId();

        productRepository.findBySellerIdAndProductCode(authoritativeSellerId, requestDto.getProductCode())
                .ifPresent(p -> {
                    throw new BusinessValidationException("Product code already exists for this seller.");
                });

        Product product = Product.builder()
                .sellerId(authoritativeSellerId)
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
        return mapToResponseDto(savedProduct, caller);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts(int page, int size, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productRepository.findAll(pageable).stream()
                .map(product -> mapToResponseDto(product, caller))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponseDto(product, caller);
    }

    @Transactional
    public ProductResponseDto.VariantResponseDto updateVariantStock(
            Long variantId, StockUpdateRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        // BOLA Check: Only the owning Seller or an Admin can update this variant's stock
        if (caller.getRole() != User.Role.ADMIN && !variant.getProduct().getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to modify inventory for another seller's product.");
        }

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

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private ProductResponseDto mapToResponseDto(Product product, User caller) {
        boolean canViewPurchasePrice = caller.getRole() == User.Role.ADMIN
                || (caller.getRole() == User.Role.SELLER && product.getSellerId().equals(caller.getId()));

        List<ProductResponseDto.VariantResponseDto> variantDtos = product.getVariants() == null ? List.of() :
                product.getVariants().stream()
                        .map(v -> ProductResponseDto.VariantResponseDto.builder()
                                .id(v.getId())
                                .sku(v.getSku())
                                .size(v.getSize())
                                .color(v.getColor())
                                .sellingPrice(v.getSellingPrice())
                                .purchasePrice(canViewPurchasePrice ? v.getPurchasePrice() : null)
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