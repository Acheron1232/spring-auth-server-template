package com.acheron.authserver.dto;

public record UserCreateDto(ProfileCreationDTO profile, UserCreationDto user) {
}
