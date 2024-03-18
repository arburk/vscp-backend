package com.github.arburk.vscp.backend;

import org.jetbrains.annotations.NotNull;
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
import java.util.List;

@Testcontainers
public abstract class KeycloakTestContainer {

  private static final String IMAGE = "quay.io/keycloak/keycloak:24.0.1";
  private static final String MY_REALM_NAME = "vscp";
  private static final String SIMPLE_API_CLIENT_ID = "mytest-client-id";
  private static final String SIMPLE_API_CLIENT_SECRET = "my-secret-test-password";
  private static final List<String> SIMPLE_API_ROLES_ADMIN = Collections.singletonList("ROLE_ADMIN");
  private static final List<String> SIMPLE_API_ROLES_USER = Collections.singletonList("ROLE_USER");
  private static final String ADMIN_USERNAME = "my-admin-user";
  private static final String USER_USERNAME = "my-user";
  private static final String ADMIN_PASSWORD = "myAdminPassw0rd";
  private static final String USER_PASSWORD = "myU5erPassw0rd";
  private static final String KEYCLOAK_ADMIN = "admin";
  private static final String KEYCLOAK_ADMIN_PW = "admin";

  /**
   * representation of a user having ROLE_ADMIN assigned
   */
  protected static Keycloak keycloakAdminApi;

  /**
   * representation of a user having ROLE_USER assigned
   */
  protected static Keycloak keycloakUserApi;

  private static final GenericContainer<?> keycloakContainer = new GenericContainer<>(IMAGE);

  @DynamicPropertySource
  private static void dynamicProperties(DynamicPropertyRegistry registry) {
    keycloakContainer.withExposedPorts(8080)
        .withEnv("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN)
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PW)
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

    if (keycloakAdminApi == null) {
      setupKeycloakAdmin(keycloakServerUrl);
    }
    if(keycloakUserApi == null) {
      setupKeycloakUser(keycloakServerUrl);
    }
  }

  private static void setupKeycloakAdmin(String keycloakServerUrl) {
    // Client
    final ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setId(SIMPLE_API_CLIENT_ID);
    clientRepresentation.setDirectAccessGrantsEnabled(true);
    clientRepresentation.setSecret(SIMPLE_API_CLIENT_SECRET);

    final UserRepresentation userRepresentation = getUserRepresentation(ADMIN_USERNAME, ADMIN_PASSWORD, SIMPLE_API_ROLES_ADMIN);

    // Realm
    final RealmRepresentation realmRepresentation = new RealmRepresentation();
    realmRepresentation.setRealm(MY_REALM_NAME);
    realmRepresentation.setEnabled(true);
    realmRepresentation.setClients(Collections.singletonList(clientRepresentation));
    realmRepresentation.setUsers(Collections.singletonList(userRepresentation));

    getKeycloakAdmin(keycloakServerUrl)
        .realms()
        .create(realmRepresentation);

    keycloakAdminApi = KeycloakBuilder.builder()
        .serverUrl(keycloakServerUrl)
        .realm(MY_REALM_NAME)
        .username(ADMIN_USERNAME)
        .password(ADMIN_PASSWORD)
        .clientId(SIMPLE_API_CLIENT_ID)
        .clientSecret(SIMPLE_API_CLIENT_SECRET)
        .build();
  }

  private static void setupKeycloakUser(String keycloakServerUrl) {
    getKeycloakAdmin(keycloakServerUrl)
        .realms()
        .realm(MY_REALM_NAME)
        .users()
        .create(getUserRepresentation(USER_USERNAME, USER_PASSWORD, SIMPLE_API_ROLES_USER));

    keycloakUserApi = KeycloakBuilder.builder()
        .serverUrl(keycloakServerUrl)
        .realm(MY_REALM_NAME)
        .username(USER_USERNAME)
        .password(USER_PASSWORD)
        .clientId(SIMPLE_API_CLIENT_ID)
        .clientSecret(SIMPLE_API_CLIENT_SECRET)
        .build();
  }

  @NotNull
  private static UserRepresentation getUserRepresentation(final String userUsername, final String userPassword, final List<String> simpleApiRolesUser) {
    final UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(userUsername);
    userRepresentation.setEnabled(true);
    userRepresentation.setCredentials(Collections.singletonList(getPasswordCredentialRepresentation(userPassword)));
    userRepresentation.setRealmRoles(simpleApiRolesUser);

    /* Enable verify-profile required action by default was introduced by keycloak v24
     * see https://www.keycloak.org/2024/03/keycloak-2400-released
     * Thus at least email, first and last name needs to be provided to enable direct access grant
     * see https://github.com/keycloak/keycloak/blob/main/docs/documentation/server_admin/topics/users/user-profile.adoc#understanding-the-default-configuration
     */
    userRepresentation.setEmail(userUsername +".keycloak24@requires-email.com");
    userRepresentation.setEmailVerified(true);
    userRepresentation.setFirstName("first " + userUsername);
    userRepresentation.setLastName("last " + userUsername);

    return userRepresentation;
  }

  @NotNull
  private static CredentialRepresentation getPasswordCredentialRepresentation(final String password) {
    final CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
    credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
    credentialRepresentation.setValue(password);
    return credentialRepresentation;
  }

  private static Keycloak getKeycloakAdmin(final String keycloakServerUrl) {
    final Keycloak keycloakAdmin = KeycloakBuilder.builder()
        .serverUrl(keycloakServerUrl)
        .realm("master")
        .username(KEYCLOAK_ADMIN)
        .password(KEYCLOAK_ADMIN_PW)
        .clientId("admin-cli")
        .build();
    return keycloakAdmin;
  }
}
