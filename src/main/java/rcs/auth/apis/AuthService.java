package rcs.auth.apis;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import rcs.auth.models.api.AuthenticatedUser;
import rcs.auth.models.api.LoginCredentials;
import rcs.auth.models.api.UpdateAuthorityRequest;
import rcs.auth.models.api.UpdatePasswordRequest;
import rcs.auth.models.db.UserAuthority;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthService {

    public static final String authTokenName = "JSESSIONID";

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public AuthService(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    public Optional<String> login(LoginCredentials creds) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.set("username", creds.getUsername());
        params.set("password", creds.getPassword());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(params, new HttpHeaders());

        ResponseEntity<Void> response = restTemplate.exchange(
                createUrl("/login"),
                HttpMethod.POST,
                entity,
                Void.class);

        return Optional.ofNullable(response.getHeaders().get("Set-Cookie"))
                .map(cookies -> cookies.get(0))
                .map(setCookieHeader -> getCookieValue(authTokenName, setCookieHeader));
    }

    public ResponseEntity<AuthenticatedUser> authenticate(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", authTokenName + "=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/api/authenticate"),
                HttpMethod.GET,
                request,
                AuthenticatedUser.class);
    }

    public ResponseEntity<AuthenticatedUser> authenticate(LoginCredentials creds) {
        return login(creds)
                .map(this::authenticate)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public ResponseEntity<Void> register(LoginCredentials creds) {
        Map<String, String> payload = ImmutableMap.of(
                "username", creds.getUsername(),
                "password", creds.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(
                createUrl("/api/users"),
                HttpMethod.POST,
                request,
                Void.class);
    }

    public ResponseEntity<Void> delete(LoginCredentials creds, String usernameToDelete) {
        return login(creds)
                .map(authToken -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", "JSESSIONID=" + authToken);

                    HttpEntity<Map<String, String>> request = new HttpEntity<>(null, headers);

                    return restTemplate.exchange(
                            createUrl("/api/users/" + usernameToDelete),
                            HttpMethod.DELETE,
                            request,
                            Void.class);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public ResponseEntity<Void> updateAuthority(
            LoginCredentials creds,
            String usernameToUpdate,
            UserAuthority newAuthority) {
        return login(creds)
                .map(authToken -> {
                    UpdateAuthorityRequest payload = new UpdateAuthorityRequest(newAuthority);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", authTokenName + "=" + authToken);

                    HttpEntity<Object> request = new HttpEntity<>(payload, headers);

                    return restTemplate.exchange(
                            createUrl("/api/users/" + usernameToUpdate + "/authority"),
                            HttpMethod.PUT,
                            request,
                            Void.class);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public ResponseEntity<Void> updatePassword(LoginCredentials creds, String usernameToUpdate, String newPassword) {
        return login(creds)
                .map(authToken -> {
                    UpdatePasswordRequest payload = new UpdatePasswordRequest(newPassword);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", authTokenName + "=" + authToken);

                    HttpEntity<Object> request = new HttpEntity<>(payload, headers);

                    return restTemplate.exchange(
                            createUrl("/api/users/" + usernameToUpdate + "/password"),
                            HttpMethod.PUT,
                            request,
                            Void.class);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private String getCookieValue(String cookieName, String setCookieHeader) {
        Pattern pattern = Pattern.compile(cookieName + "=(.*?);");
        Matcher matcher = pattern.matcher(setCookieHeader);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String createUrl(String uri) {
        return baseUrl + uri;
    }
}
