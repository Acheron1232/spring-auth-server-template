package com.acheron.authserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE mail SET deleted_at = now() WHERE id=?")
@SQLRestriction("deleted_at is NULL")
public class Mail extends AbstractAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "to_email")
    private String to;
    @Column(name = "from_email")
    private String from;
    private String subject;
    private String content;
    @Column(name = "user_id")
    private Long userId;
}
