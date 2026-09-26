package com.xq.rabbitmqdemo.service;

import com.xq.rabbitmqdemo.model.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 模拟发通知（短信/邮件/站内信），演示消费者业务逻辑。
 */
@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    public void sendNotify(OrderMessage msg) {
        log.info("[模拟发通知] 用户 {} 订单 {} 已支付成功，商品：{}，金额：{} 分",
                msg.getUserId(), msg.getOrderId(), msg.getProductName(), msg.getAmount());

        // 演示「消费失败 → 自动重试 3 次 → 进死信队列」：
        // 把下面这段注释打开，下单时商品名传 "FAIL"，即可看到重试后进死信的完整链路
        // if ("FAIL".equals(msg.getProductName())) {
        //     throw new RuntimeException("模拟通知发送失败");
        // }
    }
}
