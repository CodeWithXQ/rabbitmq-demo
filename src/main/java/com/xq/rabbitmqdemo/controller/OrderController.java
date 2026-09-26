package com.xq.rabbitmqdemo.controller;

import com.xq.rabbitmqdemo.model.OrderMessage;
import com.xq.rabbitmqdemo.producer.OrderProducer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 下单接口：演示 MQ 的「同步秒回 + 异步处理」削峰解耦。
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderProducer orderProducer;

    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    /**
     * 下单：接口立即返回，通知走 MQ 异步处理（削峰解耦）。
     */
    @PostMapping
    public String createOrder(@RequestBody OrderMessage order) {
        String orderId = "O" + UUID.randomUUID().toString().substring(0, 8);
        order.setOrderId(orderId);
        order.setCreateTime(System.currentTimeMillis());
        orderProducer.sendNotify(order);
        return "下单成功（通知异步处理中），订单号：" + orderId;
    }

    /**
     * 下单 + 延迟取消：30 秒未支付自动取消（延迟队列）。
     */
    @PostMapping("/delay")
    public String createOrderWithDelayCancel(@RequestBody OrderMessage order) {
        String orderId = "O" + UUID.randomUUID().toString().substring(0, 8);
        order.setOrderId(orderId);
        order.setCreateTime(System.currentTimeMillis());
        orderProducer.sendDelayCancel(order);
        return "下单成功，30 秒未支付将自动取消，订单号：" + orderId;
    }
}
