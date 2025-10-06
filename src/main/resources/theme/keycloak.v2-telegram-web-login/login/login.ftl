<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<#import "social-providers.ftl" as identityProviders>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
<!-- template: login.ftl -->
    <#if section = "header">
        ${msg("Войти с помощью")}

    <#elseif section = "socialProviders" >
        <#if realm.password && ((social.providers?? && social.providers?has_content) || (telegram_bot_username?? && telegram_redirect_uri??))>
            <@identityProviders.show social=social/>
        </#if>
    </#if>

</@layout.registrationLayout>
