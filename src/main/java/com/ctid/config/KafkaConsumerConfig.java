package com.ctid.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * kafka消费者配置
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    @Value("${spring.kafka.consumer.enable-auto-commit}")
    private boolean enableAutoCommit;
    @Value("${spring.kafka.consumer.auto-commit-interval-ms}")
    private String intervalMs;
    @Value("${spring.kafka.properties.max.poll.interval.ms}")
    private String maxPollIntervalTime;
    @Value("${spring.kafka.consumer.max-poll-records}")
    private String maxPollRecords;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${spring.kafka.listener.concurrency}")
    private Integer concurrency;
    @Value("${spring.kafka.listener.missing-topics-fatal}")
    private boolean missingTopicsFatal;
    @Value("${spring.kafka.listener.poll-timeout}")
    private long pollTimeout;
    @Value("${spring.kafka.consumer.key-deserializer}")
    private String keyDeserializer;
    @Value("${spring.kafka.consumer.value-deserializer}")
    private String valueDeserializer;
    @Value("${spring.kafka.consumer.properties.spring.deserializer.key.delegate.class}")
    private String keyDeserializerClass;
    @Value("${spring.kafka.consumer.properties.spring.deserializer.key.delegate.class}")
    private String valueDeserializerClass;
    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String packages;

    @Resource
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> propsMap = new HashMap<>();
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        //是否自动提交偏移量，默认值是true，为了避免出现重复数据和数据丢失，可以把它设置为false，然后手动提交偏移量
        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        //自动提交的时间间隔，自动提交开启时生效
        propsMap.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, intervalMs);
        //该属性指定了消费者在读取一个没有偏移量的分区或者偏移量无效的情况下该作何处理：
        //earliest：当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，从头开始消费分区的记录
        //latest：当各分区下有已提交的offset时，从提交的offset开始消费；无提交的offset时，消费新产生的该分区下的数据（在消费者启动之后生成的记录）
        //none：当各分区都存在已提交的offset时，从提交的offset开始消费；只要有一个分区不存在已提交的offset，则抛出异常
        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        //两次poll之间的最大间隔，默认值为5分钟。如果超过这个间隔会触发reBalance
        propsMap.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalTime);
        //这个参数定义了poll方法最多可以拉取多少条消息，默认值为500。如果在拉取消息的时候新消息不足500条，那有多少返回多少；如果超过500条，每次只返回500。
        //这个默认值在有些场景下太大，有些场景很难保证能够在5min内处理完500条消息，
        propsMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        //序列化
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass);
        propsMap.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, keyDeserializer);
        propsMap.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, valueDeserializer);
        propsMap.put(JsonDeserializer.TRUSTED_PACKAGES, packages);
        return new DefaultKafkaConsumerFactory<>(propsMap);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        //在侦听器容器中运行的线程数，一般设置为 机器数*分区数
        factory.setConcurrency(concurrency);
        //消费监听接口监听的主题不存在时，默认会报错，所以设置为false忽略错误
        factory.setMissingTopicsFatal(missingTopicsFatal);
        //自动提交关闭，需要设置手动消息确认
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setPollTimeout(pollTimeout);
        //设置为批量监听，需要用List接收
        factory.setBatchListener(false); //批量消费消息
        // 配置错误处理器
        // 3次重试后失败的处理
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                this::sendToDlq, new FixedBackOff(1000, 2)
        );
        // 确保在发送到DLQ后提交偏移量
        errorHandler.setCommitRecovered(true);
        // 指定 SerializationException类型的异常为不可重试的异常
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        // 添加重试监听器用于日志记录
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.info("重试消费消息 (尝试:{}次): 主题：{}，分区：{}，偏移量：{}， 原因：{}，内容：【{}】", deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage(), record.value())
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // 发送到死信队列的专用方法
    private void sendToDlq(ConsumerRecord<?, ?> record, Exception exception) {
        try {
            Headers headers = new RecordHeaders();
            String topic = record.topic();
            String partition = String.valueOf(record.partition());
            String offset = String.valueOf(record.offset());
            String timestamp = String.valueOf(record.timestamp());

            headers.add("ORIGIN_ERROR_MSG", exception.getMessage().getBytes(StandardCharsets.UTF_8));
            headers.add("ORIGIN_TOPIC", topic.getBytes(StandardCharsets.UTF_8));
            headers.add("ORIGIN_PARTITION", partition.getBytes(StandardCharsets.UTF_8));
            headers.add("ORIGIN_OFFSET", offset.getBytes(StandardCharsets.UTF_8));
            headers.add("ORIGIN_TIMESTAMP", timestamp.getBytes(StandardCharsets.UTF_8));

            // 3. 构建带 Headers 的 ProducerRecord
            ProducerRecord<Object, Object> dlqRecord = new ProducerRecord<>(
                    "dead-letter-queue",  // 死信队列 topic
                    null,                   // 分区 (null 表示由分区器决定)
                    record.key(),           // 原始 key
                    record.value(),         // 原始 value
                    headers                 // 自定义 Headers
            );

            kafkaTemplate.send(dlqRecord).addCallback(result -> {
            }, ex -> log.error("发送至死信队列失败", ex));
            log.error("已将消费失败的信息发送到死信队列 {}", record.value());
        } catch (Exception e) {
            log.error("写入死信队列失败，消息内容{}，异常信息{}", record.value(), e.getMessage(), e);
        }
    }
}
