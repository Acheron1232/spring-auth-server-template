package com.acheron.authserver.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
@EnableCaching
public class RedisConfig {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
    public abstract static class SecurityPrincipalMixin {
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJacksonJsonRedisSerializer(buildObjectMapper());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(springSessionDefaultRedisSerializer());
        template.setHashValueSerializer(springSessionDefaultRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    private tools.jackson.databind.ObjectMapper buildObjectMapper() {
        ClassLoader loader = this.getClass().getClassLoader();

        BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .allowIfSubType("java.net.URL");

        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(loader, builder))
                .activateDefaultTyping(builder.build(), DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();
    }
}
