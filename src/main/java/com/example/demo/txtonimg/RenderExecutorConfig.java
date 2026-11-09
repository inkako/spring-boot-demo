package com.example.demo.txtonimg;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 海报并行渲染线程池。
 *
 * <p>Java2D 合成为 CPU 密集，配合背景图下载 / 落盘的 IO，按核数放大；
 * 采用有界队列 + {@link ThreadPoolExecutor.CallerRunsPolicy}，饱和时由调用线程执行，
 * 既不丢任务也形成天然背压。供 {@link com.example.demo.util.AsyncTaskUtils} 使用。
 */
@Configuration
public class RenderExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor posterRenderExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(2, cores);
        int maxPoolSize = corePoolSize * 2;
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "poster-render-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256), factory, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
