package com.github.arburk.vscp.backend.infra.api;

import com.github.arburk.vscp.backend.core.domain.PokerTimerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  @GetMapping(value = "/", produces = {"application/json"})
  public PokerTimerConfig getConfig() {
    PokerTimerConfig pokerTimerConfig = new PokerTimerConfig((short) 12, (short) 1, Collections.emptyList());
    log.info("TODO: inject service to get config {}", pokerTimerConfig);
    return pokerTimerConfig;
  }

  @PostMapping(value = "/", consumes = {"application/json"})
  public void updateConfig(@RequestBody PokerTimerConfig config) {
    log.info("TODO: update {}", config);
  }

}
