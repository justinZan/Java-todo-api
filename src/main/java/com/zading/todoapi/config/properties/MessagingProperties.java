package com.zading.todoapi.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 第 27 周消息队列配置。
 *
 * <p>默认环境使用内存消息发布器；启用 kafka 或 rabbitmq profile 后，
 * 对应的连接配置和消息拓扑会生效。</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        @NotNull @Valid Kafka kafka,
        @NotNull @Valid Rabbit rabbit
) {
    public record Kafka(
            @NotBlank String topic,
            @NotBlank String consumerGroup,
            @Min(1) int partitions,
            @Min(1) int replicas
    ) {
    }

    public record Rabbit(
            @NotBlank String exchange,
            @NotBlank String queue,
            @NotBlank String routingKey,
            @NotBlank String deadLetterExchange,
            @NotBlank String deadLetterQueue,
            @NotBlank String deadLetterRoutingKey,
            @Min(1) int retryAttempts,
            @Min(0) long retryDelayMs
    ) {
    }
}
