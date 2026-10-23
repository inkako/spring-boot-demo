package com.example.demo.modules.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("test")
public class TestController {
    private final RabbitProducer rabbitProducer;
    private final ObjectProvider<RabbitListenerEndpointRegistry> registryProvider;

    @GetMapping("hello")
    public String hello() {
        String msg = "hello world" + System.currentTimeMillis();
        rabbitProducer.sendMsg("testQueue", msg);
        log.info("Sent message: {}", msg);
        return "finish";
    }

    /**
     * 优先级队列示例（可真实观察优先级重排）。
     * 原理：RabbitMQ 的优先级只对“同时在队列里堆积”的消息生效。
     * 所以先停掉消费者，把一批乱序优先级的消息全部堆进队列，
     * 再启动消费者：此时 broker 会按 priority 从高到低投递，
     * 日志里的消费顺序与发送顺序不同（高优先先出），即可验证。
     */
    @GetMapping("priority")
    public String priority() throws InterruptedException {
        MessageListenerContainer container =
                registryProvider.getObject().getListenerContainer("priorityListener");
        // 1. 先停止消费，保证接下来发的消息都堆在队列里，而不是被立即消费掉
        container.stop();

        // 2. 故意以乱序 priority 发送（数字代优先级，1~10）
        int[] priorities = {1, 9, 3, 10, 2, 8, 5, 9, 1, 7, 6, 4};
        for (int i = 0; i < priorities.length; i++) {
            rabbitProducer.sendMsg("testPriorityQueue",
                    "seq=" + i + ", priority=" + priorities[i], priorities[i]);
        }
        log.info("发送顺序(priority): {}", Arrays.toString(priorities));

        // 3. 稍等确保消息全部入队，再启动消费者：此时队列内消息已就位，
        //    broker 会按 priority 高->低投递，消费日志会呈现优先级插队效果
        Thread.sleep(300);
        container.start();
        return "已发送 " + priorities.length + " 条消息；发送顺序见日志，消费顺序应大致按 priority 10->1";
    }
}
