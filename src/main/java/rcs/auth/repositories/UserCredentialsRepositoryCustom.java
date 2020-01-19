package rcs.auth.repositories;

import rcs.auth.models.db.UserAuthority;

public interface UserCredentialsRepositoryCustom {

    boolean updatePassword(String username, String encodedPassword);
    boolean updateAuthority(String username, UserAuthority authority);
}
