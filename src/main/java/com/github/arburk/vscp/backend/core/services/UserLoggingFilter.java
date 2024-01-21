package com.github.arburk.vscp.backend.core.services;

import com.github.arburk.vscp.backend.core.domain.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserLoggingFilter implements Filter {

  private final UserInfoService userInfoService;

  public UserLoggingFilter(final UserInfoService userInfoService) {
    this.userInfoService = userInfoService;
  }

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
    userInfoService.getAsUser().ifPresent(user -> {
      log.info(user.toString());
      userInfoService.getUserRepo().add(user);
    });
    log.info(userInfoService.getUserRepo().stream().map(User::userName).collect(Collectors.joining(", ")));

    filterChain.doFilter(servletRequest, servletResponse);
  }
}
