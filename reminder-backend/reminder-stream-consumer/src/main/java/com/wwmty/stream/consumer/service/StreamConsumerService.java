package com.wwmty.stream.consumer.service;

import com.wwmty.stream.consumer.handler.StreamEventHandler;
import com.wwmty.stream.consumer.handler.StreamEventHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Service
public class StreamConsumerService {

    private final StreamEventHandlerFactory handlerFactory;
    
    // 线程池配置
    @Value("${reminder.stream.thread-pool.core-size:5}")
    private int corePoolSize;
    
    @Value("${reminder.stream.thread-pool.max-size:20}")
    private int maxPoolSize;
    
    @Value("${reminder.stream.thread-pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${reminder.stream.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;
    
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public StreamConsumerService(StreamEventHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
    
    @PostConstruct
    public void initThreadPool() {
        threadPoolExecutor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "stream-consumer-" + (++counter));
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用线程执行
        );
        
        log.info("Stream消费者线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
    }
    
    @PreDestroy
    public void destroyThreadPool() {
        if (threadPoolExecutor != null) {
            log.info("正在关闭Stream消费者线程池...");
            threadPoolExecutor.shutdown();
            try {
                if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("线程池未能在30秒内正常关闭，强制关闭");
                    threadPoolExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("等待线程池关闭时被中断", e);
                threadPoolExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Stream消费者线程池已关闭");
        }
    }

    /**
     * Receives a stream message, extracts the command, and delegates to the appropriate handler.
     * The @Transactional annotation has been moved to the specific handlers that need it.
     * 使用线程池异步处理消息，提高并发处理能力。
     * @param record The message record from Redis Stream.
     */
    public void handleStreamEvent(MapRecord<String, String, String> record) {
        log.info("Received message from stream [{}]: {}", record.getStream(), record.getValue());

        // 提交到线程池异步处理
        threadPoolExecutor.submit(() -> {
            try {
                processStreamEvent(record);
            } catch (Exception e) {
                log.error("处理Stream事件时发生未预期的异常 - 消息ID: {}", record.getId(), e);
            }
        });
    }
    
    /**
     * 实际处理Stream事件的逻辑
     * @param record The message record from Redis Stream.
     */
    private void processStreamEvent(MapRecord<String, String, String> record) {
        // 清理所有字段的序列化问题
        Map<String, String> cleanedData = cleanSerializationIssues(record.getValue());
        
        String command = cleanedData.get("command");

        if (!StringUtils.hasText(command)) {
            log.error("消息 id {} 中的 command 字段缺失或为空。无法处理。 原始负载: {} 清理后负载: {}", 
                    record.getId(), record.getValue(), cleanedData);
            return;
        }
        
        log.debug("处理命令: [{}] (清理后)", command);

        Optional<StreamEventHandler> handlerOptional = handlerFactory.getHandler(command);
        if (handlerOptional.isPresent()) {
            StreamEventHandler handler = handlerOptional.get();
            try {
                log.info("发现命令 [{}] 的处理器。正在处理消息 id {} [线程: {}]", 
                        command, record.getId(), Thread.currentThread().getName());
                handler.handle(cleanedData, record.getId().getValue());
                log.info("使用命令 [{}] 的处理器成功处理了消息 id {} [线程: {}]", 
                        command, record.getId(), Thread.currentThread().getName());
            } catch (Exception e) {
                log.error("命令 [{}] 的处理器在处理消息 id {} 时失败 [线程: {}]: {}", 
                        command, record.getId(), Thread.currentThread().getName(), e.getMessage(), e);
                // 处理失败消息的逻辑，例如，发送到死信队列。
            }
        } else {
            log.warn("未找到命令 [{}] 的处理器，消息 id {} [线程: {}]。已忽略。", 
                    command, record.getId(), Thread.currentThread().getName());
            // 处理未知命令的逻辑。
        }
    }
    
    /**
     * 获取线程池状态信息，用于监控
     * @return 线程池状态信息
     */
    public String getThreadPoolStatus() {
        if (threadPoolExecutor == null) {
            return "线程池未初始化";
        }
        
        return String.format("线程池状态 - 活跃线程: %d, 核心线程: %d, 最大线程: %d, 队列大小: %d, 已完成任务: %d",
                threadPoolExecutor.getActiveCount(),
                threadPoolExecutor.getCorePoolSize(),
                threadPoolExecutor.getMaximumPoolSize(),
                threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getCompletedTaskCount());
    }
    
    /**
     * 清理Redis序列化问题，移除多余的双引号
     * @param originalData 原始数据
     * @return 清理后的数据
     */
    private Map<String, String> cleanSerializationIssues(Map<String, String> originalData) {
        Map<String, String> cleanedData = new HashMap<>();
        
        for (Map.Entry<String, String> entry : originalData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 清理值中的多余引号
            if (value != null) {
                value = value.trim();
                // 移除前后的双引号（如果存在）
                while (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                }
            }
            
            cleanedData.put(key, value);
        }
        
        log.debug("清理序列化问题 - 原始: {} -> 清理后: {}", originalData, cleanedData);
        return cleanedData;
    }
} 