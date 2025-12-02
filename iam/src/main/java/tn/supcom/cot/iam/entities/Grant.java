package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.time.LocalDateTime;

@Entity("Grant")
public class Grant implements RootEntity<String> {

    @Id
    private String id;  // Composite: tenantId::identityId

    @Column
    private long version = 0L;

    @Column
    private String tenantId;

    @Column
    private String identityId;

    @Column
    private String approvedScopes;

    @Column
    private LocalDateTime issuanceDateTime;

    @Column
    private LocalDateTime expirationDateTime;

    @Column
    private boolean active = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastModifiedAt;

    public Grant() {
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.issuanceDateTime = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
        this.lastModifiedAt = LocalDateTime.now();
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        if (this.version != version) {
            throw new IllegalStateException("Optimistic locking violation");
        }
        ++this.version;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void generateCompositeId() {
        if (tenantId != null && identityId != null) {
            this.id = tenantId + "::" + identityId;
        }
    }

    public String getApprovedScopes() {
        return approvedScopes;
    }

    public void setApprovedScopes(String approvedScopes) {
        this.approvedScopes = approvedScopes;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public LocalDateTime getIssuanceDateTime() {
        return issuanceDateTime;
    }

    public void setIssuanceDateTime(LocalDateTime issuanceDateTime) {
        this.issuanceDateTime = issuanceDateTime;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public LocalDateTime getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(LocalDateTime expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
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

    public boolean isExpired() {
        return expirationDateTime != null && LocalDateTime.now().isAfter(expirationDateTime);
    }
}