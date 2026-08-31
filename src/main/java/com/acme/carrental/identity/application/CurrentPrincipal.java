package com.acme.carrental.identity.application;

import com.acme.carrental.identity.domain.UserRole;
import com.acme.carrental.shared.error.ApiException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentPrincipal {
    public PrincipalView from(Jwt jwt) {
        String subjectRef = jwt.getClaimAsString("subject_ref");
        return new PrincipalView(
            UUID.fromString(jwt.getSubject()),
            UserRole.valueOf(jwt.getClaimAsString("role")),
            subjectRef == null || subjectRef.isBlank() ? null : UUID.fromString(subjectRef)
        );
    }

    public record PrincipalView(UUID userId, UserRole role, UUID subjectRef) {
        public UUID requireCustomerId() {
            if (role != UserRole.CUSTOMER || subjectRef == null) {
                throw ApiException.forbidden("CUSTOMER_REQUIRED", "Customer identity is required");
            }
            return subjectRef;
        }
    }
}
