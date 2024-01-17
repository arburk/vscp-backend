package com.github.arburk.vscp.backend.config.api;

import com.github.arburk.vscp.backend.core.services.UserInfoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // allows @PreAuthorize checks
public class WebConfiguration implements WebMvcConfigurer {

  private final UserInfoService userInfoService;

  public WebConfiguration(final UserInfoService userInfoService) {
    this.userInfoService = userInfoService;
  }

  @Profile("!dev")
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return configureDefaults(http).build();
  }

  @Profile("dev")
  @Bean
  public SecurityFilterChain securityFilterChainDev(HttpSecurity http) throws Exception {
    return configureDefaults(http).csrf(AbstractHttpConfigurer::disable).build();
  }

  private HttpSecurity configureDefaults(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests((authorize) -> authorize
            .requestMatchers("/login").permitAll()
            .anyRequest().authenticated()
        )
        .cors(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults())
        .oauth2Client(Customizer.withDefaults())
        .oauth2Login(login -> login.setBuilder(http))
        .oauth2ResourceServer(configure ->
            configure.jwt(customizer ->
            {
              JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
              converter.setJwtGrantedAuthoritiesConverter(userInfoService);
              customizer.jwtAuthenticationConverter(converter);
            }));
    return http;
  }


  @Bean
  public UserDetailsService users() {
    // The builder will ensure the passwords are encoded before saving in memory
    User.UserBuilder users = User.withDefaultPasswordEncoder(); // for demo only
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

}
