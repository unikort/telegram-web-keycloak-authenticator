package com.github.rickispp.keycloak.telegram.web.authenticator;

import com.github.rickispp.keycloak.telegram.web.authenticator.model.AuthParameter;
import com.github.rickispp.keycloak.telegram.web.authenticator.model.TelegramAuthData;
import com.github.rickispp.keycloak.telegram.web.authenticator.model.TelegramWebAuthenticatorConfig;
import com.github.rickispp.keycloak.telegram.web.authenticator.validator.TelegramAuthDataValidator;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.utils.StringUtil;

import java.util.UUID;

public class TelegramWebLoginWidgetAuthenticator implements Authenticator {

    private static final Logger LOGGER = Logger.getLogger(TelegramWebLoginWidgetAuthenticator.class);

    public static final String TELEGRAM_BOT_USERNAME_FORM_ATTRIBUTE_NAME = "telegram_bot_username";
    public static final String TELEGRAM_REDIRECT_URI_FORM_ATTRIBUTE_NAME = "telegram_redirect_uri";

    public static final String TG_USERNAME_ATTRIBUTE_NAME = "telegram_username";
    public static final String TG_USER_ID_ATTRIBUTE_NAME = "telegram_user_id";
    public static final String TG_USER_PHOTO_URL_ATTRIBUTE_NAME = "telegram_photo_url";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.attempted();

        TelegramWebAuthenticatorConfig authenticatorConfig;
        try {
            authenticatorConfig = getAuthenticatorConfig(context);
        } catch (Exception ex) {
            LOGGER.error("Invalid authenticator config!", ex);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        addFormAttributes(context, authenticatorConfig);

        TelegramAuthData telegramAuthData = getTelegramAuthData(context.getHttpRequest());
        if (telegramAuthData == null) {
            return;
        }

        UserModel user;
        TelegramAuthDataValidator telegramAuthDataValidator = new TelegramAuthDataValidator(authenticatorConfig, telegramAuthData);
        if (!telegramAuthDataValidator.isValid() || (user = getOrCreateUser(context, telegramAuthData)) == null) {
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            return;
        }

        context.setUser(user);
        context.success();
    }

    private TelegramWebAuthenticatorConfig getAuthenticatorConfig(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();

        if (authenticatorConfig == null || authenticatorConfig.getConfig() == null) {
            throw new IllegalArgumentException(TelegramWebLoginAuthenticatorFactory.PROVIDER_ID + " not configured! Please, specify all required params.");
        }

        String botToken = authenticatorConfig.getConfig().get(TelegramWebLoginAuthenticatorFactory.BOT_TOKEN_CONFIG_NAME);
        if (StringUtil.isBlank(botToken)) {
            throw new IllegalArgumentException(TelegramWebLoginAuthenticatorFactory.BOT_TOKEN_CONFIG_NAME + " not configured!");
        }

        String botUsername = authenticatorConfig.getConfig().get(TelegramWebLoginAuthenticatorFactory.BOT_USERNAME_CONFIG_NAME);
        if (StringUtil.isBlank(botUsername)) {
            throw new IllegalArgumentException(TelegramWebLoginAuthenticatorFactory.BOT_USERNAME_CONFIG_NAME + " not configured!");
        }

        String rawAuthTimeDelta = authenticatorConfig.getConfig().get(TelegramWebLoginAuthenticatorFactory.AUTH_TIME_DELTA_CONFIG_NAME);
        if (StringUtil.isBlank(rawAuthTimeDelta)) {
            throw new IllegalArgumentException(TelegramWebLoginAuthenticatorFactory.AUTH_TIME_DELTA_CONFIG_NAME + " not configured!");
        }

        long authTimeDelta = Long.parseLong(rawAuthTimeDelta);
        if (authTimeDelta <= 0) {
            throw new IllegalArgumentException(TelegramWebLoginAuthenticatorFactory.AUTH_TIME_DELTA_CONFIG_NAME + " must be greater than 0");
        }

        return TelegramWebAuthenticatorConfig.builder()
                .botUsername(botUsername)
                .botToken(botToken)
                .authTimeDelta(authTimeDelta)
                .build();
    }

