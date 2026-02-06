package com.acheron.authserver.dto.request;

public record MailDto(String to, String subject, String content) {
}
