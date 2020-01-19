package rcs.auth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${service.baseUrl}")
    private String baseUrl;

    private final String testUserAUsername = "usernameA";
    private final String testUserAPassword = "passwordA";
    private final String testUserBUsername = "usernameB";
    private final String testUserBPassword = "passwordB";

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Before
    @After
    public void cleanup() {
        deleteUserRequest(adminUsername, adminPassword, testUserAUsername);
        deleteUserRequest(adminUsername, adminPassword, testUserBUsername);
    }

    @Test
    public void testGetLoggedInUser() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);

        // Act
        ResponseEntity<AuthenticatedUser> actual = getLoggedInUserRequest(testUserAUsername, testUserAPassword);

        // Assert
        assertThat(actual.getBody().getUsername()).isEqualTo(testUserAUsername);
    }

    @Test
    public void testCreateUser() {
        // Arrange

        // Act
        ResponseEntity<Void> response = createUserRequest(testUserAUsername, testUserAPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(login(testUserAUsername, testUserAPassword)).isNotNull();
    }

    @Test
    public void testUpdateOwnPassword() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);
        String newPassword = "newPassword";

        // Act
        ResponseEntity<Void> response = updatePasswordRequest(testUserAUsername, testUserAPassword, testUserAUsername, newPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // old password no longer works
        assertThat(login(testUserAUsername, testUserAPassword)).isNull();

        // new password works
        assertThat(login(testUserAUsername, newPassword)).isNotNull();
    }

    @Test
    public void testUpdateOthersPassword() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);
        createUserRequest(testUserBUsername, testUserBPassword);
        String newPassword = "newPassword";

        // Act
        ResponseEntity<Void> response = updatePasswordRequest(testUserAUsername, testUserAPassword, testUserBUsername, newPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // potential hacking victim's password hasn't changed
        assertThat(login(testUserBUsername, testUserBPassword)).isNotNull();
    }

    @Test
    public void testUpdatePasswordRequesterIsAdmin() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);
        String newPassword = "newPassword";

        // Act
        ResponseEntity<Void> response = updatePasswordRequest(adminUsername, adminPassword, testUserAUsername, newPassword);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // old password no longer works
        assertThat(login(testUserAUsername, testUserAPassword)).isNull();

        // new password works
        assertThat(login(testUserAUsername, newPassword)).isNotNull();
    }

    @Test
    public void testUpdateAuthorityRequestIsUser() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);
        UserAuthority newAuthority = UserAuthority.ADMIN;

        // Act
        ResponseEntity<Void> response = updateAuthorityRequest(testUserAUsername, testUserAPassword, testUserAUsername, newAuthority);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // authority hasn't changed
        assertThat(getLoggedInUserRequest(testUserAUsername, testUserAPassword).getBody().getRoles())
                .hasSameElementsAs(UserAuthority.USER.getRoles());
    }

    @Test
    public void testUpdateAuthorityRequesterIsAdmin() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);
        UserAuthority newAuthority = UserAuthority.ADMIN;

        // Act
        ResponseEntity<Void> response = updateAuthorityRequest(adminUsername, adminPassword, testUserAUsername, newAuthority);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(getLoggedInUserRequest(testUserAUsername, testUserAPassword).getBody().getRoles())
                .isEqualTo(UserAuthority.ADMIN.getRoles());
    }

    @Test
    public void testDeleteUserRequesterIsAdmin() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);

        // Act
        ResponseEntity<Void> response = deleteUserRequest(adminUsername, adminPassword, testUserAUsername);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(getLoggedInUserRequest(testUserAUsername, testUserAPassword).getStatusCodeValue()).isEqualTo(500);
    }

    @Test
    public void testDeleteUserRequesterIsNotAdmin() {
        // Arrange
        createUserRequest(testUserAUsername, testUserAPassword);

        // Act
        ResponseEntity<Void> response = deleteUserRequest(testUserAUsername, testUserAPassword, testUserAUsername);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(403);

        // user has not been deleted
        assertThat(getLoggedInUserRequest(testUserAUsername, testUserAPassword)).isNotNull();
    }

    private ResponseEntity<AuthenticatedUser> getLoggedInUserRequest(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(username, password);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/api/authenticate"),
                HttpMethod.GET,
                request,
                AuthenticatedUser.class);
    }

    private ResponseEntity<Void> createUserRequest(String username, String password) {
        UserRegistrationRequest payload = new UserRegistrationRequest(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserRegistrationRequest> request = new HttpEntity<>(payload, headers);

        return restTemplate.postForEntity(
                createUrl("/api/users"),
                request,
                Void.class);
    }

    private ResponseEntity<Void> deleteUserRequest(String username, String password, String usernameToDelete) {
        HttpHeaders headers = new HttpHeaders();
        String authToken = login(username, password);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                createUrl("/api/users/" + usernameToDelete),
                HttpMethod.DELETE,
                request,
                Void.class);
    }

    private ResponseEntity<Void> updatePasswordRequest(String username, String password, String usernameToUpdate, String newPassword) {
        UpdatePasswordRequest payload = new UpdatePasswordRequest(newPassword);

        HttpHeaders headers = new HttpHeaders();
        String authToken = login(username, password);
        headers.add("Cookie", "JSESSIONID=" + authToken);

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(
                createUrl("/api/users/" + usernameToUpdate + "/password"),
                HttpMethod.PUT,
                request,
                Void.class);
    }

    private ResponseEntity<Void> updateAuthorityRequest(
            String username,
            String password,
            String usernameToUpdate,
            UserAuthority newAuthority) {

        UpdateAuthorityRequest payload = new UpdateAuthorityRequest(newAuthority);

        HttpHeaders headers = new HttpHeaders();
        String authToken = login(username, password);
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

    public String login(String username, String password) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.set("username", username);
        params.set("password", password);

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
