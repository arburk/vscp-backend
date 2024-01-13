package com.github.arburk.vscp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestVscpBackendApplication {

  public static void main(String[] args) {
    SpringApplication.from(VscpBackendApplication::main).with(TestVscpBackendApplication.class).run(args);
  }

}
