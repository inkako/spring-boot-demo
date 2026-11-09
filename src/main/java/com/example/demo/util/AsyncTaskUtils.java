package com.example.demo.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AsyncTaskUtils {
    /**
     * 并行执行任务并返回执行结果
     *
     * @param tasks    任务列表
     * @param function 任务处理逻辑
     * @param executor 线程池
     * @param <T>      待执行任务
     * @param <R>      任务对应执行结果
     * @return 任务执行结果列表
     */
    public static <T, R> List<R> execute(List<T> tasks,
                                         Function<T, R> function,
                                         ThreadPoolExecutor executor) {
        List<CompletableFuture<R>> completableFutureList = tasks.stream()
                .map(task -> {
                    return CompletableFuture.supplyAsync(() -> {
                        return function.apply(task);
                    }, executor);
                })
                .collect(Collectors.toList());

        List<R> resultList = completableFutureList.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return resultList;
    }

    /**
     * 并行执行任务
     *
     * @param tasks    任务列表
     * @param consumer 任务处理逻辑
     * @param executor 线程池
     * @param <T>      待执行任务
     */
    public static <T> void execute(List<T> tasks,
                                   Consumer<T> consumer,
                                   ThreadPoolExecutor executor) {
        List<CompletableFuture<Void>> completableFutureList = tasks.stream()
                .map(task -> {
                    return CompletableFuture.runAsync(() -> {
                        consumer.accept(task);
                    }, executor);
                })
                .collect(Collectors.toList());

        completableFutureList.forEach(CompletableFuture::join);
    }
}
