package com.acheron.authserver.repository;

import com.acheron.authserver.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MailRepository extends JpaRepository<Mail, UUID> {
}
