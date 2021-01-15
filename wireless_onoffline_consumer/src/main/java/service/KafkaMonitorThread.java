package service;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import utils.KafkaUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaMonitorThread implements Runnable {
    private String topicName;
    private String groupId;
    private Map<String, String> confMap;

    public KafkaMonitorThread(String topicName, Map<String, String> confMap, String groupId) {
        this.topicName = topicName;
        this.confMap = confMap;
        this.groupId = groupId;
    }

    @Override
    public void run() {
        Map<Integer, Long> endOffsetMap = new HashMap<>();
        Map<Integer, Long> commitOffsetMap = new HashMap<>();
        List<TopicPartition> topicPartitions = new ArrayList<>();
        KafkaConsumer<String, String> consumer = KafkaUtil.createKafkaConsumer(confMap, groupId);

        List<PartitionInfo> partitionsFor = consumer.partitionsFor(topicName);

        while (true) {

            for (PartitionInfo partitionInfo : partitionsFor) {
                TopicPartition topicAndPartition = new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
                topicPartitions.add(topicAndPartition);
            }

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);

            for (TopicPartition partitionInfo : endOffsets.keySet()) {
                endOffsetMap.put(partitionInfo.partition(), endOffsets.get(partitionInfo));
            }

            for (TopicPartition topicAndPartition : topicPartitions) {
                OffsetAndMetadata committed = consumer.committed(topicAndPartition);
                if (null == committed) {
                    commitOffsetMap.put(topicAndPartition.partition(), endOffsetMap.get(topicAndPartition.partition()));
                } else {
                    commitOffsetMap.put(topicAndPartition.partition(), committed.offset());
                }
            }


            long lagSum = 0L;
            if (endOffsetMap.size() == commitOffsetMap.size()) {
                for (Integer partition : endOffsetMap.keySet()) {
                    long endOffSet = endOffsetMap.get(partition);
                    long commitOffSet = commitOffsetMap.get(partition);
                    long diffOffset = endOffSet - commitOffSet;
                    lagSum += diffOffset;
                    LogUtil.diagLog(Severity.WARN, "groupID:" + this.groupId + ", partition:" + partition + ", endOffset:" + endOffSet + ", commitOffset:" + commitOffSet + ", diffOffset:" + diffOffset);

                }
            } else {
                LogUtil.diagLog(Severity.WARN, "this topic partitions lost");
            }

            try {
                Thread.sleep(10000L);
            } catch (InterruptedException ie) {
                LogUtil.diagLog(Severity.ERROR, "\n{}", ExceptionUtils.getStackTrace(ie));
            }
        }
    }
}
