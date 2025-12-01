package com.greenhouse.controllers.managers;

import jakarta.ejb.EJBException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import com.greenhouse.entities.Identity;
import com.greenhouse.controllers.Role;
import com.greenhouse.controllers.repositories.IdentityRepository;
import com.greenhouse.security.Argon2Utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

@ApplicationScoped
public class IdentityManager {
    private static final Logger LOGGER = Logger.getLogger(IdentityManager.class.getName());

    @Inject
    private IdentityRepository identityRepository;

    @Inject
    private Argon2Utils argon2Utils;

    @Inject
    private EmailManager emailManager;

    private static final String ACTIVATION_EMAIL_SENDER = "marwenbellili72@gmail.com";
    private static final String ACTIVATION_EMAIL_SUBJECT = "Activate Account";
    private static final String ACTIVATION_EMAIL_TEMPLATE = "Dear User,\n\n" +
            "Thank you for choosing our Smart Green House Monitoring System! We are excited to have you onboard.\n\n" +
            "To complete your account setup, please use the activation code:\n\n" +
            "Activation Code: %s\n\n" +
            "⚠️ Please note: This code is valid for the next 5 minutes.\n\n" +
            "If you did not request this activation or need assistance, please contact our support team immediately.\n\n"
            +
            "Best regards,\n" +
            "The Smart Green House Monitoring System Team";

    private static final int ACTIVATION_CODE_LENGTH = 6;
    private static final int ACTIVATION_CODE_EXPIRATION_MINUTES = 5;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final Map<String, Pair<String, LocalDateTime>> activationCodes = new HashMap<>();

    public void registerIdentity(String username, String password, String email, String activationBaseUrl) {
        validateIdentity(username, email);
        validatePassword(password);
        LOGGER.info("Password registration side: " + password);
        Identity identity = createNewIdentity(username, password, email);
        identityRepository.save(identity);
        String activationCode = generateActivationCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(ACTIVATION_CODE_EXPIRATION_MINUTES);
        activationCodes.put(activationCode, Pair.of(identity.getEmail(), expirationTime));
        String message = String.format(ACTIVATION_EMAIL_TEMPLATE, activationCode);
        emailManager.sendEmail(ACTIVATION_EMAIL_SENDER, identity.getEmail(), ACTIVATION_EMAIL_SUBJECT, message);
    }

    public void activateIdentity(String code) {
        Pair<String, LocalDateTime> codeDetails = activationCodes.get(code);

        if (codeDetails == null) {
            throw new EJBException("Invalid activation code.");
        }
        String email = codeDetails.getLeft();
        LOGGER.info("Activated Identity: " + email);
        LocalDateTime expirationTime = codeDetails.getRight();
        if (LocalDateTime.now().isAfter(expirationTime)) {
            activationCodes.remove(code);
            deleteIdentityByEmail(email);
            throw new EJBException("Activation code expired.");
        }
        Identity identity = identityRepository.findByEmail(email)
                .orElseThrow(() -> new EJBException("Identity associated with the activation code not found."));
        identity.setAccountActivated(true);
        identityRepository.save(identity);
        activationCodes.remove(code);
    }

    private void validateIdentity(String username, String email) {
        if (identityRepository.findByUsername(username).isPresent()) {
            throw new EJBException("An identity with username '" + username + "' already exists.");
        }
        if (identityRepository.findByEmail(email).isPresent()) {
            throw new EJBException("An identity with email '" + email + "' already exists.");
        }
        if (username == null || username.isEmpty()) {
            throw new EJBException("Username is required.");
        }
        if (email == null || email.isEmpty()) {
            throw new EJBException("Email is required.");
        }
    }

    private Identity createNewIdentity(String username, String password, String email) {
        Identity identity = new Identity();
        identity.setUsername(username);
        identity.setPassword(password);
        identity.setEmail(email);
        identity.setCreationDate(LocalDateTime.now().toString());
        identity.setRoles(Role.R_P00.getValue());
        identity.setScopes("resource:read resource:write");
        identity.hashPassword(password, argon2Utils);
        // LOGGER.info("Created new identity with ID: " + identity.getId());
        return identity;
    }

    private void deleteIdentityByEmail(String email) {
        identityRepository.findByEmail(email).ifPresent(identityRepository::delete);
    }

    private String generateActivationCode() {
        String characters = "0123456789";
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(ACTIVATION_CODE_LENGTH);

        for (int i = 0; i < ACTIVATION_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

    // Get Identity by ID
    public Identity getIdentityById(Long id) {
        return identityRepository.findById(id.toString())
                .orElseThrow(() -> new EJBException("Identity not found with ID: " + id));
    }

    public Identity updateIdentity(Long id, String username, String email, String newPassword, String currentPassword) {
        // Step 1: Get the current identity by ID
        Identity identity = identityRepository.findById(id.toString())
                .orElseThrow(() -> new EJBException("Identity not found with ID: " + id));

        // Step 2: Verify if the provided current password matches the stored password
        if (!argon2Utils.check(identity.getPassword(), currentPassword.toCharArray())) {
            throw new EJBException("Current password is incorrect.");
        }
        // Step 3: Update the identity details
        identity.setUsername(username);
        identity.setEmail(email);

        // Step 4: If new password is provided, hash and update it
        if (newPassword != null && !newPassword.isEmpty()) {
            identity.hashPassword(newPassword, argon2Utils);
        }

        // Step 5: Save the updated identity
        identityRepository.save(identity);

        return identity;
    }

    // Delete Identity by ID
    public void deleteIdentityById(Long id) {
        Identity identity = identityRepository.findById(id.toString())
                .orElseThrow(() -> new EJBException("Identity not found with ID: " + id));
        identityRepository.delete(identity);
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new EJBException("Password is required.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new EJBException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (!password.matches(".*\\d.*")) { // At least one digit
            throw new EJBException("Password must contain at least one number.");
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) { // At least one special character
            throw new EJBException("Password must contain at least one special character.");
        }
    }

}
