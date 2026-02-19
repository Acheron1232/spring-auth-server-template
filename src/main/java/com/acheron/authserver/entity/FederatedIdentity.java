package com.acheron.authserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE federated_identity SET deleted_at = now() WHERE id=?")
@SQLRestriction("deleted_at is NULL")
@Entity
@Table(name = "federated_identity")
public class FederatedIdentity extends AbstractAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Include(name = "userId")
    private UUID getUserId() {
        return user != null ? user.getId() : null;
    }


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    @ToString.Include
    private String providerUserId;  // GitHub: "12345", Google: "1098765..."

    @Column(name = "provider_username")
    @ToString.Include
    private String providerUsername; // GitHub login, Google email

    // OAuth tokens (encrypted!)
    @Column(name = "access_token", columnDefinition = "TEXT")
    @JsonIgnore
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    @JsonIgnore
    private String refreshToken;

    @Column(name = "token_expires_at")
    @JsonIgnore
    private Instant tokenExpiresAt;

    // Provider-specific metadata
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonIgnore
    private Map<String, Object> providerMetadata;
}