package com.example.demo.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitReceiver implements InitializingBean {
    private final AmqpAdmin amqpAdmin;

    @Override
    public void afterPropertiesSet() {
        amqpAdmin.declareQueue(new Queue("testQueue"));
        // 声明一个支持优先级的队列，x-max-priority=10，数值越大越优先
        amqpAdmin.declareQueue(QueueBuilder.durable("testPriorityQueue")
                .maxPriority(10)
                .build());
    }

    @RabbitListener(queues = "testQueue")
    public void receiver(String msg) {
        log.info("Received message: {}", msg);
    }

    @RabbitListener(id = "priorityListener", queues = "testPriorityQueue", autoStartup = "false")
    public void receiverPriority(String msg, Message message) {
        log.info("Received priority message: priority={}, msg={}",
                message.getMessageProperties().getPriority(), msg);
    }
}
