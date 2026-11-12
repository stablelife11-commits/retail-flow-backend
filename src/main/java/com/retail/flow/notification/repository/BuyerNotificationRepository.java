package com.retail.flow.notification.repository;

import com.retail.flow.notification.entity.BuyerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuyerNotificationRepository extends JpaRepository<BuyerNotification, Long> {

    List<BuyerNotification> findTop30ByCustomerIdOrderByIdDesc(Long customerId);

    @Modifying
    @Query("UPDATE BuyerNotification n SET n.isRead = true WHERE n.customerId = :customerId AND n.isRead = false")
    void markAllAsReadForCustomer(@Param("customerId") Long customerId);
}