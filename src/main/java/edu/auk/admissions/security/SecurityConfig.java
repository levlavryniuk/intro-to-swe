package edu.auk.admissions.security;

import edu.auk.admissions.web.SpaController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Access rules from NFR-01: visitors use the public API, applicants see only their own application,
 * only staff reach the staff API and the documents. The React front end calls this API with fetch(),
 * so sign-in and sign-out answer with JSON and status codes instead of redirects.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/applicant/**").hasRole(Roles.APPLICANT)
                        .requestMatchers("/api/staff/**").hasRole(Roles.STAFF)
                        // The React app itself is public; its data comes from the protected API above.
                        .requestMatchers(SpaController.ROUTES).permitAll()
                        .requestMatchers("/index.html", "/assets/**", "/favicon.svg", "/error").permitAll()
                        .anyRequest().authenticated())
                // People sign in with the email address they registered with.
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .successHandler(signedIn())
                        .failureHandler(signInFailed())
                        .permitAll())
                .logout(logout -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                // The app sends the token from the XSRF-TOKEN cookie in the X-XSRF-TOKEN header.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Tells the app which role signed in, so it can open the staff or the applicant page. */
    private static AuthenticationSuccessHandler signedIn() {
        return (request, response, authentication) -> {
            boolean staff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Roles.STAFF));
            writeJson(response, HttpStatus.OK, "{\"role\":\"" + (staff ? Roles.STAFF : Roles.APPLICANT) + "\"}");
        };
    }

    private static AuthenticationFailureHandler signInFailed() {
        return (request, response, exception) -> writeJson(response, HttpStatus.UNAUTHORIZED,
                "{\"message\":\"The email address or password is not correct. Check them and try again.\"}");
    }

    private static void writeJson(HttpServletResponse response, HttpStatus status, String json) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    /** Makes sure the XSRF-TOKEN cookie is written on every response, including for visitors. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
