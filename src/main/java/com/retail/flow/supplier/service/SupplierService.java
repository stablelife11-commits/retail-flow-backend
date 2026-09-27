package com.retail.flow.supplier.service;

import com.retail.flow.supplier.dto.SupplierRequestDto;
import com.retail.flow.supplier.dto.SupplierResponseDto;
import com.retail.flow.supplier.entity.Supplier;
import com.retail.flow.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierResponseDto createSupplier(SupplierRequestDto requestDto) {
        Supplier supplier = Supplier.builder()
                .name(requestDto.getName())
                .contactPerson(requestDto.getContactPerson())
                .phone(requestDto.getPhone())
                .email(requestDto.getEmail())
                .address(requestDto.getAddress())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponseDto(saved);
    }

    public List<SupplierResponseDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private SupplierResponseDto mapToResponseDto(Supplier supplier) {
        return SupplierResponseDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .build();
    }
}