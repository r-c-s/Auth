package rcs.auth.models.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {

    private String username;
    private Set<String> roles;
}
