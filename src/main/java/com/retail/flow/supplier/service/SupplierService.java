package com.retail.flow.supplier.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.supplier.dto.SupplierRequestDto;
import com.retail.flow.supplier.dto.SupplierResponseDto;
import com.retail.flow.supplier.entity.Supplier;
import com.retail.flow.supplier.repository.SupplierRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupplierResponseDto createSupplier(SupplierRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        if (supplierRepository.existsBySellerIdAndPhone(caller.getId(), requestDto.getPhone())) {
            throw new BusinessValidationException("A supplier with this phone number already exists in your directory.");
        }

        Supplier supplier = Supplier.builder()
                .sellerId(caller.getId())
                .name(requestDto.getName())
                .contactPerson(requestDto.getContactPerson())
                .phone(requestDto.getPhone())
                .email(requestDto.getEmail())
                .address(requestDto.getAddress())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getAllSuppliers(String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        List<Supplier> suppliers = (caller.getRole() == User.Role.ADMIN)
                ? supplierRepository.findAll()
                : supplierRepository.findBySellerId(caller.getId());

        return suppliers.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
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