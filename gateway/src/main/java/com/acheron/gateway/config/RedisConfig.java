package com.acheron.gateway.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.jackson.OAuth2ClientJacksonModule;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 2592000)
public class RedisConfig {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
    public abstract static class OAuth2AuthorizationRequestMixin {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
    public abstract static class OAuth2AuthorizedClientMixin {
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJacksonJsonRedisSerializer(objectMapper());
    }

    private ObjectMapper objectMapper() {
        ClassLoader loader = this.getClass().getClassLoader();

        BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .allowIfSubType("java.net.URL");

        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(loader, builder))
                .addModule(new OAuth2ClientJacksonModule())
                .addMixIn(OAuth2AuthorizationRequest.class, OAuth2AuthorizationRequestMixin.class)
                .addMixIn(OAuth2AuthorizedClient.class, OAuth2AuthorizedClientMixin.class)
                .activateDefaultTyping(builder.build(), DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();
    }

    @Bean
    @Primary
    public ReactiveRedisSessionRepository reactiveRedisSessionRepository(
            ReactiveRedisConnectionFactory factory,
            RedisSerializer<Object> sessionRedisSerializer) {

        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .value(sessionRedisSerializer)
                .hashKey(new StringRedisSerializer())
                .hashValue(sessionRedisSerializer)
                .build();

        ReactiveRedisTemplate<String, Object> sessionRedisTemplate =
                new ReactiveRedisTemplate<>(factory, serializationContext);

        ReactiveRedisSessionRepository repository =
                new ReactiveRedisSessionRepository(sessionRedisTemplate);

        repository.setRedisKeyNamespace("spring:session");
        repository.setDefaultMaxInactiveInterval(Duration.ofSeconds(1800));

        return repository;
    }

    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }
}