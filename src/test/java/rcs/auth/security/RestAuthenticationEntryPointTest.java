package rcs.auth.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rcs.auth.exceptions.UnauthorizedException;
import rcs.auth.security.RestAuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
public class RestAuthenticationEntryPointTest {

    private RestAuthenticationEntryPoint target;

    @Before
    public void setup() {
        target = new RestAuthenticationEntryPoint();
    }

    @Test
    public void testCommence() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);

        // Act & assert
        assertThrows(
                UnauthorizedException.class,
                () -> target.commence(request, response, exception));
    }
}
