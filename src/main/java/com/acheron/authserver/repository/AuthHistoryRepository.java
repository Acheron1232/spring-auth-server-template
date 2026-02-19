package com.acheron.authserver.repository;

import com.acheron.authserver.entity.AuthHistory;
import com.acheron.authserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthHistoryRepository extends JpaRepository<AuthHistory, UUID> {
    List<AuthHistory> findTop10ByUserOrderByTimestampDesc(User user);
}