    private TelegramAuthData getTelegramAuthData(HttpRequest request) {
        MultivaluedMap<String, String> parameters;

        MultivaluedMap<String, String> queryParameters = request.getUri().getQueryParameters(); // for browser flow
        MultivaluedMap<String, String> formParameters = request.getDecodedFormParameters(); // for direct grant flow
        if (queryParameters.keySet().containsAll(AuthParameter.requiredParameters)) {
            parameters = queryParameters;
        } else if (formParameters.keySet().containsAll(AuthParameter.requiredParameters)) {
            parameters = formParameters;
        } else {
            return null;
        }

        return TelegramAuthData.builder()
                .id(parameters.getFirst(AuthParameter.ID_FIELD_NAME.queryName))
                .firstName(parameters.getFirst(AuthParameter.FIRST_NAME_FIELD_NAME.queryName))
                .lastName(parameters.getFirst(AuthParameter.LAST_NAME_FIELD_NAME.queryName))
                .username(parameters.getFirst(AuthParameter.USERNAME_FIELD_NAME.queryName))
                .photoUrl(parameters.getFirst(AuthParameter.PHOTO_URL_FIELD_NAME.queryName))
                .authDate(parameters.getFirst(AuthParameter.AUTH_DATE_FIELD_NAME.queryName))
                .hash(parameters.getFirst(AuthParameter.HASH_FIELD_NAME.queryName))
                .build();
    }

    private UserModel getOrCreateUser(AuthenticationFlowContext context, TelegramAuthData telegramAuthData) {
        UserProvider userProvider = context.getSession().users();
        RealmModel realm = context.getRealm();
        return userProvider.searchForUserByUserAttributeStream(realm, TG_USER_ID_ATTRIBUTE_NAME, telegramAuthData.getId())
                .findFirst()
                .orElseGet(() -> createNewUser(realm, userProvider, telegramAuthData));

    }

    private UserModel createNewUser(RealmModel realm, UserProvider userProvider, TelegramAuthData telegramAuthData) {
        if (!realm.isRegistrationAllowed()) {
            LOGGER.info("Can't create new telegram authenticated user! User registration is not allowed.");
            return null;
        }

        String username = StringUtil.isNotBlank(telegramAuthData.getUsername()) ? telegramAuthData.getUsername() : UUID.randomUUID().toString();
        UserModel user = userProvider.addUser(realm, username);
        user.setEnabled(true);
        user.setFirstName(telegramAuthData.getFirstName());
        user.setLastName(telegramAuthData.getLastName());
        user.setSingleAttribute(TG_USER_ID_ATTRIBUTE_NAME, telegramAuthData.getId());
        user.setSingleAttribute(TG_USERNAME_ATTRIBUTE_NAME, telegramAuthData.getUsername());
        user.setSingleAttribute(TG_USER_PHOTO_URL_ATTRIBUTE_NAME, telegramAuthData.getPhotoUrl());

        return user;
    }

    private void addFormAttributes(AuthenticationFlowContext context, TelegramWebAuthenticatorConfig authenticatorConfig) {
        context.form().setAttribute(TELEGRAM_BOT_USERNAME_FORM_ATTRIBUTE_NAME, authenticatorConfig.getBotUsername());
        context.form().setAttribute(TELEGRAM_REDIRECT_URI_FORM_ATTRIBUTE_NAME, getSanitizedRedirectUri(context));
    }

    private String getSanitizedRedirectUri(AuthenticationFlowContext context) {
        UriBuilder redirectUriBuilder = UriBuilder.fromUri(context.getHttpRequest().getUri().getRequestUri());
        for (AuthParameter param : AuthParameter.values()) {
            redirectUriBuilder.replaceQueryParam(param.queryName, null);
        }
        return redirectUriBuilder.build().toString();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // no-op
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
