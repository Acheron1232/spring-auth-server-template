package com.acheron.authserver.repository;

import com.acheron.authserver.entity.FederatedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FederatedIdentityRepository extends JpaRepository<FederatedIdentity, UUID> {
}
