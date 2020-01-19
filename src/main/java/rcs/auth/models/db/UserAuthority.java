package rcs.auth.models.db;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public enum UserAuthority {
    USER("USER"),
    ADMIN("USER", "ADMIN");

    private Set<String> roles;

    UserAuthority(String... roles) {
        this.roles = ImmutableSet.copyOf(roles);
    }

    public Set<String> getRoles() {
        return roles;
    }
}
