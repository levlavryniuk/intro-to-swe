package edu.auk.admissions.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Local sign-in: people sign in with the email address they registered with. */
class SignInTest extends ApiTestBase {

    @Test
    @DisplayName("The sign-in page is public and asks for an email address")
    void signInPage() throws Exception {
        mvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("name=\"email\"")))
                .andExpect(content().string(Matchers.containsString("/signup.html")));
    }

    @Test
    @DisplayName("Pages that need an account send visitors to the sign-in page")
    void redirectToSignIn() throws Exception {
        mvc.perform(get("/applicant.html").accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("http://localhost/login.html"));
    }

    @Test
    @DisplayName("A new applicant signs in with the email and password from sign-up")
    void applicantSignsIn() throws Exception {
        String email = uniqueEmail("newapplicant");
        postJson("/api/public/accounts", Map.of("name", "New Applicant", "email", email, "password", "secret-pass-1"), null)
                .andExpect(status().isCreated());

        mvc.perform(formLogin("/login").userParameter("email").user(email).password("secret-pass-1"))
                .andExpect(redirectedUrl("/applicant.html"));
    }

    @Test
    @DisplayName("Staff land on the staff page after signing in")
    void staffSignsIn() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(staffEmail).password("password-123"))
                .andExpect(redirectedUrl("/staff.html"));
    }

    @Test
    @DisplayName("A wrong password returns to the sign-in page with an error")
    void wrongPassword() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").user(staffEmail).password("wrong"))
                .andExpect(redirectedUrl("/login.html?error"));
    }
}
