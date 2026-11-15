package com.sol.office.report.config;

import com.sol.office.common.event.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitConfig {

    public static final String EXECUTION_QUEUE = "report.execution";
    public static final String EXECUTION_DLQ = "report.execution.dlq";
    public static final String EXECUTION_LISTENER_FACTORY = "reportExecutionListenerFactory";

    @Bean
    public TopicExchange reportExchange() {
        return new TopicExchange(MessagingConstants.REPORT_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(MessagingConstants.NOTIFICATION_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(MessagingConstants.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue reportExecutionQueue() {
        return QueueBuilder.durable(EXECUTION_QUEUE)
                .deadLetterExchange(MessagingConstants.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(EXECUTION_DLQ)
                .build();
    }

    @Bean
    public Queue reportExecutionDeadLetterQueue() {
        return QueueBuilder.durable(EXECUTION_DLQ).build();
    }

    @Bean
    public Binding reportExecutionBinding() {
        return BindingBuilder.bind(reportExecutionQueue()).to(reportExchange())
                .with(MessagingConstants.REPORT_EXECUTION_ROUTING_KEY);
    }

    @Bean
    public Binding reportExecutionDeadLetterBinding() {
        return BindingBuilder.bind(reportExecutionDeadLetterQueue()).to(deadLetterExchange()).with(EXECUTION_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Consumers per instance = max concurrent fills, prefetch 1 so a busy instance doesn't hoard queued work
     * that an idle instance could take.
     */
    @Bean(EXECUTION_LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory reportExecutionListenerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            ReportProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setConcurrentConsumers(properties.execution().maxConcurrent());
        factory.setMaxConcurrentConsumers(properties.execution().maxConcurrent());
        factory.setPrefetchCount(1);
        return factory;
    }
}
