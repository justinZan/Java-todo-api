package com.zading.todoapi.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zading.todoapi.config.properties.MessagingProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

/**
 * RabbitMQ 的 Exchange、Queue、Binding、死信队列和消费重试配置。
 */
@Configuration
@Profile("rabbitmq")
@EnableRabbit
public class RabbitMessagingConfig {
    @Bean
    public DirectExchange todoActionLogExchange(MessagingProperties properties) {
        return new DirectExchange(properties.rabbit().exchange());
    }

    @Bean
    public DirectExchange todoDeadLetterExchange(MessagingProperties properties) {
        return new DirectExchange(properties.rabbit().deadLetterExchange());
    }

    @Bean
    public Queue todoActionLogQueue(MessagingProperties properties) {
        MessagingProperties.Rabbit rabbit = properties.rabbit();
        return QueueBuilder.durable(rabbit.queue())
                .withArgument("x-dead-letter-exchange", rabbit.deadLetterExchange())
                .withArgument("x-dead-letter-routing-key", rabbit.deadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue todoActionLogDeadLetterQueue(MessagingProperties properties) {
        return QueueBuilder.durable(properties.rabbit().deadLetterQueue()).build();
    }

    @Bean
    public Binding todoActionLogBinding(
            Queue todoActionLogQueue,
            DirectExchange todoActionLogExchange,
            MessagingProperties properties
    ) {
        return BindingBuilder.bind(todoActionLogQueue)
                .to(todoActionLogExchange)
                .with(properties.rabbit().routingKey());
    }

    @Bean
    public Binding todoActionLogDeadLetterBinding(
            Queue todoActionLogDeadLetterQueue,
            DirectExchange todoDeadLetterExchange,
            MessagingProperties properties
    ) {
        return BindingBuilder.bind(todoActionLogDeadLetterQueue)
                .to(todoDeadLetterExchange)
                .with(properties.rabbit().deadLetterRoutingKey());
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory todoRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            MessagingProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(properties.rabbit().retryAttempts())
                .backOffOptions(properties.rabbit().retryDelayMs(), 2.0, 10_000L)
                .recoverer(rabbitRetryRecoverer())
                .build());
        return factory;
    }

    private MethodInvocationRecoverer<Object> rabbitRetryRecoverer() {
        return (arguments, cause) -> {
            throw new AmqpRejectAndDontRequeueException("RabbitMQ 消息重试耗尽", cause);
        };
    }
}
