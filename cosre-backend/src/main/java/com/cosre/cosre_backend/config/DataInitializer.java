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

    public DataInitializer(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void run(String... args) {
        if (enabled && accountService.findByUsername(username).isEmpty()) {
            accountService.createUser(username, email, password, "System Administrator", RoleEnum.ADMIN);
        }
    }
}
