package conf;

public class BasicConstant {

    public static final String KAFKA_KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
    public static final String KAFKA_VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
    public static final String KAFKA_SERIALIZER_CLASS_DEST = "kafka.serializer.StringEncoder";
    public static final String KAFKA_PRODUCER_TYPE_DEST = "async";
    public static final String KAFKA_REQUEST_REQUIRED_ACKS_DEST = "1";
    public static final String KAFKA_QUEUE_BUFFERING_MAX_MS_DEST = "1000";
    public static final String KAFKA_QUEUE_BUFFERING_MAX_MESSAGES_DEST = "10000";
    public static final String KAFKA_QUEUE_ENQUEUE_TIMEOUT_MS_DEST = "-1";
    public static final String KAFKA_BATCH_NUM_MESSAGES_DEST = "200";
    public static final long dayMsNum = 24*60*60*1000;
    public static final int PACKET_LENGTH = 11;
    public static final int ONE_SECOND_MS = 1000;
    public static final int BATCH_SIZE = 2500;

    public static final String ONLY_ONLINE_FLAG="0";
    public static final String ONLY_OFFLINE_FLAG="1";
    public static final String ONLINE_OFFLINE_FLAG="2";
}
