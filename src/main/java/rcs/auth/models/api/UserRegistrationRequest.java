package rcs.auth.models.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRegistrationRequest {

    private String username;
    private String password;
}
