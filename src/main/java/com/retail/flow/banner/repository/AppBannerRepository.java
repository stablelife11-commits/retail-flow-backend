package com.retail.flow.banner.repository;

import com.retail.flow.banner.entity.AppBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppBannerRepository extends JpaRepository<AppBanner, Long> {

    List<AppBanner> findByBannerTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(String bannerType);

    List<AppBanner> findAllByOrderByDisplayOrderAscIdAsc();
}