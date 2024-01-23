package com.github.arburk.vscp.backend.core.services;

import com.github.arburk.vscp.backend.config.api.AuthorizedParty;
import com.github.arburk.vscp.backend.core.domain.User;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class UserInfoServiceTest {

  private SecurityContext mockedContext;
  private AuthorizedParty mockedAZP;

  private UserInfoService testee;

  @BeforeEach
  void setUp() {
    mockedAZP = Mockito.mock(AuthorizedParty.class);
    mockedContext = Mockito.mock(SecurityContext.class);
    testee = new UserInfoService(mockedAZP);
  }

  @Test
  void getAsUser_Unauthenticated() {
    final Authentication mockedAuth = Mockito.mock(Authentication.class);
    when(mockedContext.getAuthentication()).thenReturn(mockedAuth);
    when(mockedAuth.isAuthenticated()).thenReturn(false);

    try (MockedStatic<SecurityContextHolder> secCtxHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
      secCtxHolder.when(SecurityContextHolder::getContext).thenReturn(mockedContext);

      final Optional<User> result = testee.getAsUser();
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void getAsUser_UsernamePasswordAuthenticationToken() {
    final var auth = Collections.singletonList(new SimpleGrantedAuthority("testrole"));
    final var user = new org.springframework.security.core.userdetails.User("testuser", "testpwd", auth);

    final var mockedAuth = Mockito.mock(UsernamePasswordAuthenticationToken.class);
    when(mockedContext.getAuthentication()).thenReturn(mockedAuth);
    when(mockedAuth.isAuthenticated()).thenReturn(true);
    when(mockedAuth.getPrincipal()).thenReturn(user);

    try (MockedStatic<SecurityContextHolder> secCtxHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
      secCtxHolder.when(SecurityContextHolder::getContext).thenReturn(mockedContext);

      final Optional<User> result = testee.getAsUser();
      assertTrue(result.isPresent());
      final User mappedUser = result.get();
      assertEquals("testuser", mappedUser.userName());
      assertEquals("testuser", mappedUser.subject());
      assertNull(mappedUser.name());
      assertNull(mappedUser.givenName());
      assertNull(mappedUser.familyName());
      assertNull(mappedUser.email());
      assertNull(mappedUser.pictureUrl());
      assertEquals(1, mappedUser.roles().size());
      assertEquals("testrole", mappedUser.roles().getFirst());
      assertEquals(AuthorizedParty.BASIC, mappedUser.authorizedParty());

    }
  }

  @Test
  void getAsUser_OAuth2AuthenticationToken_KeyCloak() {
    final var oidcUser = Mockito.mock(OidcUserAuthority.class);
    when(oidcUser.getAuthority()).thenReturn("OIDC_USER");
    when(mockedAZP.getKeycloak()).thenReturn("keycloak-client-id");
    when(oidcUser.getAttributes()).thenReturn(getOidcAttributes("keycloak-client-id"));

    final var mockedAuth = Mockito.mock(OAuth2AuthenticationToken.class);
    when(mockedContext.getAuthentication()).thenReturn(mockedAuth);
    when(mockedAuth.isAuthenticated()).thenReturn(true);
    when(mockedAuth.getAuthorities()).thenReturn(Collections.singletonList(oidcUser));

    try (MockedStatic<SecurityContextHolder> secCtxHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
      secCtxHolder.when(SecurityContextHolder::getContext).thenReturn(mockedContext);

      final Optional<User> result = testee.getAsUser();
      assertTrue(result.isPresent());
      final User mappedUser = result.get();
      assertEquals("username_preferred_by_keycloak", mappedUser.userName());
      assertEquals("subject", mappedUser.subject());
      assertEquals("Name Test", mappedUser.name());
      assertEquals("Given Test Name", mappedUser.givenName());
      assertEquals("Family Test Name", mappedUser.familyName());
      assertEquals("user@email.test", mappedUser.email());
      assertEquals("http://link.to.my.picture/", mappedUser.pictureUrl());
      assertEquals(2, mappedUser.roles().size());
      assertEquals("ROLE_REALM_ACCESS_1", mappedUser.roles().getFirst());
      assertEquals("ROLE_RESOURCE_ACCESS_1", mappedUser.roles().getLast());
      assertEquals("keycloak-client-id", mappedUser.authorizedParty());
    }
  }


  @Test
  void getAsUser_JwtToken_KeyCloak() {
    when(mockedAZP.getKeycloak()).thenReturn("keycloak-client-id");

    final var mockedAuth = Mockito.mock(JwtAuthenticationToken.class);
    when(mockedContext.getAuthentication()).thenReturn(mockedAuth);
    when(mockedAuth.isAuthenticated()).thenReturn(true);
    when(mockedAuth.getTokenAttributes()).thenReturn(getOidcAttributes("keycloak-client-id"));

    try (MockedStatic<SecurityContextHolder> secCtxHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
      secCtxHolder.when(SecurityContextHolder::getContext).thenReturn(mockedContext);

      final Optional<User> result = testee.getAsUser();
      assertTrue(result.isPresent());
      final User mappedUser = result.get();
      assertEquals("username_preferred_by_keycloak", mappedUser.userName());
      assertEquals("subject", mappedUser.subject());
      assertEquals("Name Test", mappedUser.name());
      assertEquals("Given Test Name", mappedUser.givenName());
      assertEquals("Family Test Name", mappedUser.familyName());
      assertEquals("user@email.test", mappedUser.email());
      assertEquals("http://link.to.my.picture/", mappedUser.pictureUrl());
      assertEquals(2, mappedUser.roles().size());
      assertEquals("ROLE_REALM_ACCESS_1", mappedUser.roles().getFirst());
      assertEquals("ROLE_RESOURCE_ACCESS_1", mappedUser.roles().getLast());
      assertEquals("keycloak-client-id", mappedUser.authorizedParty());
    }
  }

  @Test
  void getAsUser_OAuth2AuthenticationToken_Google() {
    final var oidcUser = Mockito.mock(OidcUserAuthority.class);
    when(oidcUser.getAuthority()).thenReturn("OIDC_USER");
    when(mockedAZP.getKeycloak()).thenReturn("keycloak-client-id");
    when(oidcUser.getAttributes()).thenReturn(getOidcAttributes("google-client-id"));

    final var mockedAuth = Mockito.mock(OAuth2AuthenticationToken.class);
    when(mockedContext.getAuthentication()).thenReturn(mockedAuth);
    when(mockedAuth.isAuthenticated()).thenReturn(true);
    when(mockedAuth.getAuthorities()).thenReturn(Collections.singletonList(oidcUser));

    try (MockedStatic<SecurityContextHolder> secCtxHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
      secCtxHolder.when(SecurityContextHolder::getContext).thenReturn(mockedContext);

      final Optional<User> result = testee.getAsUser();
      assertTrue(result.isPresent());
      final User mappedUser = result.get();
      assertEquals("user@email.test", mappedUser.userName());
      assertEquals("subject", mappedUser.subject());
      assertEquals("Name Test", mappedUser.name());
      assertEquals("Given Test Name", mappedUser.givenName());
      assertEquals("Family Test Name", mappedUser.familyName());
      assertEquals("user@email.test", mappedUser.email());
      assertEquals("http://link.to.my.picture/", mappedUser.pictureUrl());
      assertEquals(2, mappedUser.roles().size());
      assertEquals("ROLE_REALM_ACCESS_1", mappedUser.roles().getFirst());
      assertEquals("ROLE_RESOURCE_ACCESS_1", mappedUser.roles().getLast());
      assertEquals("google-client-id", mappedUser.authorizedParty());
    }
  }

  @Test
  void getAsUser_UnmappedAuth_Fallback() {
    final var mockedAuth = Mockito.mock(AnonymousAuthenticationToken.class);
    when(mockedContext.getAuthentication()).thenReturn(mockedAuth);
    when(mockedAuth.isAuthenticated()).thenReturn(true);
    when(mockedAuth.getName()).thenReturn("anonymous");

    try (MockedStatic<SecurityContextHolder> secCtxHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
      secCtxHolder.when(SecurityContextHolder::getContext).thenReturn(mockedContext);

      final Optional<User> result = testee.getAsUser();
      assertTrue(result.isPresent());
      final User mappedUser = result.get();
      assertEquals("anonymous", mappedUser.userName());
      assertNull(mappedUser.subject());
      assertNull(mappedUser.name());
      assertNull(mappedUser.givenName());
      assertNull(mappedUser.familyName());
      assertNull(mappedUser.email());
      assertNull(mappedUser.pictureUrl());
      assertTrue(mappedUser.roles().isEmpty());
      assertNull(mappedUser.authorizedParty());
    }
  }

  private Map<String, Object> getOidcAttributes(String azp) {
    final HashMap<String, Object> oidcAttribs = new HashMap<>();
    oidcAttribs.put("azp", azp);
    oidcAttribs.put("preferred_username", "username_preferred_by_keycloak");
    oidcAttribs.put("email", "user@email.test");
    oidcAttribs.put("sub", "subject");
    oidcAttribs.put("name", "Name Test");
    oidcAttribs.put("given_name", "Given Test Name");
    oidcAttribs.put("family_name", "Family Test Name");
    oidcAttribs.put("picture", "http://link.to.my.picture/");
    final LinkedTreeMap<Object, Object> realmAccess = new LinkedTreeMap<>();
    realmAccess.put("role1", "ROLE_REALM_ACCESS_1");
    oidcAttribs.put("realm_access", realmAccess);
    oidcAttribs.put("resource_access", Collections.singletonList("ROLE_RESOURCE_ACCESS_1"));
    return oidcAttribs;
  }
}