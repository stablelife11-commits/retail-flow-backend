package com.retail.flow.returns.service;

import com.retail.flow.product.entity.ProductVariant;
import com.retail.flow.product.repository.ProductVariantRepository;
import com.retail.flow.returns.dto.ReturnRequestDto;
import com.retail.flow.returns.dto.ReturnResponseDto;
import com.retail.flow.returns.entity.ReturnItem;
import com.retail.flow.returns.entity.SaleReturn;
import com.retail.flow.returns.repository.SaleReturnRepository;
import com.retail.flow.sale.entity.Sale;
import com.retail.flow.sale.entity.SaleItem;
import com.retail.flow.sale.repository.SaleRepository;
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
    private final ProductVariantRepository variantRepository;
    private final SaleRepository saleRepository; // 🟢 NAYA: Sale verify karne ke liye

    @Transactional
    public ReturnResponseDto processReturn(ReturnRequestDto requestDto) {
        // 🟢 STRICT RULE 1: Check if the Sale actually exists
        Sale sale = saleRepository.findById(requestDto.getSaleId())
                .orElseThrow(() -> new RuntimeException("Invalid Sale ID. This sale does not exist."));

        SaleReturn saleReturn = SaleReturn.builder()
                .saleId(sale.getId())
                .returnDate(LocalDate.now())
                .reason(requestDto.getReason())
                .build();

        List<ReturnItem> returnItems = requestDto.getItems().stream().map(itemDto -> {

            // 🟢 STRICT RULE 2: Check if this variant was actually sold in this Sale
            SaleItem originalSaleItem = sale.getSaleItems().stream()
                    .filter(si -> si.getProductVariant().getId().equals(itemDto.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Fraud Alert: Variant ID " + itemDto.getVariantId() + " was not part of Sale ID " + sale.getId()));

            // 🟢 STRICT RULE 3: Check if return quantity exceeds sold quantity
            if (itemDto.getQuantity() > originalSaleItem.getQuantity()) {
                throw new RuntimeException("Fraud Alert: Trying to return more items than were originally sold.");
            }

            ProductVariant variant = variantRepository.findById(itemDto.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found with id: " + itemDto.getVariantId()));

            // Inventory Restoration
            variant.setStock(variant.getStock() + itemDto.getQuantity());
            variantRepository.save(variant);

            return ReturnItem.builder()
                    .saleReturn(saleReturn)
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .quantity(itemDto.getQuantity())
                    .build();
        }).collect(Collectors.toList());

        saleReturn.setItems(returnItems);
        SaleReturn savedReturn = returnRepository.save(saleReturn);

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