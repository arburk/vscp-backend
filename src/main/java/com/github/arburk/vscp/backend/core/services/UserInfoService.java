package com.github.arburk.vscp.backend.core.services;

import com.github.arburk.vscp.backend.config.api.AuthorizedParty;
import com.github.arburk.vscp.backend.core.domain.User;
import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps the user's info into domain DTO independent of auth mechanism and origin
 */
@Component
@Slf4j
public class UserInfoService implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Getter private final Set<User> userRepo = new HashSet<>();

  private static final String ATTR_REALM_ACCESS = "realm_access";
  private static final String ATTR_EMAIL = "email";
  private static final String ATTR_AZP = "azp";
  public static final String ATTR_ROLES = "roles";
  public static final String GRANTED_AUTHORITY_PREFIX = "ROLE_";

  private AuthorizedParty authParty;

  public UserInfoService(AuthorizedParty authParty) {
    this.authParty = authParty;
  }

  @Override
  public Collection<GrantedAuthority> convert(final Jwt jwt) {
    final Map<String, Object> realmAccess = jwt.getClaim(ATTR_REALM_ACCESS);
    return realmAccess != null
        && realmAccess.containsKey(ATTR_ROLES)
        && realmAccess.get(ATTR_ROLES) instanceof List<?> roleList
        ? asGrantedAuthorities(roleList)
        : Collections.emptyList();
  }

  private static List<GrantedAuthority> asGrantedAuthorities(final List<?> roleList) {
    return roleList.stream()
        .filter(role -> role != null && !String.valueOf(role).trim().isEmpty())
        .map(role -> String.valueOf(role).toUpperCase(Locale.ROOT))
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(GRANTED_AUTHORITY_PREFIX + role))
        .toList();
  }

  @Bean
  public UserDetailsService users() {
    // The builder will ensure the passwords are encoded before saving in memory
    org.springframework.security.core.userdetails.User.UserBuilder users = org.springframework.security.core.userdetails.User.withDefaultPasswordEncoder(); // for demo only
    UserDetails user = users
        .username("user")
        .password("password")
        .roles("USER")
        .build();
    UserDetails admin = users
        .username("admin")
        .password("password")
        .roles("USER", "ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user, admin);
  }

  public Optional<User> getAsUser() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    if (authentication instanceof OAuth2AuthenticationToken oauth2token) {
      return Optional.of(getAsOAuthUser(oauth2token));
    }

    if (authentication instanceof UsernamePasswordAuthenticationToken userPass) {
      return Optional.of(getAsBasicUser(userPass));
    }

    if(authentication instanceof JwtAuthenticationToken jwtToken) {
      return Optional.of(mapUserFromOidcAttributes(jwtToken.getTokenAttributes()));
    }

    log.warn("unknown authentication object {}", authentication.getClass());
    return Optional.of(getBasicUserFallback(authentication));
  }

  private static User getAsBasicUser(final UsernamePasswordAuthenticationToken userPass) {
    if (userPass.getPrincipal() instanceof org.springframework.security.core.userdetails.User springUser) {
      final List<String> roles = springUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
      return new User(AuthorizedParty.basic, springUser.getUsername(), springUser.getUsername(), null, null, null, null, null, roles);
    }

    return getBasicUserFallback(userPass);
  }

  private User getAsOAuthUser(final OAuth2AuthenticationToken oauth2token) {
    Optional<GrantedAuthority> oidcUSerInfo = oauth2token.getAuthorities().stream()
        .filter(auth -> "OIDC_USER".equalsIgnoreCase(auth.getAuthority()))
        .findFirst();

    if (oidcUSerInfo.isEmpty()) {
      log.warn("OIDC_USER not present in provided authorities: {}", oauth2token.getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .collect(Collectors.joining(",")));
      return getBasicUserFallback(oauth2token);
    }

    return oidcUSerInfo.get() instanceof OidcUserAuthority oidcUA
        ? mapUserFromOidcAttributes(oidcUA.getAttributes())
        : getBasicUserFallback(oauth2token);
  }

  private User mapUserFromOidcAttributes(final Map<String, Object> oidcUAAttributes) {
    return new User(
        String.valueOf(oidcUAAttributes.get(ATTR_AZP)),
        String.valueOf(oidcUAAttributes.get("sub")),
        resolveUsername(oidcUAAttributes),
        String.valueOf(oidcUAAttributes.get("name")),
        String.valueOf(oidcUAAttributes.get("given_name")),
        String.valueOf(oidcUAAttributes.get("family_name")),
        String.valueOf(oidcUAAttributes.get(ATTR_EMAIL)),
        String.valueOf(oidcUAAttributes.get("picture")),
        mapRoles(oidcUAAttributes.get(ATTR_REALM_ACCESS)));
  }

  private static List<String> mapRoles(final Object potentialAttributes) {
    if (potentialAttributes instanceof LinkedTreeMap<?, ?> realmAccess) {
      return realmAccess.values().stream().map(String::valueOf).toList();
    }
    if (potentialAttributes instanceof String singleValue) {
      return Collections.singletonList(singleValue);
    }
    if (potentialAttributes instanceof List<?> listValues) {
      return listValues.stream().map(String::valueOf).toList();
    }
    return Collections.emptyList();
  }

  private String resolveUsername(final Map<String, Object> attributes) {
    return authParty.getKeycloak().equalsIgnoreCase(String.valueOf(attributes.get(ATTR_AZP)))
        ? String.valueOf(attributes.get("preferred_username"))
        : String.valueOf(attributes.get(ATTR_EMAIL));
  }

  private static User getBasicUserFallback(final Authentication authentication) {
    return new User(null,null, authentication.getName(), null, null, null,
        null, null, Collections.emptyList());
  }

}
