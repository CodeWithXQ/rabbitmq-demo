package com.xq.rabbitmqdemo.producer;

import com.xq.rabbitmqdemo.config.RabbitConfig;
import com.xq.rabbitmqdemo.model.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 生产者：发送订单消息。
 * 可靠性关键点：Publisher Confirm —— 通过 CorrelationData 的 Future 回调确认
 * 消息是否真正到达 Broker（保证生产端消息不丢）。
 */
@Component
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单通知（异步削峰：下单接口同步返回，通知走这里异步处理）。
     */
    public void sendNotify(OrderMessage msg) {
        CorrelationData correlationData = new CorrelationData(msg.getOrderId());
        correlationData.getFuture().whenComplete((ack, throwable) -> {
            if (throwable != null) {
                log.error("消息发送异常 orderId={}", msg.getOrderId(), throwable);
            } else if (ack != null && ack.isAck()) {
                log.info("消息已确认到达 Broker orderId={}", msg.getOrderId());
            } else {
                log.warn("消息被 Broker Nack（未到达）orderId={}", msg.getOrderId());
            }
        });
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_NOTIFY_ROUTING_KEY,
                msg,
                correlationData);
    }

    /**
     * 发送延迟消息：进延迟队列（TTL 30s），30 秒后过期进死信，触发"未支付自动取消"。
     */
    public void sendDelayCancel(OrderMessage msg) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_DELAY_ROUTING_KEY,
                msg);
        log.info("已投递延迟取消消息，30 秒后过期 orderId={}", msg.getOrderId());
    }
}
