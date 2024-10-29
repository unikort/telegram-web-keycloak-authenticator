package com.github.rickispp.keycloak.telegram.web.authenticator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TelegramAuthData {
    private String id;
    private String firstName;
    private String lastName;
    private String username;
    private String photoUrl;
    private String authDate;
    private String hash;
}

