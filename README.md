# RabbitMQ 异步削峰 Demo

> 补 Java 岗「消息队列」短板的学习 demo，场景挂靠"订单异步处理"，覆盖**消息可靠性（Confirm / 手动 ACK / 持久化）、幂等、死信队列、延迟队列**。
> 已在本机 `java -jar` 方式启动通过（2026-09-26）。

## 场景

- 下单接口**同步秒回**，通知 / 统计走 **MQ 异步处理** —— 演示「削峰 + 解耦」。
- 另一个接口演示「30 秒未支付自动取消」—— 延迟队列（TTL + 死信实现）。

## 拓扑

```
order.exchange (topic)
  ├─ order.notify.queue  ──消费失败重试耗尽──▶ 死信(order.notify.dlq)
  └─ order.delay.queue   ──TTL 30s 过期──────▶ 死信(order.delay.expired)
order.dlx.exchange ──▶ order.dlx.queue（DeadLetterConsumer 按 routingKey 区分）
```

## 环境要求

| 组件 | 要求 | 本机参考 |
|---|---|---|
| JDK | **21**（项目 release 21 编译，运行必须 JDK21）| `D:\AppGallery\Downloads\IDEA\jdk21` |
| Maven | 3.9+ | 已装，用的 JDK21 |
| Redis | 运行中（默认 localhost:6379）| 已装 |
| RabbitMQ | 3.x（Docker 跑）| 需 `docker run` 启动 |
| Docker Desktop | 已装 | 需手动启动 |

## ⚠️ 三个必看的坑（先看，省时间）

1. **不要用 `mvn spring-boot:run`**：项目路径含中文（`D:\人生目标生命体验\求职\`），`spring-boot:run` 会 fork 子 JVM，中文路径在命令行编码转换时乱码，报 `ClassNotFoundException`。改用 `mvn clean package` + `java -jar`。
2. **`java -jar` 必须用 JDK21 完整路径**：系统默认 `java` 是 1.8，用它跑 release 21 的包会报错。用 `D:\AppGallery\Downloads\IDEA\jdk21\bin\java`（按你机器实际 JDK21 路径改）。
3. **端口默认 9090**：8080/8081 常被其他项目（音乐后台/失物招领）占用，本项目默认 9090。

## 完整启动步骤（Windows + PowerShell）

### Step 1：启动 RabbitMQ

先手动打开 Docker Desktop，等它启动完成，再执行：

```powershell
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

验证：浏览器打开 http://localhost:15672 （账号 `guest` / 密码 `guest`）。

> 如果之前已经 `docker run` 过一次，容器已存在，下次直接 `docker start rabbitmq` 即可。

### Step 2：打包

```powershell
cd D:\人生目标生命体验\求职\rabbitmq-demo
mvn clean package
```

看到 `BUILD SUCCESS` 再继续。

### Step 3：运行

```powershell
D:\AppGallery\Downloads\IDEA\jdk21\bin\java -jar target\rabbitmq-demo-1.0.0.jar
```

看到 `Started RabbitmqDemoApplication` 即启动成功。

### Step 4：测试下单

PowerShell 里 `curl` 是 `Invoke-WebRequest` 的**别名**，不支持 `-d` 参数，要用 `curl.exe` 或 `Invoke-RestMethod`：

```powershell
# 方式一：Invoke-RestMethod（PowerShell 原生，推荐）
Invoke-RestMethod -Uri http://localhost:9090/order -Method Post -ContentType "application/json" -Body '{"userId":"u1","productName":"蓝牙耳机","amount":19900}'

# 方式二：curl.exe（真 curl）
curl.exe -X POST http://localhost:9090/order -H "Content-Type: application/json" -d "{\"userId\":\"u1\",\"productName\":\"蓝牙耳机\",\"amount\":19900}"
```

**期望结果**：
- 接口**秒回**：`下单成功（通知异步处理中），订单号：Oxxxxxxxx`
- 应用控制台随后打印：`[模拟发通知] 用户 u1 订单 Oxxxxxxxx 已支付成功，商品：蓝牙耳机...`

> 这就是「同步秒回 + 异步处理」的削峰解耦：下单接口不阻塞等待通知，立刻返回。

## 延迟队列（30s 未支付自动取消）

```powershell
Invoke-RestMethod -Uri http://localhost:9090/order/delay -Method Post -ContentType "application/json" -Body '{"userId":"u1","productName":"机械键盘","amount":49900}'
```

30 秒后控制台打印：`[死信-订单超时] 30 秒未支付，自动取消订单...`

## 演示「消费失败 → 重试 3 次 → 进死信」

1. 打开 `src/main/java/com/xq/rabbitmqdemo/service/NotifyService.java`，取消注释模拟失败那段（`productName == "FAIL"` 时抛异常）；
2. 重新 `mvn clean package` 并运行；
3. 下单时商品名传 `FAIL`；
4. 观察控制台：失败 → 重试 3 次（间隔 1s、2s）→ 进死信队列 → 打印 `[死信-通知失败] 通知处理重试耗尽，需人工补偿...`。

## 幂等说明

- 用 Redis `order:processed:{orderId}` 记录已处理，24h 过期。
- 处理**成功后才打标记**：失败的消息不打标记，重试时能再次处理，不会被幂等误判成重复。
- 生产上幂等完整做法：消息带全局唯一业务 ID + Redis/DB 去重 + 数据库唯一索引兜底。

## 诚实边界

- 这是**独立学习 demo**，**不是**集成到音乐后台里的。
- 延迟队列用的是**队列级 TTL**（所有消息统一 30s）；要不同延迟需每消息 TTL 或 `rabbitmq_delayed_message_exchange` 插件。
