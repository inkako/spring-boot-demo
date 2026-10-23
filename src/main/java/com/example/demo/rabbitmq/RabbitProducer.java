package com.example.demo.rabbitmq;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitProducer {
    private final AmqpTemplate amqpTemplate;

    public RabbitProducer(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void sendMsg(String queue, String message) {
        this.amqpTemplate.convertAndSend(queue, message);
    }

    /**
     * 发送带优先级的消息。priority 取值范围 0~255（需不超过队列声明时的 x-max-priority），
     * 数值越大优先级越高，会被优先消费。
     */
    public void sendMsg(String queue, String message, final int priority) {
        this.amqpTemplate.convertAndSend(queue, message, msg -> {
            msg.getMessageProperties().setPriority(priority);
            return msg;
        });
    }
}
