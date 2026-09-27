package com.retail.flow.returns.service;

import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.returns.dto.ReturnRequestDto;
import com.retail.flow.returns.dto.ReturnResponseDto;
import com.retail.flow.returns.entity.ReturnItem;
import com.retail.flow.returns.entity.SaleReturn;
import com.retail.flow.returns.repository.SaleReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final SaleReturnRepository returnRepository;
    private final ProductVariantRepository variantRepository; // सुनिश्चित करें कि आपके पास यह रिपॉजिटरी हो

    @Transactional
    public ReturnResponseDto processReturn(ReturnRequestDto requestDto) {
        SaleReturn saleReturn = SaleReturn.builder()
                .saleId(requestDto.getSaleId())
                .returnDate(LocalDate.now())
                .reason(requestDto.getReason())
                .build();

        List<ReturnItem> returnItems = requestDto.getItems().stream().map(itemDto -> {
            // 1. वेरिएंट ढूंढें
            ProductVariant variant = variantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found with id: " + itemDto.getVariantId()));

            // 2. स्टॉक वापस बढ़ाएं (Inventory Restoration)
            variant.setStock(variant.getStock() + itemDto.getQuantity());
            variantRepository.save(variant);

            // 3. रिटर्न आइटम मैप करें
            return ReturnItem.builder()
                    .saleReturn(saleReturn)
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .quantity(itemDto.getQuantity())
                    .build();
        }).collect(Collectors.toList());

        saleReturn.setItems(returnItems);
        SaleReturn savedReturn = returnRepository.save(saleReturn);

        // Map to Response DTO
        List<ReturnResponseDto.ReturnItemResponseDto> itemResponseDtos = savedReturn.getItems().stream()
                .map(ri -> ReturnResponseDto.ReturnItemResponseDto.builder()
                        .id(ri.getId())
                        .variantId(ri.getVariantId())
                        .sku(ri.getSku())
                        .quantity(ri.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return ReturnResponseDto.builder()
                .id(savedReturn.getId())
                .saleId(savedReturn.getSaleId())
                .returnDate(savedReturn.getReturnDate())
                .reason(savedReturn.getReason())
                .items(itemResponseDtos)
                .build();
    }
}