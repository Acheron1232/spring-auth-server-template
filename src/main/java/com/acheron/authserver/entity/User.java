package com.acheron.authserver.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id=?")
@SQLRestriction("deleted_at is NULL")
@Table(name = "users")
@Entity
public class User extends AbstractAuditableEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String email;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String username;

    // Optional: NULL for OAuth-only users
    @Column(name = "password_hash")
    @JsonIgnore
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private UUID tokenVersion = UUID.randomUUID();

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private Set<FederatedIdentity> federatedIdentities = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(role);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Helper methods
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    public boolean hasFederatedIdentity(OAuthProvider provider) {
        return federatedIdentities.stream()
                .anyMatch(fi -> fi.getProvider() == provider);
    }

    public Optional<FederatedIdentity> getFederatedIdentity(OAuthProvider provider) {
        return federatedIdentities.stream()
                .filter(fi -> fi.getProvider() == provider)
                .findFirst();
    }
}