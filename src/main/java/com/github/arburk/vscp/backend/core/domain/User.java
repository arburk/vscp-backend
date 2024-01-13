package com.github.arburk.vscp.backend.core.domain;

import java.util.List;

public record User(
    String userId,
    String name,
    String givenName,
    String familyName,
    String email,
    String pictureUrl,
    List<String> roles) {
}
