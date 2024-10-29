package com.github.rickispp.keycloak.telegram.web.authenticator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TelegramWebAuthenticatorConfig {
    private String botUsername;
    private String botToken;
    private Long authTimeDelta;
}
