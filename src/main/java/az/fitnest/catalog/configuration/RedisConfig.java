/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonTypeInfo$As
 *  com.fasterxml.jackson.databind.DeserializationFeature
 *  com.fasterxml.jackson.databind.Module
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectMapper$DefaultTyping
 *  com.fasterxml.jackson.databind.SerializationFeature
 *  com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
 *  com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
 *  com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
 *  org.springframework.cache.CacheManager
 *  org.springframework.cache.annotation.EnableCaching
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.data.redis.cache.RedisCacheConfiguration
 *  org.springframework.data.redis.cache.RedisCacheManager
 *  org.springframework.data.redis.connection.RedisConnectionFactory
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
 *  org.springframework.data.redis.serializer.RedisSerializationContext$SerializationPair
 *  org.springframework.data.redis.serializer.RedisSerializer
 *  org.springframework.data.redis.serializer.StringRedisSerializer
 */
package az.fitnest.catalog.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(this.redisObjectMapper());
        template.setKeySerializer((RedisSerializer) new StringRedisSerializer());
        template.setValueSerializer((RedisSerializer) serializer);
        template.setHashKeySerializer((RedisSerializer) new StringRedisSerializer());
        template.setHashValueSerializer((RedisSerializer) serializer);
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10L)).serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer((RedisSerializer) new GenericJackson2JsonRedisSerializer(this.redisObjectMapper())));
        return RedisCacheManager.builder((RedisConnectionFactory) connectionFactory).cacheDefaults(config).build();
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule((Module) new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.activateDefaultTyping((PolymorphicTypeValidator) BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }
}

