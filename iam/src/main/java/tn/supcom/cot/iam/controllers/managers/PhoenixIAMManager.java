package tn.supcom.cot.iam.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.supcom.cot.iam.controllers.Role;
import tn.supcom.cot.iam.controllers.repositories.GrantRepository;
import tn.supcom.cot.iam.controllers.repositories.IdentityRepository;
import tn.supcom.cot.iam.controllers.repositories.TenantRepository;
import tn.supcom.cot.iam.entities.Grant;
import tn.supcom.cot.iam.entities.GrantPK;
import tn.supcom.cot.iam.entities.Identity;
import tn.supcom.cot.iam.entities.Tenant;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PhoenixIAMManager {

    @Inject
    private IdentityRepository identityRepository;

    @Inject
    private GrantRepository grantRepository;

    @Inject
    private TenantRepository tenantRepository;

    // Tenant operations
    public Tenant findTenantByName(String name) {
        return tenantRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + name));
    }

    public Tenant createTenant(String name, String secret, String redirectUri) {
        if (tenantRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists: " + name);
        }

        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSecret(secret);
        tenant.setRedirectUri(redirectUri);
        tenant.setActive(true);

        return tenantRepository.save(tenant);
    }

    public List<Tenant> findAllActiveTenants() {
        return tenantRepository.findAll()
                .filter(Tenant::isActive)
                .toList();
    }

    // Identity operations
    public Identity findIdentityByUsername(String username) {
        return identityRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Identity not found: " + username));
    }

    public Identity createIdentity(String username, String password, String email) {
        if (identityRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        Identity identity = new Identity();
        identity.setUsername(username);
        identity.setPassword(password);
        identity.setEmail(email);
        identity.setActive(true);

        return identityRepository.save(identity);
    }

    public Identity authenticate(String username, String password) {
        return identityRepository.findByUsernameAndActiveTrue(username)
                .filter(identity -> identity.getPassword().equals(password))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials or inactive account"));
    }

    public List<Identity> findAllActiveIdentities() {
        return identityRepository.findAll()
                .filter(Identity::isActive)
                .toList();
    }

    // Grant operations
    public Optional<Grant> findGrant(String tenantName, String identityId) {
        Tenant tenant = findTenantByName(tenantName);
        if (tenant == null) {
            throw new IllegalArgumentException("Invalid Client Id!");
        }

        return grantRepository.findByTenantIdAndIdentityId(tenant.getId(), identityId);
    }

    public Grant createGrant(String tenantName, String username, String approvedScopes, int validityHours) {
        Tenant tenant = findTenantByName(tenantName);
        Identity identity = findIdentityByUsername(username);

        Grant grant = new Grant();
        grant.setTenantId(tenant.getId());
        grant.setIdentityId(identity.getId());
        grant.setApprovedScopes(approvedScopes);
        grant.setActive(true);

        if (validityHours > 0) {
            grant.setExpirationDateTime(LocalDateTime.now().plusHours(validityHours));
        }

        grant.generateCompositeId();

        return grantRepository.save(grant);
    }

    public boolean validateGrant(String tenantName, String username) {
        try {
            Tenant tenant = findTenantByName(tenantName);
            Identity identity = findIdentityByUsername(username);

            Optional<Grant> grant = grantRepository.findByTenantIdAndIdentityIdAndActiveTrue(
                    tenant.getId(), identity.getId());

            return grant.map(g -> !g.isExpired()).orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeGrant(String tenantName, String username) {
        Tenant tenant = findTenantByName(tenantName);
        Identity identity = findIdentityByUsername(username);

        grantRepository.findByTenantIdAndIdentityId(tenant.getId(), identity.getId())
                .ifPresent(grant -> {
                    grant.setActive(false);
                    grantRepository.save(grant);
                });
    }

    // Role operations
    public String[] getRoles(String username) {
        var identity = identityRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        var roles = identity.getRoles();
        if (roles == null) {
            return new String[0];
        }

        var ret = new HashSet<String>();
        for (var role : Role.values()) {
            if ((roles & role.getValue()) != 0L) {
                String value = Role.byValue(role.getValue());
                if (value == null) {
                    continue;
                }
                ret.add(value);
            }
        }
        return ret.toArray(new String[0]);
    }

    public void assignRole(String username, Role role) {
        Identity identity = findIdentityByUsername(username);
        Long currentRoles = identity.getRoles() != null ? identity.getRoles() : 0L;
        identity.setRoles(currentRoles | role.getValue());
        identityRepository.save(identity);
    }

    public void removeRole(String username, Role role) {
        Identity identity = findIdentityByUsername(username);
        if (identity.getRoles() != null) {
            identity.setRoles(identity.getRoles() & ~role.getValue());
            identityRepository.save(identity);
        }
    }

    public boolean hasRole(String username, Role role) {
        return identityRepository.findByUsername(username)
                .map(identity -> {
                    if (identity.getRoles() == null) return false;
                    return (identity.getRoles() & role.getValue()) != 0L;
                })
                .orElse(false);
    }
}