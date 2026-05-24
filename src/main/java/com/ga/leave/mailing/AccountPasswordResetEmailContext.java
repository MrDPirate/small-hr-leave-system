package com.ga.leave.mailing;

import com.ga.leave.model.User;
import org.springframework.web.util.UriComponentsBuilder;

public class AccountPasswordResetEmailContext extends AbstractEmailContext {
    private String token;

    @Override
    public <T> void init(T context){
        User user = (User) context;
        put("username", user.getUserName());
        setTemplateLocation("mailing/password-reset");
        setSubject("Reset your password");
        //setFrom("no-reply@kttpro.com");
        setTo(user.getEmailAddress());
    }

    public void setToken(String token) {
        this.token = token;
        put("token", token);
    }

    public void buildResetUrl(final String baseURL, final String token){
        final String url= UriComponentsBuilder.fromUriString(baseURL)
                .path("/api/v1/auth/reset-password").queryParam("token", token).toUriString();
        put("resetURL", url);
    }
}