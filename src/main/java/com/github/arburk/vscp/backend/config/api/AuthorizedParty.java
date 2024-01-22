package com.github.arburk.vscp.backend.config.api;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AuthorizedParty {

  public static final String BASIC = "basicUserPass";

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String google; //authorized party client id

  @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
  private String keycloak; //authorized party client id

}
