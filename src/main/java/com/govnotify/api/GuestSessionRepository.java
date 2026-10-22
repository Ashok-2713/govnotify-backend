package com.govnotify.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {
    List<GuestSession> findAllByOrderByLoginTimeDesc();
}
