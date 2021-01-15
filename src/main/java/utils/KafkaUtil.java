package utils;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

public class KafkaUtil {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    public static KafkaConsumer<String, String> createKafkaConsumer(Map<String, String> confMap, String groupId) {
        Properties props = new Properties();
        Date current = new Date();
        String currentStr = format.format(current);
        props.put("bootstrap.servers", confMap.getOrDefault("kafka.bootstrap.servers", "node1:6667"));
        props.put("group.id", confMap.getOrDefault("kafka.topic.group", groupId));
        props.put("enable.auto.commit", confMap.getOrDefault("kafka.enable.auto.commit", "false"));
        props.put("auto.offset.reset", confMap.getOrDefault("kafka.auto.offset.reset", "earliest"));
        props.put("max.poll.records", confMap.getOrDefault("kafka.max.poll.records", "1000"));
        props.put("max.poll.interval.ms", confMap.getOrDefault("kafka.max.poll.interval.ms", "30000"));
        props.put("session.timeout.ms", confMap.getOrDefault("kafka.session.timeout.ms", "30000"));
        props.put("request.timeout.ms", confMap.getOrDefault("kafka.request.timeout.ms", "30000"));
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
        return consumer;
    }

    public static KafkaProducer createKafkaProducer(Map<String, String> confMap) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", confMap.get("kafka.bootstrap.server.dest"));
        propsProducer.put("key.serializer", StringSerializer.class.getName());
        propsProducer.put("value.serializer", StringSerializer.class.getName());
        long bufferMemory = 256 * 1024 * 1024;
        propsProducer.put("buffer.memory", bufferMemory);
        return new KafkaProducer(propsProducer);
    }

    public static void producerSend(KafkaProducer kafkaProducer, String topic, String data) {
        kafkaProducer.send(new ProducerRecord<String, String>(topic, data), new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if (null != e) {
                    LogUtil.diagLog(Severity.ERROR, "发送kafka异常：\n{}", ExceptionUtils.getStackTrace(e));
                }

            }
        });
    }
}
