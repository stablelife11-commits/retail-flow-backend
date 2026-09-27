package com.retail.flow.customer.service;

import com.retail.flow.customer.dto.CustomerRequestDto;
import com.retail.flow.customer.dto.CustomerResponseDto;
import com.retail.flow.customer.entity.Customer;
import com.retail.flow.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponseDto createCustomer(CustomerRequestDto requestDto) {
        if (customerRepository.existsByMobile(requestDto.getMobile())) {
            throw new RuntimeException("Customer with this mobile number already exists.");
        }

        Customer customer = Customer.builder()
                .name(requestDto.getName())
                .mobile(requestDto.getMobile())
                .email(requestDto.getEmail())
                .active(true)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponseDto(savedCustomer);
    }

    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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