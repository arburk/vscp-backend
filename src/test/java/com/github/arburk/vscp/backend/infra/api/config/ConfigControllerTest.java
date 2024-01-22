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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
class ConfigControllerTest extends KeycloakTestContainer {

  private static final String ENDPOUNT_URL = "/config/";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testConfigGetEndpoint_Unauthorized() {
    final ResponseEntity<PokerTimerConfig> response = testRestTemplate.getForEntity(ENDPOUNT_URL, PokerTimerConfig.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testConfigGetEndpoint_Authorized_OK() {
    final String accessToken = keycloakAdminApi.tokenManager().grantToken().getToken();
    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    final ResponseEntity<PokerTimerConfig> response = testRestTemplate.exchange(
        ENDPOUNT_URL, HttpMethod.GET, new HttpEntity<>(headers), PokerTimerConfig.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    final PokerTimerConfig body = response.getBody();
    assertNotNull(body);
    assertEquals("PokerTimerConfig[roundInMinutes=12, warningTimeInMinutes=1, blindLevels=[]]", body.toString());
  }

  @Test
  void testConfigPostEndpoint_Unauthorized() {
    final PokerTimerConfig testConfig = new PokerTimerConfig((short) 2, (short) 1, Collections.emptyList());
    final HttpEntity<?> httpEntity = new HttpEntity<>(testConfig, new HttpHeaders());

    final ResponseEntity<Object> response = testRestTemplate.exchange(
        ENDPOUNT_URL, HttpMethod.POST, httpEntity, (Class<Object>) null, Collections.emptyMap());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testConfigPostEndpoint_Authorized_FORBIDDEN() {

    final String accessToken = keycloakUserApi.tokenManager().grantToken().getToken();

    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    final PokerTimerConfig testConfig = new PokerTimerConfig((short) 2, (short) 1, Collections.emptyList());
    final HttpEntity<?> httpEntity = new HttpEntity<>(testConfig, headers);

    final ResponseEntity<Object> response = testRestTemplate.exchange(
        ENDPOUNT_URL, HttpMethod.POST, httpEntity, (Class<Object>) null, Collections.emptyMap());

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  void testConfigPostEndpoint_Authorized_OK() {
    final String accessToken = keycloakAdminApi.tokenManager().grantToken().getToken();
    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    final PokerTimerConfig testConfig = new PokerTimerConfig((short) 2, (short) 1, Collections.emptyList());
    final HttpEntity<?> httpEntity = new HttpEntity<>(testConfig, headers);

    final ResponseEntity<Object> response = testRestTemplate.exchange(
        ENDPOUNT_URL, HttpMethod.POST, httpEntity, (Class<Object>) null, Collections.emptyMap());

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

}