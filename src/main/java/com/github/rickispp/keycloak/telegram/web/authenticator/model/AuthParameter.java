package com.github.rickispp.keycloak.telegram.web.authenticator.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AuthParameter {
    ID_FIELD_NAME("id", true),
    FIRST_NAME_FIELD_NAME("first_name", false),
    LAST_NAME_FIELD_NAME("last_name", false),
    USERNAME_FIELD_NAME("username", false),
    PHOTO_URL_FIELD_NAME("photo_url", false),
    AUTH_DATE_FIELD_NAME("auth_date", true),
    HASH_FIELD_NAME("hash", true);

    public static final Set<String> requiredParameters = Arrays.stream(values())
            .filter(p -> p.required)
            .map(p -> p.queryName)
            .collect(Collectors.toSet());

    public final String queryName;
    public final boolean required;

    AuthParameter(String queryName, boolean required) {
        this.queryName = queryName;
        this.required = required;
    }
}
