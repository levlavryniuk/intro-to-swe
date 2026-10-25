package edu.auk.admissions.security;

import edu.auk.admissions.domain.Applicant;
import edu.auk.admissions.domain.BusinessRuleException;
import edu.auk.admissions.domain.Lead;
import edu.auk.admissions.domain.StaffMember;
import edu.auk.admissions.domain.User;
import edu.auk.admissions.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates local accounts. Applicants sign themselves up; staff accounts are created by the seeder. */
@Service
public class AccountService {

    private final UserRepository users;
    private final LocalAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository users, LocalAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Applicant signUpApplicant(String name, String email, String password) {
        return create(new Applicant(name.trim(), Lead.normalizeEmail(email)), password);
    }

    @Transactional
    public StaffMember createStaff(String name, String email, String password) {
        return create(new StaffMember(name.trim(), Lead.normalizeEmail(email)), password);
    }

    private <T extends User> T create(T user, String password) {
        if (users.existsByEmailIgnoreCase(user.getEmail())) {
            throw new BusinessRuleException("An account with this email address already exists.");
        }
        T saved = users.save(user);
        saved.setPlatformUserId("local-" + saved.getId());
        accounts.save(new LocalAccount(saved, passwordEncoder.encode(password)));
        return saved;
    }
}
