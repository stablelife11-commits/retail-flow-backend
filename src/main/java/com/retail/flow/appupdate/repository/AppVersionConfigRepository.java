package com.retail.flow.appupdate.repository;

import com.retail.flow.appupdate.entity.AppVersionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppVersionConfigRepository extends JpaRepository<AppVersionConfig, String> {
}