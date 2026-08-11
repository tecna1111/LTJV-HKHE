package com.cosre.cosre_backend.config;

import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final AccountService accountService;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;
    @Value("${app.bootstrap-admin.username:admin}")
    private String username;
    @Value("${app.bootstrap-admin.email:admin@cosre.local}")
    private String email;
    @Value("${app.bootstrap-admin.password:Admin@123}")
    private String password;

    @Value("${app.bootstrap-users.enabled:false}")
    private boolean bootstrapUsersEnabled;
    @Value("${app.bootstrap-users.head-dept.username:headdept}")
    private String headDeptUsername;
    @Value("${app.bootstrap-users.head-dept.email:headdept@cosre.local}")
    private String headDeptEmail;
    @Value("${app.bootstrap-users.head-dept.password:HeadDept@123}")
    private String headDeptPassword;
    @Value("${app.bootstrap-users.staff.username:staff}")
    private String staffUsername;
    @Value("${app.bootstrap-users.staff.email:staff@cosre.local}")
    private String staffEmail;
    @Value("${app.bootstrap-users.staff.password:Staff@123}")
    private String staffPassword;

    public DataInitializer(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void run(String... args) {
        if (enabled) {
            createUserIfMissing(username, email, password, "System Administrator", RoleEnum.ADMIN);
        }
        if (bootstrapUsersEnabled) {
            createUserIfMissing(
                    headDeptUsername,
                    headDeptEmail,
                    headDeptPassword,
                    "Trưởng bộ môn COSRE",
                    RoleEnum.HEAD_DEPT
            );
            createUserIfMissing(
                    staffUsername,
                    staffEmail,
                    staffPassword,
                    "Nhân viên đào tạo COSRE",
                    RoleEnum.STAFF
            );
        }
    }

    private void createUserIfMissing(
            String username,
            String email,
            String password,
            String fullName,
            RoleEnum role
    ) {
        if (accountService.findByUsername(username).isEmpty()
                && accountService.findByEmail(email).isEmpty()) {
            accountService.createUser(username, email, password, fullName, role);
        }
    }
}
