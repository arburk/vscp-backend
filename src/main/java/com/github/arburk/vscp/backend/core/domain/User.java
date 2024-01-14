package com.github.arburk.vscp.backend.core.domain;

import java.util.List;

public record User(

    String authorizedParty,
    String subject,
    String userName,
    String name,
    String givenName,
    String familyName,
    String email,
    String pictureUrl,
    List<String> roles) {
}
