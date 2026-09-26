package com.xq.rabbitmqdemo.model;

import java.io.Serializable;

/**
 * 订单消息体：下单后投递给 MQ 的消息。
 * 用普通 POJO 而非 record，保证 Jackson 反序列化零边界问题，也方便讲清序列化。
 */
public class OrderMessage implements Serializable {

    private String orderId;      // 订单号（幂等去重的唯一键）
    private String userId;       // 用户
    private String productName;  // 商品
    private long amount;         // 金额（分）
    private long createTime;     // 创建时间戳

    public OrderMessage() {
    }

    public OrderMessage(String orderId, String userId, String productName,
                        long amount, long createTime) {
        this.orderId = orderId;
        this.userId = userId;
        this.productName = productName;
        this.amount = amount;
        this.createTime = createTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "OrderMessage{orderId='" + orderId + "', userId='" + userId
                + "', productName='" + productName + "', amount=" + amount + "}";
    }
}
