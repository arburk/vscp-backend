package com.github.arburk.vscp.backend.infra.api.config;

import com.github.arburk.vscp.backend.KeycloakTestContainer;
import com.github.arburk.vscp.backend.core.domain.PokerTimerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
class ConfigControllerTest extends KeycloakTestContainer {

  private static final String CONFIG_URL = "/config/";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testConfigGetEndpoint_Unauthorized_Forbidden() {
    final ResponseEntity<PokerTimerConfig> response = testRestTemplate.getForEntity(CONFIG_URL, PokerTimerConfig.class);
    assertEquals(response.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void testConfigGetEndpoint_Authorized() {
    final String accessToken = keycloakSimpleApi.tokenManager().grantToken().getToken();
    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    final ResponseEntity<PokerTimerConfig> response = testRestTemplate.exchange(
        CONFIG_URL, HttpMethod.GET, new HttpEntity<>(headers), PokerTimerConfig.class);

    assertEquals(response.getStatusCode(), HttpStatus.OK);
    final PokerTimerConfig body = response.getBody();
    assertNotNull(body);
    assertEquals("PokerTimerConfig[roundInMinutes=12, warningTimeInMinutes=1, blindLevels=[]]", body.toString());
  }
}