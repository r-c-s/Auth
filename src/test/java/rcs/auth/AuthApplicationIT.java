package rcs.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import rcs.auth.models.api.AuthenticatedUser;
import rcs.auth.models.api.UpdateAuthorityRequest;
import rcs.auth.models.api.UpdatePasswordRequest;
import rcs.auth.models.api.UserRegistrationRequest;
import rcs.auth.models.db.UserAuthority;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("file:${app.properties}")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AuthApplicationIT {

    @Getter
    @AllArgsConstructor
    private static final class LoginCredentials {
        private String username;
        private String password;
    }

    private TestRestTemplate restTemplate = new TestRestTemplate();
    private LoginCredentials admin = new LoginCredentials("testAdmin", "password");
    private LoginCredentials userA = new LoginCredentials("usernameA", "passwordA");
    private LoginCredentials userB = new LoginCredentials("usernameB", "passwordB");

    @Value("${service.baseUrl}")
    private String baseUrl;

    @Before
    public void cleanup() {
        deleteUserRequest(admin, userA.getUsername());
        deleteUserRequest(admin, userB.getUsername());
    }

    @Rule
    public TestRule watchman = new TestWatcher() {
        // unlike @After, this also runs when exceptions are thrown inside test methods
        @Override
        protected void finished(Description ignored) {
            cleanup();
        }
    };

    @Test
    public void testGetLoggedInUser() {
        // Arrange
        createUserRequest(userA);

        // Act
        ResponseEntity<AuthenticatedUser> actual = getLoggedInUserRequest(userA);

        // Assert
        assertThat(actual.getBody().getUsername()).isEqualTo(userA.getUsername());
    }

    @Test
    public void testCreateUser() {
        // Arrange

        // Act
        ResponseEntity<Void> response = createUserRequest(userA);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(login(userA)).isNotNull();
    }

    @Test
    public void testUpdateOwnPassword() {
        // Arrange
        createUserRequest(userA);
        String newPassword = "newPassword";

        // Act
        ResponseEntity<Void> response = updatePasswordRequest(userA, userA.getUsername(), newPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // old password no longer works
        assertThat(login(userA)).isNull();

        // new password works
        assertThat(login(new LoginCredentials(userA.getUsername(), newPassword))).isNotNull();
    }

    @Test
    public void testUpdateOthersPassword() {
        // Arrange
        createUserRequest(userA);
        createUserRequest(userB);
        String newPassword = "newPassword";

        // Act
        ResponseEntity<Void> response = updatePasswordRequest(userA, userB.getUsername(), newPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // potential hacking victim's password hasn't changed
        assertThat(login(userB)).isNotNull();
    }

    @Test
    public void testUpdatePasswordRequesterIsAdmin() {
        // Arrange
        createUserRequest(userA);
        String newPassword = "newPassword";

        // Act
        ResponseEntity<Void> response = updatePasswordRequest(admin, userA.getUsername(), newPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // old password no longer works
        assertThat(login(userA)).isNull();

        // new password works
        assertThat(login(new LoginCredentials(userA.getUsername(), newPassword))).isNotNull();
    }

    @Test
    public void testUpdateAuthorityRequestIsUser() {
        // Arrange
        createUserRequest(userA);
        UserAuthority newAuthority = UserAuthority.ADMIN;

        // Act
        ResponseEntity<Void> response = updateAuthorityRequest(userA, userA.getUsername(), newAuthority);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // authority hasn't changed
        assertThat(getLoggedInUserRequest(userA).getBody().getRoles())
                .hasSameElementsAs(UserAuthority.USER.getRoles());
    }

    @Test
    public void testUpdateAuthorityRequesterIsAdmin() {
        // Arrange
        createUserRequest(userA);
        UserAuthority newAuthority = UserAuthority.ADMIN;

        // Act
        ResponseEntity<Void> response = updateAuthorityRequest(admin, userA.getUsername(), newAuthority);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(getLoggedInUserRequest(userA).getBody().getRoles())
                .isEqualTo(UserAuthority.ADMIN.getRoles());
    }

    @Test
    public void testDeleteUserRequesterIsAdmin() {
        // Arrange
        createUserRequest(userA);

        // Act
        ResponseEntity<Void> response = deleteUserRequest(admin, userA.getUsername());

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(getLoggedInUserRequest(userA).getStatusCodeValue()).isEqualTo(401);
    }

    @Test
    public void testDeleteUserRequesterIsNotAdmin() {
        // Arrange
        createUserRequest(userA);

        // Act
        ResponseEntity<Void> response = deleteUserRequest(userA, userA.getUsername());

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // user has not been deleted
        assertThat(getLoggedInUserRequest(userA)).isNotNull();
    }

    private ResponseEntity<AuthenticatedUser> getLoggedInUserRequest(LoginCredentials creds) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(creds);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/api/authenticate"),
                HttpMethod.GET,
                request,
                AuthenticatedUser.class);
    }

    private ResponseEntity<Void> createUserRequest(LoginCredentials creds) {
        UserRegistrationRequest payload = new UserRegistrationRequest(creds.getUsername(), creds.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserRegistrationRequest> request = new HttpEntity<>(payload, headers);

        return restTemplate.postForEntity(
                createUrl("/api/users"),
                request,
                Void.class);
    }

    private ResponseEntity<Void> deleteUserRequest(LoginCredentials creds, String usernameToDelete) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(creds);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/api/users/" + usernameToDelete),
                HttpMethod.DELETE,
                request,
                Void.class);
    }

    private ResponseEntity<Void> updatePasswordRequest(LoginCredentials creds, String usernameToUpdate, String newPassword) {
        UpdatePasswordRequest payload = new UpdatePasswordRequest(newPassword);

        HttpHeaders headers = new HttpHeaders();
        String authToken = login(creds);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(
                createUrl("/api/users/" + usernameToUpdate + "/password"),
                HttpMethod.PUT,
                request,
                Void.class);
    }

    private ResponseEntity<Void> updateAuthorityRequest(
            LoginCredentials creds,
            String usernameToUpdate,
            UserAuthority newAuthority) {

        UpdateAuthorityRequest payload = new UpdateAuthorityRequest(newAuthority);

        HttpHeaders headers = new HttpHeaders();
        String authToken = login(creds);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(
                createUrl("/api/users/" + usernameToUpdate + "/authority"),
                HttpMethod.PUT,
                request,
                Void.class);
    }

    private String createUrl(String path) {
        return baseUrl + path;
    }

    public String login(LoginCredentials creds) {
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
                .map(setCookieHeader -> getCookieValue("JSESSIONID", setCookieHeader))
                .orElse(null);
    }

    private String getCookieValue(String cookieName, String setCookieHeader) {
        Pattern pattern = Pattern.compile(cookieName + "=(.*?);");
        Matcher matcher = pattern.matcher(setCookieHeader);
        return matcher.find() ? matcher.group(1) : null;
    }
}
