package com.github.arburk.vscp.backend;

import com.github.arburk.vscp.backend.core.services.UserInfoService;
import com.github.arburk.vscp.backend.core.services.UserLoggingFilter;
import com.github.arburk.vscp.backend.infra.api.config.ConfigController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles(value = "test")
class VscpBackendApplicationTests {

  @Autowired private ConfigController configController;

  @Autowired private UserInfoService userInfoService;

  @Autowired private UserLoggingFilter userLoggingFilter;

  @Test
  void contextLoads() {
    assertNotNull(configController);
    assertNotNull(userInfoService);
    assertNotNull(userLoggingFilter);
  }

}
