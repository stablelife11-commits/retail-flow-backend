package com.retail.flow.user.repository;

import com.retail.flow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(User.Role role);

    // सक्रिय Admins को ढूँढने और पुराने टोकन साफ करने के लिए
    List<User> findByRoleAndActiveTrue(User.Role role);
    List<User> findByFcmToken(String fcmToken);
}