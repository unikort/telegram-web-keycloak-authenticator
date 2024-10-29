package com.github.rickispp.keycloak.telegram.web.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class TelegramWebLoginAuthenticatorFactory implements AuthenticatorFactory {

    public static final String BOT_USERNAME_CONFIG_NAME = "telegram_bot_username";
    public static final String BOT_TOKEN_CONFIG_NAME = "telegram_bot_token";
    public static final String AUTH_TIME_DELTA_CONFIG_NAME = "telegram_auth_time_delta";
    public static final String PROVIDER_ID = "telegram-web-login-authenticator";

    public static final long DEFAULT_AUTH_TIME_DELTA = 60;

    @Override
    public String getDisplayType() {
        return "Telegram Web Login Widget";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.CONDITIONAL,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Authenticator that allows you to login using Telegram Web Login Widget";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(BOT_TOKEN_CONFIG_NAME)
                .label("Bot Token")
                .helpText("Enter the token of your Telegram bot.")
                .type(ProviderConfigProperty.PASSWORD)
                .secret(true)
                .required(true)
                .add()
                .property()
                .name(BOT_USERNAME_CONFIG_NAME)
                .label("Bot username")
                .helpText("Enter the username of your Telegram bot.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(false)
                .required(true)
                .add()
                .property()
                .name(AUTH_TIME_DELTA_CONFIG_NAME)
                .label("Auth time delta (in seconds)")
                .helpText("""
                        The maximum delta (in seconds) between the time of successful authorization in Telegram ('auth_date' parameter) and the time of receive the authorization request in Keycloak.
                        After this time, request will be considered expired, even if it contains valid data.
                        This is necessary to avoid reuse of authorization data in case of leakage.
                        """)
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(false)
                .required(true)
                .defaultValue(DEFAULT_AUTH_TIME_DELTA)
                .add()
                .build();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new TelegramWebLoginWidgetAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
