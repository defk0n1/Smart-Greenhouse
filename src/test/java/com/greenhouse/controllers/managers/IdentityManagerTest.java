package com.greenhouse.controllers.managers;

import com.greenhouse.controllers.repositories.IdentityRepository;
import com.greenhouse.entities.Identity;
import jakarta.ejb.EJBException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityManagerTest {

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private EmailManager emailManager;

    @InjectMocks
    private IdentityManager identityManager;

    @Test
    void registerIdentity_Success() {
        // Arrange
        String username = "testuser";
        String password = "Password123!";
        String email = "test@example.com";
        String activationBaseUrl = "http://localhost:8080/activate";

        when(identityRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(identityRepository.findByEmail(email)).thenReturn(Optional.empty());
        doNothing().when(emailManager).sendEmail(anyString(), anyString(), anyString(), anyString());

        // Act
        identityManager.registerIdentity(username, password, email, activationBaseUrl);

        // Assert
        verify(identityRepository).save(any(Identity.class));
        verify(emailManager).sendEmail(anyString(), eq(email), anyString(), anyString());
        // Note: Argon2Utils uses static methods, so we don't mock or verify it
    }

    @Test
    void registerIdentity_DuplicateUsername() {
        // Arrange
        String username = "existinguser";
        String password = "Password123!";
        String email = "test@example.com";

        when(identityRepository.findByUsername(username)).thenReturn(Optional.of(new Identity()));

        // Act & Assert
        assertThrows(EJBException.class, () -> identityManager.registerIdentity(username, password, email, "url"));
        verify(identityRepository, never()).save(any());
    }

    @Test
    void registerIdentity_DuplicateEmail() {
        // Arrange
        String username = "newuser";
        String password = "Password123!";
        String email = "existing@example.com";

        when(identityRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(new Identity()));

        // Act & Assert
        assertThrows(EJBException.class, () -> identityManager.registerIdentity(username, password, email, "url"));
        verify(identityRepository, never()).save(any());
    }

    @Test
    void activateIdentity_Success() throws Exception {
        // Arrange
        String code = "123456";
        String email = "test@example.com";
        Identity identity = new Identity();
        identity.setEmail(email);
        identity.setAccountActivated(false);

        // Inject activation code into the private map using reflection
        Field activationCodesField = IdentityManager.class.getDeclaredField("activationCodes");
        activationCodesField.setAccessible(true);
        Map<String, Pair<String, LocalDateTime>> activationCodes = (Map<String, Pair<String, LocalDateTime>>) activationCodesField
                .get(identityManager);

        activationCodes.put(code, Pair.of(email, LocalDateTime.now().plusMinutes(5)));

        when(identityRepository.findByEmail(email)).thenReturn(Optional.of(identity));

        // Act
        identityManager.activateIdentity(code);

        // Assert
        assertTrue(identity.isAccountActivated());
        verify(identityRepository).save(identity);
        assertFalse(activationCodes.containsKey(code));
    }

    @Test
    void activateIdentity_InvalidCode() {
        // Act & Assert
        assertThrows(EJBException.class, () -> identityManager.activateIdentity("invalid"));
    }
}
