package rcs.auth.models.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import rcs.auth.models.db.UserAuthority;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAuthorityRequest {

    private UserAuthority authority;
}
