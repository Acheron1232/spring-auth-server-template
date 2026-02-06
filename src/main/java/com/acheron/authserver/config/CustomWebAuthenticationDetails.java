package com.acheron.authserver.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@EqualsAndHashCode(callSuper = false)
public class CustomWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String remoteAddress;
    private final String sessionId;
    private final String verificationCode;

    public CustomWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.remoteAddress = request.getRemoteAddr();
        HttpSession session = request.getSession(false);
        this.sessionId = (session != null) ? session.getId() : null;
        this.verificationCode = request.getParameter("2facode");
    }

    @JsonCreator
    public CustomWebAuthenticationDetails(
            @JsonProperty("remoteAddress") String remoteAddress,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("verificationCode") String verificationCode) {
        super(remoteAddress, sessionId);
        this.remoteAddress = remoteAddress;
        this.sessionId = sessionId;
        this.verificationCode = verificationCode;
    }

}