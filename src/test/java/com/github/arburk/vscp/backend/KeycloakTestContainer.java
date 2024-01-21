package com.github.arburk.vscp.backend;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Testcontainers
public abstract class KeycloakTestContainer {

  private static final String IMAGE = "quay.io/keycloak/keycloak:23.0.4";
  private static final String MY_REALM_NAME = "vscp";
  private static final String SIMPLE_API_CLIENT_ID = "mytest-client-id";
  private static final String SIMPLE_API_CLIENT_SECRET = "my-secret-test-password";
  private static final List<String> SIMPLE_API_ROLES = Collections.singletonList("ROLE_ADMIN");
  private static final String USER_USERNAME = "my-admin-user";
  private static final String USER_PASSWORD = "myAdminPassw0rd";


  protected static Keycloak keycloakSimpleApi;

  private static final GenericContainer<?> keycloakContainer = new GenericContainer<>(IMAGE);

  @DynamicPropertySource
  private static void dynamicProperties(DynamicPropertyRegistry registry) {
    keycloakContainer.withExposedPorts(8080)
        .withEnv("KEYCLOAK_ADMIN", "admin")
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
        .withEnv("KC_DB", "dev-mem")
        .withCommand("start-dev")
        .waitingFor(Wait.forHttp("/admin").forPort(8080).withStartupTimeout(Duration.ofMinutes(2)))
        .start();

    final String keycloakHost = keycloakContainer.getHost();
    final Integer keycloakPort = keycloakContainer.getMappedPort(8080);

    final String keycloakServerUrl = String.format("http://%s:%s", keycloakHost, keycloakPort);
    final String issuerUri = String.format("%s/realms/%s", keycloakServerUrl, MY_REALM_NAME);
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> issuerUri + "/protocol/openid-connect/certs");

    if (keycloakSimpleApi == null) {
      setupKeycloak(keycloakServerUrl);
    }
  }

  private static void setupKeycloak(String keycloakServerUrl) {
    final Keycloak keycloakAdmin = KeycloakBuilder.builder()
        .serverUrl(keycloakServerUrl)
        .realm("master")
        .username("admin")
        .password("admin")
        .clientId("admin-cli")
        .build();

    // Realm
    final RealmRepresentation realmRepresentation = new RealmRepresentation();
    realmRepresentation.setRealm(MY_REALM_NAME);
    realmRepresentation.setEnabled(true);

    // Client
    final ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setId(SIMPLE_API_CLIENT_ID);
    clientRepresentation.setDirectAccessGrantsEnabled(true);
    clientRepresentation.setSecret(SIMPLE_API_CLIENT_SECRET);
    realmRepresentation.setClients(Collections.singletonList(clientRepresentation));

    // Credentials
    final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
    credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
    credentialRepresentation.setValue(USER_PASSWORD);

    // Client roles
    final Map<String, List<String>> clientRoles = new HashMap<>();
    clientRoles.put(SIMPLE_API_CLIENT_ID, SIMPLE_API_ROLES);

    // User
    final UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(USER_USERNAME);
    userRepresentation.setEnabled(true);
    userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));
    userRepresentation.setClientRoles(clientRoles);

    realmRepresentation.setUsers(Collections.singletonList(userRepresentation));

    keycloakAdmin.realms().create(realmRepresentation);

    keycloakSimpleApi = KeycloakBuilder.builder()
        .serverUrl(keycloakServerUrl)
        .realm(MY_REALM_NAME)
        .username(USER_USERNAME)
        .password(USER_PASSWORD)
        .clientId(SIMPLE_API_CLIENT_ID)
        .clientSecret(SIMPLE_API_CLIENT_SECRET)
        .build();
  }

}
