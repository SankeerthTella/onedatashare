package org.onedatashare.server.system;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onedatashare.server.controller.LoginController.LoginControllerRequest;
import org.onedatashare.server.controller.RegistrationController.RegistrationControllerRequest;
import org.onedatashare.server.model.core.ODSConstants;
import org.onedatashare.server.model.core.Role;
import org.onedatashare.server.system.mockuser.WithMockCustomUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * A system test suite that tests actions on user accounts like logging in, changing passwords and verification.
 * In order to inform the backend components of the currently logged in user, each test must be annotated with the
 * {@link WithMockCustomUser} annotation, which is a custom annotation that extends and customizes the behavior
 * of Spring's security context. In order to emulate an anonymous request, Spring's {@link WithAnonymousUser}
 * should be used
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class UserActionTest extends SystemTest {

    private static final String TEST_USER_EMAIL = "bigstuff@bigwhoopcorp.com";
    private static final String TEST_USER_NAME = "test_user";
    private static final String TEST_USER_PASSWORD = "IamTheWalrus";

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserDoesNotExist_WhenRegistered_ShouldBeAddedToUserRepository() throws Exception {
        registerUserAndChangePassword(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME);

        assertEquals(users.size(), 1);
        assertEquals((getFirstUser()).getEmail(), TEST_USER_EMAIL);
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserAlreadyExists_WhenRegistered_ShouldNotDuplicateUser() throws Exception {
        registerUserAndChangePassword(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME);
        long firstRegistrationTimestamp = getFirstUser().getRegisterMoment();

        // Try registering the same user again
        registerUserAndChangePassword(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME);

        assertEquals(users.size(), 1);
        assertEquals(getFirstUser().getEmail(), TEST_USER_EMAIL);
        assertEquals(getFirstUser().getRegisterMoment(), firstRegistrationTimestamp);
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserRegistered_WhenCodeIsVerified_ShouldValidateUser() throws Exception {
        registerUserAndChangePassword(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME);

        assertTrue(users.get(TEST_USER_EMAIL).isValidated());
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserVerified_WhenLoggingIn_ShouldSucceed() throws Exception {
        registerUserAndChangePassword(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME);

        boolean wasSuccessful = loginUser(TEST_USER_EMAIL, TEST_USER_PASSWORD);

        assertTrue(wasSuccessful);
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserNotVerified_WhenLoggingIn_ShouldFail() throws Exception {
        // does not perform verification by email
        registerUser(TEST_USER_EMAIL, TEST_USER_NAME);

        boolean loginSuccessful = loginUser(TEST_USER_EMAIL, TEST_USER_PASSWORD);

        assertFalse(loginSuccessful);
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserChangedPassword_WhenLoggingInWithNewPassword_ShouldSucceed() throws Exception {
        String oldPassword = TEST_USER_PASSWORD;
        String new_password = "new_password";
        registerUserAndChangePassword(TEST_USER_EMAIL, oldPassword, TEST_USER_NAME);
        loginUser(TEST_USER_EMAIL, oldPassword);

        resetUserPassword(TEST_USER_EMAIL, oldPassword, new_password);

        boolean loginSuccessful = loginUser(TEST_USER_EMAIL, new_password);
        assertTrue(loginSuccessful);
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenUserChangedPassword_WhenLoggingInWithOldPassword_ShouldFail() throws Exception {
        String oldPassword = TEST_USER_PASSWORD;
        String new_password = "new_password";
        registerUserAndChangePassword(TEST_USER_EMAIL, oldPassword, TEST_USER_NAME);

        resetUserPassword(TEST_USER_EMAIL, oldPassword, new_password);

        boolean loginSuccessful = loginUser(TEST_USER_EMAIL, oldPassword);
        assertFalse(loginSuccessful);
    }

    @Test
    @WithMockCustomUser(username = TEST_USER_EMAIL, role = Role.USER)
    public void givenIncorrectOldPassword_WhenResettingPassword_ShouldFail() throws Exception {
        String oldPassword = TEST_USER_PASSWORD;
        String new_password = "new_password";
        String wrongPassword = "random_guess";
        registerUserAndChangePassword(TEST_USER_EMAIL, oldPassword, TEST_USER_NAME);

        resetUserPassword(TEST_USER_EMAIL, wrongPassword, new_password);

        boolean loginSuccessful = loginUser(TEST_USER_EMAIL, wrongPassword);
        assertFalse(loginSuccessful);
        loginSuccessful = loginUser(TEST_USER_EMAIL, oldPassword);
        assertTrue(loginSuccessful);
    }

    private void resetUserPassword(String userEmail, String oldPassword, String newPassword) throws Exception {
        LoginControllerRequest requestData = new LoginControllerRequest();
        requestData.setEmail(userEmail);
        requestData.setPassword(oldPassword);
        requestData.setNewPassword(newPassword);
        requestData.setConfirmPassword(newPassword);
        processPostWithRequestData(ODSConstants.UPDATE_PASSWD_ENDPOINT, requestData);
    }

    private void setUserPassword(String userEmail, String userPassword, String authToken) throws Exception {
        LoginControllerRequest requestData = new LoginControllerRequest();
        requestData.setEmail(userEmail);
        requestData.setPassword(userPassword);
        requestData.setConfirmPassword(userPassword);
        requestData.setCode(authToken);
        processPostWithRequestData(ODSConstants.RESET_PASSWD_ENDPOINT, requestData);
    }

    private boolean loginUser(String userEmail, String password) throws Exception {
        LoginControllerRequest requestData = new LoginControllerRequest();
        requestData.setEmail(userEmail);
        requestData.setPassword(password);
        long timeBeforeLoggingIn = System.currentTimeMillis();
        processPostWithRequestData(ODSConstants.AUTH_ENDPOINT, requestData);
        Long lastActivity = users.get(userEmail).getLastActivity();
        return lastActivity != null && lastActivity > timeBeforeLoggingIn;
    }

    private void registerUserAndChangePassword(String userEmail, String userPassword, String username) throws Exception {
        registerUser(userEmail, username);
        String verificationCodeEmail = inbox.get(userEmail);
        String verificationCode = extractVerificationCode(verificationCodeEmail);
        verifyCode(userEmail, verificationCode);
        String authToken = users.get(userEmail).getAuthToken();
        setUserPassword(userEmail, userPassword, authToken);
    }

    private void verifyCode(String userEmail, String verificationCode) throws Exception {
        RegistrationControllerRequest requestData = new RegistrationControllerRequest();
        requestData.setEmail(userEmail);
        requestData.setCode(verificationCode);
        processPostWithRequestData(ODSConstants.EMAIL_VERIFICATION_ENDPOINT, requestData);
    }

    private void registerUser(String userEmail, String firstName) throws Exception {
        RegistrationControllerRequest requestData = new RegistrationControllerRequest();
        requestData.setFirstName(firstName);
        requestData.setEmail(userEmail);
        processPostWithRequestData(ODSConstants.REGISTRATION_ENDPOINT, requestData);
    }

    private String extractVerificationCode(String verificationCodeEmail) {
        return verificationCodeEmail.substring(verificationCodeEmail.lastIndexOf(" ")).trim();
    }
}
