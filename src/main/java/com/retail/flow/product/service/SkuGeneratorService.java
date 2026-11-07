package com.retail.flow.product.service;

import com.retail.flow.product.repository.ProductRepository;
import com.retail.flow.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SkuGeneratorService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    /**
     * Auto-generates Seller-scoped unique Product Code if left blank (e.g., "LUC-MEV-VEN")
     */
    public String generateUniqueProductCode(Long sellerId, String brand, String category, String productName) {
        String brandCode = smartCode(brand, "GEN");
        String catCode   = smartCode(category, "APP");
        String prodCode  = smartCode(productName, "ITM");

        String baseCode = String.format("%s-%s-%s", brandCode, catCode, prodCode);
        String finalCode = baseCode;
        int counter = 1;

        while (productRepository.findBySellerIdAndProductCode(sellerId, finalCode).isPresent()) {
            finalCode = baseCode + "-" + counter;
            counter++;
        }
        return finalCode;
    }

    /**
     * Auto-generates Seller-scoped unique Variant SKU (e.g., "LUC-MEV-VEN-WHI-85")
     */
    public String generateBestWholesaleSku(
            Long sellerId,
            String brand,
            String category,
            String productName,
            String color,
            String size,
            Set<String> allocatedInCurrentBatch) {

        String brandCode = smartCode(brand, "GEN");       // "Lux Cozi" -> "LUC"
        String catCode   = smartCode(category, "APP");    // "Men Vest" -> "MEV"
        String prodCode  = smartCode(productName, "ITM"); // "Venus"    -> "VEN"
        String colorCode = smartCode(color, "STD");       // "White"    -> "WHI"
        String sizeCode  = normalizeSize(size);           // "85 cm"    -> "85"

        String baseSku = String.format("%s-%s-%s-%s-%s",
                brandCode, catCode, prodCode, colorCode, sizeCode);

        String finalSku = baseSku;
        int counter = 1;

        while (allocatedInCurrentBatch.contains(finalSku.toUpperCase())
                || variantRepository.existsBySellerIdAndSku(sellerId, finalSku)) {
            finalSku = baseSku + "-" + counter;
            counter++;
        }

        allocatedInCurrentBatch.add(finalSku.toUpperCase());
        return finalSku;
    }

    /**
     * 2 words ("Lux Cozi") -> "LUC" (2 chars from 1st + 1 char from 2nd)
     * 1 word  ("Rupa")     -> "RUP" (first 3 chars)
     */
    public String smartCode(String input, String fallback) {
        if (input == null || input.trim().isEmpty()) {
            return fallback;
        }
        String[] words = input.trim().toUpperCase().split("\\s+");

        if (words.length >= 2) {
            String w1 = words[0].replaceAll("[^A-Z0-9]", "");
            String w2 = words[1].replaceAll("[^A-Z0-9]", "");
            if (w1.length() >= 2 && w2.length() >= 1) {
                return w1.substring(0, 2) + w2.substring(0, 1);
            }
        }

        String cleaned = input.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned.length() >= 3 ? cleaned.substring(0, 3) : cleaned;
    }

    /**
     * Normalizes garment sizes: "85cm", "85 CM", "85-cm" -> "85"
     */
    public String normalizeSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            return "FS";
        }
        String s = size.toUpperCase().trim()
                .replaceAll("CM|CMS|INCH|INCHES|\\s+", "")
                .replaceAll("[^A-Z0-9]", "");
        return s.isEmpty() ? "FS" : s;
    }
}