package com.securestorage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUserGlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final FileService fileService;
    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    public AccountController(FileService fileService, CognitoIdentityProviderClient cognitoClient) {
        this.fileService = fileService;
        this.cognitoClient = cognitoClient;
    }

    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal Jwt principal) {
        try {
            String userId = principal.getSubject();
            // Purge user data (files + profile photo)
            fileService.deleteAllForUser(userId);

                // Invalidate refresh tokens (global sign-out), then delete the Cognito user
            try {
                AdminUserGlobalSignOutRequest signOutReq = AdminUserGlobalSignOutRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userId)
                    .build();
                cognitoClient.adminUserGlobalSignOut(signOutReq);

                AdminDeleteUserRequest request = AdminDeleteUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(userId)
                        .build();
                cognitoClient.adminDeleteUser(request);
                return ResponseEntity.accepted().body("Account deletion initiated: data purged and Cognito user removed.");
            } catch (CognitoIdentityProviderException e) {
                String msg = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
                return ResponseEntity.accepted().body("Data purged, but Cognito deletion failed: " + msg);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting account: " + e.getMessage());
        }
    }
}
