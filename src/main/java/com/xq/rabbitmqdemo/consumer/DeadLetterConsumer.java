package com.xq.rabbitmqdemo.consumer;

import com.xq.rabbitmqdemo.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者。
 * 两类消息最终到这里：
 *  1. order.notify.dlq    —— 通知处理失败、重试耗尽
 *  2. order.delay.expired —— 订单 30s 未支付自动取消
 * 生产上这里接：异常落库 / 告警 / 人工补偿。
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = RabbitConfig.DLX_QUEUE)
    public void onDeadLetter(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody());
        if (RabbitConfig.DELAY_EXPIRED_KEY.equals(routingKey)) {
            log.warn("[死信-订单超时] 30 秒未支付，自动取消订单，body={}", body);
        } else if (RabbitConfig.NOTIFY_DLQ_KEY.equals(routingKey)) {
            log.error("[死信-通知失败] 通知处理重试耗尽，需人工补偿，body={}", body);
        } else {
            log.error("[死信] 未知来源，routingKey={}, body={}", routingKey, body);
        }
    }
}
