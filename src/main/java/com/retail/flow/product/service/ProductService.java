package com.retail.flow.product.service;

import com.retail.flow.common.dto.PagedResponseDto;
import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.inventory.entity.InventoryTransaction;
import com.retail.flow.inventory.repository.InventoryTransactionRepository;
import com.retail.flow.product.dto.ProductRequestDto;
import com.retail.flow.product.dto.ProductResponseDto;
import com.retail.flow.product.dto.StockUpdateRequestDto;
import com.retail.flow.product.dto.VariantRequestDto;
import com.retail.flow.product.entity.Product;
import com.retail.flow.product.entity.ProductImage;
import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        Long authoritativeSellerId = (caller.getRole() == User.Role.ADMIN && requestDto.getSellerId() != null)
                ? requestDto.getSellerId()
                : caller.getId();

        String trimmedProductCode = requestDto.getProductCode().trim();

        productRepository.findBySellerIdAndProductCode(authoritativeSellerId, trimmedProductCode)
                .ifPresent(p -> {
                    throw new BusinessValidationException("Product code '" + trimmedProductCode + "' already exists in your catalog.");
                });

        Set<String> requestSkus = new HashSet<>();
        for (VariantRequestDto vDto : requestDto.getVariants()) {
            String normalizedSku = vDto.getSku().trim();
            if (!requestSkus.add(normalizedSku.toUpperCase())) {
                throw new BusinessValidationException("Duplicate SKU '" + normalizedSku + "' found in the same product request.");
            }
            if (variantRepository.existsBySellerIdAndSku(authoritativeSellerId, normalizedSku)) {
                throw new BusinessValidationException("SKU '" + normalizedSku + "' already exists in your catalog.");
            }
            validateMoqAndPackSize(vDto);
        }

        Product product = Product.builder()
                .sellerId(authoritativeSellerId)
                .productCode(trimmedProductCode)
                .name(requestDto.getName().trim())
                .description(requestDto.getDescription())
                .category(requestDto.getCategory())
                .brand(requestDto.getBrand())
                .active(true)
                .build();

        List<ProductVariant> variants = requestDto.getVariants().stream()
                .map(vDto -> ProductVariant.builder()
                        .sellerId(authoritativeSellerId)
                        .product(product)
                        .sku(vDto.getSku().trim())
                        .size(vDto.getSize())
                        .color(vDto.getColor())
                        .sellingPrice(vDto.getSellingPrice())
                        .purchasePrice(vDto.getPurchasePrice())
                        .stock(vDto.getStock())
                        .minimumOrderQuantity(vDto.getMinimumOrderQuantity() != null ? vDto.getMinimumOrderQuantity() : 1)
                        .packSize(vDto.getPackSize() != null ? vDto.getPackSize() : 1)
                        .hsnCode(vDto.getHsnCode())
                        .gstPercentage(vDto.getGstPercentage() != null ? vDto.getGstPercentage() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        product.setVariants(variants);
        applyProductImages(product, requestDto.getImageUrls());

        Product savedProduct = productRepository.save(product);

        List<InventoryTransaction> initialLogs = savedProduct.getVariants().stream()
                .filter(v -> v.getStock() != null && v.getStock() > 0)
                .map(v -> InventoryTransaction.builder()
                        .productVariant(v)
                        .type(InventoryTransaction.TransactionType.ADJUSTMENT)
                        .quantity(v.getStock())
                        .referenceType("INITIAL_STOCK")
                        .referenceId(savedProduct.getId())
                        .previousStock(0)
                        .newStock(v.getStock())
                        .build())
                .collect(Collectors.toList());

        if (!initialLogs.isEmpty()) {
            inventoryTransactionRepository.saveAll(initialLogs);
        }

        return mapToResponseDto(savedProduct, caller);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (caller.getRole() != User.Role.ADMIN && !product.getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to edit another seller's product.");
        }

        Long sellerId = product.getSellerId();
        String trimmedProductCode = requestDto.getProductCode().trim();

        productRepository.findBySellerIdAndProductCode(sellerId, trimmedProductCode)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(product.getId())) {
                        throw new BusinessValidationException("Product code '" + trimmedProductCode + "' is already used by another product in your catalog.");
                    }
                });

        product.setProductCode(trimmedProductCode);
        product.setName(requestDto.getName().trim());
        product.setDescription(requestDto.getDescription());
        product.setCategory(requestDto.getCategory());
        product.setBrand(requestDto.getBrand());

        Map<Long, ProductVariant> existingById = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        Map<String, ProductVariant> existingBySku = product.getVariants().stream()
                .collect(Collectors.toMap(v -> v.getSku().toUpperCase(), v -> v, (a, b) -> a));

        Set<String> requestSkus = new HashSet<>();
        List<InventoryTransaction> stockAdjustmentLogs = new ArrayList<>();

        for (VariantRequestDto vDto : requestDto.getVariants()) {
            validateMoqAndPackSize(vDto);
            String normalizedSku = vDto.getSku().trim();
            if (!requestSkus.add(normalizedSku.toUpperCase())) {
                throw new BusinessValidationException("Duplicate SKU '" + normalizedSku + "' found in the update request.");
            }

            ProductVariant targetVariant = null;
            if (vDto.getId() != null) {
                targetVariant = existingById.get(vDto.getId());
                if (targetVariant == null) {
                    throw new BusinessValidationException("Variant ID " + vDto.getId() + " does not belong to this product.");
                }
            } else {
                targetVariant = existingBySku.get(normalizedSku.toUpperCase());
            }

            if (targetVariant != null) {
                if (!targetVariant.getSku().equalsIgnoreCase(normalizedSku)
                        && variantRepository.existsBySellerIdAndSku(sellerId, normalizedSku)) {
                    throw new BusinessValidationException("SKU '" + normalizedSku + "' already exists in your catalog.");
                }

                int previousStock = targetVariant.getStock();
                int newStock = vDto.getStock();

                targetVariant.setSku(normalizedSku);
                targetVariant.setSize(vDto.getSize());
                targetVariant.setColor(vDto.getColor());
                targetVariant.setSellingPrice(vDto.getSellingPrice());
                targetVariant.setPurchasePrice(vDto.getPurchasePrice());
                targetVariant.setStock(newStock);
                targetVariant.setMinimumOrderQuantity(vDto.getMinimumOrderQuantity() != null ? vDto.getMinimumOrderQuantity() : 1);
                targetVariant.setPackSize(vDto.getPackSize() != null ? vDto.getPackSize() : 1);
                targetVariant.setHsnCode(vDto.getHsnCode());
                targetVariant.setGstPercentage(vDto.getGstPercentage() != null ? vDto.getGstPercentage() : BigDecimal.ZERO);

                if (previousStock != newStock) {
                    stockAdjustmentLogs.add(InventoryTransaction.builder()
                            .productVariant(targetVariant)
                            .type(InventoryTransaction.TransactionType.ADJUSTMENT)
                            .quantity(Math.abs(newStock - previousStock))
                            .referenceType("PRODUCT_UPDATE")
                            .referenceId(product.getId())
                            .previousStock(previousStock)
                            .newStock(newStock)
                            .build());
                }
            } else {
                if (variantRepository.existsBySellerIdAndSku(sellerId, normalizedSku)) {
                    throw new BusinessValidationException("SKU '" + normalizedSku + "' already exists in your catalog.");
                }

                ProductVariant newVariant = ProductVariant.builder()
                        .sellerId(sellerId)
                        .product(product)
                        .sku(normalizedSku)
                        .size(vDto.getSize())
                        .color(vDto.getColor())
                        .sellingPrice(vDto.getSellingPrice())
                        .purchasePrice(vDto.getPurchasePrice())
                        .stock(vDto.getStock())
                        .minimumOrderQuantity(vDto.getMinimumOrderQuantity() != null ? vDto.getMinimumOrderQuantity() : 1)
                        .packSize(vDto.getPackSize() != null ? vDto.getPackSize() : 1)
                        .hsnCode(vDto.getHsnCode())
                        .gstPercentage(vDto.getGstPercentage() != null ? vDto.getGstPercentage() : BigDecimal.ZERO)
                        .build();

                product.getVariants().add(newVariant);
            }
        }

        if (requestDto.getImageUrls() != null) {
            if (product.getImages() != null) {
                product.getImages().clear();
            } else {
                product.setImages(new ArrayList<>());
            }
            if (!requestDto.getImageUrls().isEmpty()) {
                for (int i = 0; i < requestDto.getImageUrls().size(); i++) {
                    product.getImages().add(ProductImage.builder()
                            .product(product)
                            .imageUrl(requestDto.getImageUrls().get(i))
                            .sortOrder(i)
                            .isPrimary(i == 0)
                            .build());
                }
            }
        }

        Product savedProduct = productRepository.save(product);
        if (!stockAdjustmentLogs.isEmpty()) {
            inventoryTransactionRepository.saveAll(stockAdjustmentLogs);
        }

        return mapToResponseDto(savedProduct, caller);
    }

    @Transactional
    public ProductResponseDto updateProductStatus(Long productId, boolean active, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (caller.getRole() != User.Role.ADMIN && !product.getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to change the status of another seller's product.");
        }

        product.setActive(active);
        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct, caller);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts(
            int page, int size, String keyword, String category, String brand, Long sellerId, String callerEmail) {
        return getProductsPaged(page, size, keyword, category, brand, sellerId, callerEmail).getContent();
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<ProductResponseDto> getProductsPaged(
            int page, int size, String keyword, String category, String brand, Long sellerId, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());

        String cleanKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String cleanCategory = StringUtils.hasText(category) ? category.trim() : null;
        String cleanBrand = StringUtils.hasText(brand) ? brand.trim() : null;

        Page<Product> productPage = (caller.getRole() == User.Role.ADMIN)
                ? productRepository.searchAllProductsForAdmin(cleanKeyword, cleanCategory, cleanBrand, sellerId, pageable)
                : productRepository.searchActiveMarketplaceProducts(cleanKeyword, cleanCategory, cleanBrand, sellerId, pageable);

        List<ProductResponseDto> content = productPage.getContent().stream()
                .map(product -> mapToResponseDto(product, caller))
                .collect(Collectors.toList());

        return PagedResponseDto.<ProductResponseDto>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getMyProducts(int page, int size, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("id").descending());

        return productRepository.findBySellerId(caller.getId(), pageable).stream()
                .map(product -> mapToResponseDto(product, caller))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        boolean isOwnerOrAdmin = caller.getRole() == User.Role.ADMIN
                || (caller.getRole() == User.Role.SELLER && product.getSellerId().equals(caller.getId()));

        if (!isOwnerOrAdmin) {
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new ResourceNotFoundException("Product is no longer available.");
            }
            User seller = userRepository.findById(product.getSellerId()).orElse(null);
            if (seller == null || !Boolean.TRUE.equals(seller.getActive())) {
                throw new ResourceNotFoundException("Product is no longer available.");
            }
        }

        return mapToResponseDto(product, caller);
    }

    @Transactional
    public ProductResponseDto.VariantResponseDto updateVariantStock(
            Long variantId, StockUpdateRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (caller.getRole() != User.Role.ADMIN && !variant.getProduct().getSellerId().equals(caller.getId())) {
            throw new AccessDeniedException("You are not authorized to modify inventory for another seller's product.");
        }

        int previousStock = variant.getStock();
        int newStock = requestDto.getStock();

        variant.setStock(newStock);
        ProductVariant savedVariant = variantRepository.save(variant);

        if (previousStock != newStock) {
            inventoryTransactionRepository.save(InventoryTransaction.builder()
                    .productVariant(savedVariant)
                    .type(InventoryTransaction.TransactionType.ADJUSTMENT)
                    .quantity(Math.abs(newStock - previousStock))
                    .referenceType("MANUAL")
                    .referenceId(savedVariant.getId())
                    .previousStock(previousStock)
                    .newStock(newStock)
                    .build());
        }

        return ProductResponseDto.VariantResponseDto.builder()
                .id(savedVariant.getId())
                .sku(savedVariant.getSku())
                .size(savedVariant.getSize())
                .color(savedVariant.getColor())
                .sellingPrice(savedVariant.getSellingPrice())
                .purchasePrice(savedVariant.getPurchasePrice())
                .stock(savedVariant.getStock())
                .minimumOrderQuantity(savedVariant.getMinimumOrderQuantity())
                .packSize(savedVariant.getPackSize())
                .hsnCode(savedVariant.getHsnCode())
                .gstPercentage(savedVariant.getGstPercentage())
                .build();
    }

    private void validateMoqAndPackSize(VariantRequestDto vDto) {
        int moq = vDto.getMinimumOrderQuantity() != null ? vDto.getMinimumOrderQuantity() : 1;
        int pack = vDto.getPackSize() != null ? vDto.getPackSize() : 1;
        if (moq % pack != 0) {
            throw new BusinessValidationException(
                    "Minimum order quantity (" + moq + ") for SKU '" + vDto.getSku()
                            + "' must be a multiple of its pack size (" + pack + ").");
        }
    }

    private void applyProductImages(Product product, List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                images.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrls.get(i))
                        .sortOrder(i)
                        .isPrimary(i == 0)
                        .build());
            }
            product.setImages(images);
        }
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
                                .minimumOrderQuantity(v.getMinimumOrderQuantity())
                                .packSize(v.getPackSize())
                                .hsnCode(v.getHsnCode())
                                .gstPercentage(v.getGstPercentage())
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