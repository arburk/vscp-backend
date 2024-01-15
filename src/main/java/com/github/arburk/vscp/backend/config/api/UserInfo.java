package com.github.arburk.vscp.backend.config.api;

import com.github.arburk.vscp.backend.core.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps the user's info into domain DTO independent of auth mechanism and origin
 */
public class UserInfo {

  private static final Logger log = LoggerFactory.getLogger(UserInfo.class.getName());

  private UserInfo() {
    // static usage only
  }

  public static Optional<User> getAsUser(final AuthorizedParty authParty) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    if (authentication instanceof OAuth2AuthenticationToken oauth2token) {
      return Optional.of(getAsOAuthUser(oauth2token, authParty));
    }

    if (authentication instanceof UsernamePasswordAuthenticationToken userPass) {
      return Optional.of(getAsBasicUser(userPass));
    }

    if(authentication instanceof JwtAuthenticationToken jwtToken) {
      return Optional.of(mapUserFromOidcAttributes(authParty, jwtToken.getTokenAttributes()));
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

  private static User getAsOAuthUser(final OAuth2AuthenticationToken oauth2token, final AuthorizedParty authParty) {
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
        ? mapUserFromOidcAttributes(authParty, oidcUA.getAttributes())
        : getBasicUserFallback(oauth2token);
  }

  private static User mapUserFromOidcAttributes(final AuthorizedParty authParty, final Map<String, Object> oidcUAAttributes) {
    return new User(
        String.valueOf(oidcUAAttributes.get("azp")),
        String.valueOf(oidcUAAttributes.get("sub")),
        resolveUsername(oidcUAAttributes, authParty),
        String.valueOf(oidcUAAttributes.get("name")),
        String.valueOf(oidcUAAttributes.get("given_name")),
        String.valueOf(oidcUAAttributes.get("family_name")),
        String.valueOf(oidcUAAttributes.get("email")),
        String.valueOf(oidcUAAttributes.get("picture")),
        Collections.emptyList());
  }

  private static String resolveUsername(final Map<String, Object> attributes, final AuthorizedParty authParty) {
    return authParty.getKeycloak().equalsIgnoreCase(String.valueOf(attributes.get("azp")))
        ? String.valueOf(attributes.get("preferred_username"))
        : String.valueOf(attributes.get("email"));
  }

  private static User getBasicUserFallback(final Authentication authentication) {
    return new User(null,null, authentication.getName(), null, null, null,
        null, null, Collections.emptyList());
  }
}
