package com.retail.flow.order.repository;

import com.retail.flow.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 🟢 नया: सेलर की ID के आधार पर सारे ऑर्डर्स लाने के लिए
    List<Order> findBySellerId(Long sellerId);
}