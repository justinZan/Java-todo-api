package com.zading.todoapi.messaging;

import com.zading.todoapi.config.properties.RedisProtectionProperties;
import com.zading.todoapi.redis.IdempotencyClaim;
import com.zading.todoapi.redis.IdempotencyStore;
import com.zading.todoapi.service.TodoActionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 消息的统一业务处理器。
 *
 * <p>Kafka 和 RabbitMQ 的消费适配器都调用这个类。这里集中处理重复消费、
 * 业务执行成功、失败释放和完成状态保存，避免两套消费者复制业务逻辑。</p>
 */
@Service
public class TodoActionLogMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(TodoActionLogMessageHandler.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "message:todo-action-log:";
    private static final String ACKED_RESULT = "ACKED";

    private final TodoActionLogService todoActionLogService;
    private final IdempotencyStore idempotencyStore;
    private final RedisProtectionProperties redisProperties;

    public TodoActionLogMessageHandler(
            TodoActionLogService todoActionLogService,
            IdempotencyStore idempotencyStore,
            RedisProtectionProperties redisProperties
    ) {
        this.todoActionLogService = todoActionLogService;
        this.idempotencyStore = idempotencyStore;
        this.redisProperties = redisProperties;
    }

    public void handle(TodoActionLogMessage message) {
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + message.messageId();
        IdempotencyClaim claim = idempotencyStore.tryClaim(
                idempotencyKey,
                redisProperties.idempotencyTtl()
        );

        if (claim.status() != IdempotencyClaim.Status.CLAIMED) {
            log.info("忽略重复或正在处理的消息，messageId={}, status={}", message.messageId(), claim.status());
            return;
        }

        try {
            todoActionLogService.record(message.toEvent());
            boolean completed = idempotencyStore.complete(
                    idempotencyKey,
                    claim.ownerToken(),
                    ACKED_RESULT,
                    redisProperties.idempotencyTtl()
            );

            if (!completed) {
                log.warn("消息幂等完成状态写入失败，messageId={}", message.messageId());
            }
        } catch (RuntimeException ex) {
            idempotencyStore.release(idempotencyKey, claim.ownerToken());
            throw ex;
        }
    }
}
