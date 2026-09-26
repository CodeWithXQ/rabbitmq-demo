package com.xq.rabbitmqdemo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 交换机 / 队列 / 绑定 / 死信 / 延迟队列 统一配置。
 *
 * 拓扑：
 *   order.exchange (topic)
 *     ├─ order.notify.queue   （通知队列，消费失败 → 死信）
 *     └─ order.delay.queue    （延迟队列，TTL 30s 过期 → 死信）
 *   order.dlx.exchange (topic)
 *     └─ order.dlx.queue      （死信队列，接收两类死信：通知失败 / 订单超时）
 */
@Configuration
public class RabbitConfig {

    // 业务交换机与队列
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_NOTIFY_QUEUE = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTING_KEY = "order.notify";

    // 延迟队列（TTL + 死信实现延迟）
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    // 死信交换机与队列
    public static final String DLX_EXCHANGE = "order.dlx.exchange";
    public static final String DLX_QUEUE = "order.dlx.queue";
    public static final String NOTIFY_DLQ_KEY = "order.notify.dlq";     // 通知处理失败进死信
    public static final String DELAY_EXPIRED_KEY = "order.delay.expired"; // 延迟过期进死信

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    // 通知队列：durable + 绑定死信交换机（消费失败重试耗尽后进死信）
    @Bean
    public Queue orderNotifyQueue() {
        return QueueBuilder.durable(ORDER_NOTIFY_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(NOTIFY_DLQ_KEY)
                .build();
    }

    // 延迟队列：队列级 TTL 30 秒，过期后消息进死信交换机（延迟队列经典实现）
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .ttl(30_000)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DELAY_EXPIRED_KEY)
                .build();
    }

    // 死信队列
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(orderNotifyQueue()).to(orderExchange())
                .with(ORDER_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    // 死信队列用通配路由键接收两类死信（通知失败 + 延迟过期）
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("order.#");
    }

    // 消息序列化用 JSON（默认是 JDK 序列化，不可读、跨语言差）
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
