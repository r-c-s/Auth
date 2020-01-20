package rcs.auth.apis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import rcs.auth.models.api.AuthenticatedUser;
import rcs.auth.models.api.UpdateAuthorityRequest;
import rcs.auth.models.api.UpdatePasswordRequest;
import rcs.auth.models.api.UserRegistrationRequest;
import rcs.auth.services.UserCredentialsService;
import rcs.auth.utils.AuthUtils;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthApi {

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private UserCredentialsService userCredentialsService;

    @GetMapping("/authenticate")
    public ResponseEntity<AuthenticatedUser> getLoggedInUser() {
        return authUtils.tryGetLoggedInUser()
                .map(user -> new AuthenticatedUser(
                            user.getUsername(),
                            user.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toSet())))
                .map(authenticatedUser -> ResponseEntity.ok().body(authenticatedUser))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/users")
    public ResponseEntity<Void> createUser(@RequestBody UserRegistrationRequest request) {
        userCredentialsService.save(request);
        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/users/{username}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable String username,
            @RequestBody UpdatePasswordRequest request) {
        userCredentialsService.updatePassword(username, request.getPassword());
        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/users/{username}/authority")
    public ResponseEntity<Void> updateAuthority(
            @PathVariable String username,
            @RequestBody UpdateAuthorityRequest request) {
        userCredentialsService.updateAuthority(username, request.getAuthority());
        return ResponseEntity.ok()
                .build();
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userCredentialsService.delete(username);
        return ResponseEntity.ok()
                .build();
    }
}
