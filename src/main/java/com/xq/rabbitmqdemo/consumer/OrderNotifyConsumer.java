package com.xq.rabbitmqdemo.consumer;

import com.rabbitmq.client.Channel;
import com.xq.rabbitmqdemo.config.RabbitConfig;
import com.xq.rabbitmqdemo.model.OrderMessage;
import com.xq.rabbitmqdemo.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * 通知队列消费者。
 * 三个可靠性点集中在这里：
 *  1. 手动 ACK：处理成功才 basicAck，失败不确认
 *  2. 幂等：Redis 记录已处理的 orderId，重复投递直接丢弃
 *  3. 失败重试 + 死信：抛异常触发 Spring retry（3 次），耗尽后进死信队列
 */
@Component
public class OrderNotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifyConsumer.class);

    private final NotifyService notifyService;
    private final StringRedisTemplate redisTemplate;

    public OrderNotifyConsumer(NotifyService notifyService, StringRedisTemplate redisTemplate) {
        this.notifyService = notifyService;
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_NOTIFY_QUEUE)
    public void onNotify(OrderMessage msg, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String idempotentKey = "order:processed:" + msg.getOrderId();
        try {
            // 幂等：已处理过（说明是重复投递），直接确认丢弃
            if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotentKey))) {
                log.info("重复消息，直接确认丢弃 orderId={}", msg.getOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 业务处理（模拟发通知）
            notifyService.sendNotify(msg);
            // 处理成功后才打幂等标记（失败不打标记，允许重试再次处理）
            redisTemplate.opsForValue().set(idempotentKey, "1", Duration.ofHours(24));
            // 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("通知处理成功 orderId={}", msg.getOrderId());
        } catch (Exception e) {
            // 抛运行时异常交给 Spring retry：重试 3 次，耗尽后 reject 进死信队列
            log.error("通知处理失败，等待重试 orderId={}", msg.getOrderId(), e);
            throw new RuntimeException("通知处理失败", e);
        }
    }
}
