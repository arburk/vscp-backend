package com.github.arburk.vscp.backend.infra.api.config;

import com.github.arburk.vscp.backend.config.api.UserInfo;
import com.github.arburk.vscp.backend.core.domain.PokerTimerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController()
@RequestMapping("config")
public class ConfigController {

  private final Logger log = LoggerFactory.getLogger(ConfigController.class.getName());

  @GetMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
  public PokerTimerConfig getConfig() {

    UserInfo.getAsUser().ifPresent(user -> log.info(user.toString()));

    PokerTimerConfig pokerTimerConfig = new PokerTimerConfig((short) 12, (short) 1, Collections.emptyList());
    log.info("TODO: inject service to get config {}", pokerTimerConfig);
    return pokerTimerConfig;
  }

  @PostMapping(value = "/", consumes = {MediaType.APPLICATION_JSON_VALUE})
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public void updateConfig(@RequestBody PokerTimerConfig config) {
    log.info("TODO: update {}", config);
  }

  @GetMapping(value = "/error/",  produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
  public PokerTimerConfig produceException() {
    log.error("throw error now");
    throw new RuntimeException("Expected error thrown");
  }

}
