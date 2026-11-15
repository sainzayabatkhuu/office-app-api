package com.sol.office.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office.common.event.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String USER_NOTIFICATION_QUEUE = "notification.user";
    public static final String USER_NOTIFICATION_DLQ = "notification.user.dlq";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(MessagingConstants.NOTIFICATION_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(MessagingConstants.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue userNotificationQueue() {
        return QueueBuilder.durable(USER_NOTIFICATION_QUEUE)
                .deadLetterExchange(MessagingConstants.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(USER_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue userNotificationDeadLetterQueue() {
        return QueueBuilder.durable(USER_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding userNotificationBinding() {
        return BindingBuilder.bind(userNotificationQueue()).to(notificationExchange()).with("notification.#");
    }

    @Bean
    public Binding userNotificationDeadLetterBinding() {
        return BindingBuilder.bind(userNotificationDeadLetterQueue()).to(deadLetterExchange()).with(USER_NOTIFICATION_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
