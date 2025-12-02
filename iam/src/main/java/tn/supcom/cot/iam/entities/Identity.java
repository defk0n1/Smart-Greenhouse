package tn.supcom.cot.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.security.Principal;
import java.time.LocalDateTime;

@Entity("Identity")
public class Identity implements RootEntity<String>, Principal {

    @Id
    private String id;

    @Column
    private long version = 0L;

    @Column
    private String username;

    @Column
    private String password;

    @Column
    private Long roles;

    @Column
    private String providedScopes;

    @Column
    private boolean active = true;

    @Column
    private String email;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastModifiedAt;

    public Identity() {
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.lastModifiedAt = LocalDateTime.now();
    }

    @Override
    public String getName() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public Long getRoles() {
        return roles;
    }

    public void setRoles(Long roles) {
        this.roles = roles;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getProvidedScopes() {
        return providedScopes;
    }

    public void setProvidedScopes(String providedScopes) {
        this.providedScopes = providedScopes;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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