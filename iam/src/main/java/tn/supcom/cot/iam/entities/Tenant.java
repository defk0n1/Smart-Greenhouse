package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.time.LocalDateTime;

@Entity("Tenant")
public class Tenant implements RootEntity<String> {

    @Id
    private String id;

    @Column
    private long version = 0L;

    @Column
    private String name;

    @Column
    private String secret;

    @Column
    private String redirectUri;

    @Column
    private Long allowedRoles;

    @Column
    private String requiredScopes;

    @Column
    private String supportedGrantTypes;

    @Column
    private boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastModifiedAt;

    public Tenant() {
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        if (this.version != version) {
            throw new IllegalStateException("Optimistic locking violation");
        }
        ++this.version;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public Long getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(Long allowedRoles) {
        this.allowedRoles = allowedRoles;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getRequiredScopes() {
        return requiredScopes;
    }

    public void setRequiredScopes(String requiredScopes) {
        this.requiredScopes = requiredScopes;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    public void setSupportedGrantTypes(String supportedGrantTypes) {
        this.supportedGrantTypes = supportedGrantTypes;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}