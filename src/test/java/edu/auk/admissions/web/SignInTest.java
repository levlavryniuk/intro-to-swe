package edu.auk.admissions.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Local sign-in used by the React app: people sign in with the email address they registered with. */
class SignInTest extends ApiTestBase {

    @Test
    @DisplayName("Every page of the React app is served to visitors; the data behind them stays protected")
    void appPagesArePublic() throws Exception {
        for (String page : SpaController.ROUTES) {
            mvc.perform(get(page)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
        mvc.perform(get("/api/staff/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A new applicant signs in with the email and password from sign-up")
    void applicantSignsIn() throws Exception {
        String email = uniqueEmail("newapplicant");
        postJson("/api/public/accounts", Map.of("name", "New Applicant", "email", email, "password", "secret-pass-1"), null)
                .andExpect(status().isCreated());

        mvc.perform(formLogin("/login").userParameter("email").user(email).password("secret-pass-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("APPLICANT"));
    }

    @Test
    @DisplayName("Staff are told they signed in as staff")
    void staffSignsIn() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(staffEmail).password("password-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    @DisplayName("A wrong password is refused with a message")
    void wrongPassword() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(staffEmail).password("wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("The email address or password is not correct. Check them and try again."));
    }

    @Test
    @DisplayName("Signing out answers 204 No Content")
    void signOut() throws Exception {
        mvc.perform(post("/logout").with(asStaff()).with(csrf())).andExpect(status().isNoContent());
    }
}
