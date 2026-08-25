package com.marcptr.cine.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.marcptr.cine.model.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findByConfigKey(String key);

    boolean existsByConfigKey(String key);
}