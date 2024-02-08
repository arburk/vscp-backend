package com.github.arburk.vscp.backend.infra.api.config;

import com.github.arburk.vscp.backend.core.domain.PokerTimerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
class ConfigControllerMvcTest {

  private static final String ENDPOUNT_URL = "/config/";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void testConfigGetEndpoint_Unauthorized() {
    final ResponseEntity<PokerTimerConfig> response = testRestTemplate.getForEntity(ENDPOUNT_URL, PokerTimerConfig.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void testConfigGetEndpoint_Authorized_OK() {
    IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().forEach(number -> {
      try {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOUNT_URL)
                .with(SecurityMockMvcRequestPostProcessors.user("mockuser").roles("Authenticated"))
                .accept(MediaType.ALL))
            .andExpect(status().isOk());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void testConfigPostEndpoint_Unauthorized() {
    final PokerTimerConfig testConfig = new PokerTimerConfig((short) 2, (short) 1, Collections.emptyList());
    final HttpEntity<?> httpEntity = new HttpEntity<>(testConfig, new HttpHeaders());

    final ResponseEntity<Object> response = testRestTemplate.exchange(
        ENDPOUNT_URL, HttpMethod.POST, httpEntity, (Class<Object>) null, Collections.emptyMap());

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

}