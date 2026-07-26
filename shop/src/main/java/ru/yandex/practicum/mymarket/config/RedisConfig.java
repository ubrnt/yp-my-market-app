package ru.yandex.practicum.mymarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import ru.yandex.practicum.mymarket.domain.Item;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Item> itemRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, Item> context = RedisSerializationContext
                .<String, Item>newSerializationContext(RedisSerializer.string())
                .value(new JacksonJsonRedisSerializer<>(Item.class))
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
