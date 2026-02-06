package com.acheron.authserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "token")
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id=?")
@SQLRestriction("deleted_at is NULL")
public class Token extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false,
            name = "user_id")
    private User user;

    @Column(name = "expired_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant expiredAt;

    @Column(nullable = false, name = "token_status")
    @Enumerated(EnumType.STRING)
    private TokenStatus tokenStatus;

    @Column(nullable = false, name = "token_type")
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    public enum TokenType {
        RESET, CONFIRM
    }

    public enum TokenStatus {
        ACTIVE, INACTIVE
    }
}


