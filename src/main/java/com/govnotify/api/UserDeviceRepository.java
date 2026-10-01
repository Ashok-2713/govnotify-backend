package com.govnotify.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findByUserEmail(String userEmail);

    @Transactional
    @Modifying
    void deleteByUserEmail(String userEmail);
}
