package com.retail.flow.customer.service;

import com.retail.flow.common.exception.BusinessValidationException;
import com.retail.flow.common.exception.ResourceNotFoundException;
import com.retail.flow.customer.dto.CustomerRequestDto;
import com.retail.flow.customer.dto.CustomerResponseDto;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import com.retail.flow.user.entity.User;
import com.retail.flow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional
    public CustomerResponseDto createCustomer(CustomerRequestDto requestDto, String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        String trimmedMobile = requestDto.getMobile().trim();
        String normalizedEmail = StringUtils.hasText(requestDto.getEmail())
                ? requestDto.getEmail().trim().toLowerCase()
                : null;

        if (customerRepository.existsBySellerIdAndMobile(caller.getId(), trimmedMobile)) {
            throw new BusinessValidationException("A customer with this mobile number already exists in your directory.");
        }

        if (normalizedEmail != null && customerRepository.existsBySellerIdAndEmail(caller.getId(), normalizedEmail)) {
            throw new BusinessValidationException("A customer with this email already exists in your directory.");
        }

        Customer customer = Customer.builder()
                .sellerId(caller.getId())
                .name(requestDto.getName().trim())
                .mobile(trimmedMobile)
                .email(normalizedEmail)
                .active(true)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponseDto(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers(String callerEmail) {
        User caller = getUserByEmail(callerEmail);

        List<Customer> customers = (caller.getRole() == User.Role.ADMIN)
                ? customerRepository.findAll()
                : customerRepository.findCustomersForSeller(caller.getId());

        return customers.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private CustomerResponseDto mapToResponseDto(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .mobile(customer.getMobile())
                .email(customer.getEmail())
                .active(customer.getActive())
                .build();
    }
}