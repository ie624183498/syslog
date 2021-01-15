package service;

import com.alibaba.fastjson.JSONObject;
import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import model.AllDataModel;
import model.OdsApLocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import utils.KafkaUtil;
import utils.MacUtil;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static conf.BasicConstant.*;

public class KafkaConsumerThread implements Runnable {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");

    private String groupId;
    private Map<String, String> confMap;
    private Map<String, AllDataModel> onlineDataMap;
    private Map<String, String> macVendor;
    private Map<String, OdsApLocation> apNameLocationMap;
    private Map<String, OdsApLocation> apIdLocationMap;
    private List<AllDataModel> offlineDataList;
    private Map<String, Long> pkgMd5Map;

    public KafkaConsumerThread(Map<String, String> confMap, Map<String, AllDataModel> onlineDataMap, Map<String, String> macVendor, String groupId, Map<String, OdsApLocation> apNameLocationMap, Map<String, OdsApLocation> apIdLocationMap, List<AllDataModel> offlineDataList, Map<String, Long> pkgMd5Map) {
        this.confMap = confMap;
        this.onlineDataMap = onlineDataMap;
        this.macVendor = macVendor;
        this.groupId = groupId;
        this.apNameLocationMap = apNameLocationMap;
        this.apIdLocationMap = apIdLocationMap;
        this.offlineDataList = offlineDataList;
        this.pkgMd5Map = pkgMd5Map;
    }

    private void processSyslog(JSONObject message) {

        String type = message.getString("dataType");
        String corp = message.getString("corp");
        String ip = message.getString("ip");
        if ("0.0.0.0".equals(ip)) {
            ip = "";
        }

        String acName = message.getString("acName");
        String mac = message.getString("mac");
        String apName = message.getString("apName");
        String apSerialId = message.getString("apSerialId");
        String apMac = message.getString("apMac");
        String systemName = message.getString("systemName");
        String dataType = message.getString("relationType");
        Timestamp pkgTime = Timestamp.valueOf(message.getString("onlineTime"));

        String clientMac = MacUtil.processMac(mac);
        String apMacNew = MacUtil.processMac(apMac);

        if ("".equals(mac) || "".equals(clientMac)) {
            // 终端mac为空,丢弃该报文
            return;
        }

        String vendorName = "未知品牌";
        if (clientMac.length() >= 8 && macVendor.containsKey(clientMac.substring(0, 8))) {
            vendorName = macVendor.get(clientMac.substring(0, 8));
        }

        AllDataModel currentDataModel = new AllDataModel();
        currentDataModel.setIp(ip);
        currentDataModel.setMac(clientMac);
        currentDataModel.setApSerialId(apSerialId);
        currentDataModel.setSystemName(systemName);
        currentDataModel.setDataType(dataType);
        currentDataModel.setVendorName(vendorName);

        OdsApLocation apLocation;
        if ("h3c_v5_ac".equals(corp)) {
            apLocation = apIdLocationMap.getOrDefault(acName + apSerialId, new OdsApLocation());
            currentDataModel.setAcName(acName);
            currentDataModel.setApSerialId(acName + apSerialId);
            currentDataModel.setApName(apLocation.getName() == null ? "" : apLocation.getName());
        } else {
            apLocation = apNameLocationMap.getOrDefault(apName, new OdsApLocation());
            currentDataModel.setApName(apName);
            currentDataModel.setApSerialId(apLocation.getApSerialId() == null ? "" : apLocation.getApSerialId());
            if ("ruijie_ac".equals(corp)) {
                String[] ruiJieApSerialId = currentDataModel.getApSerialId().split("__");
                if (ruiJieApSerialId.length < 2) {
                    currentDataModel.setAcName("ruijie");
                } else {
                    currentDataModel.setAcName(ruiJieApSerialId[0]);
                }
            } else {
                currentDataModel.setAcName(acName);
            }
        }

        // 华为的ap_mac从报文里获取
        if ("huawei_ac".equals(corp)) {
            currentDataModel.setApMac(apMacNew);
        } else {
            currentDataModel.setApMac(apLocation.getMac() == null ? "" : apLocation.getMac());
        }

        // 华为的漫游两种情况
        // 1.收到AP2的上线报文,收到AP1->AP2的漫游报文,且这两种保温时间相同,此时不处理漫游报文
        // 2.单独收到一个漫游报文,需要记录处理
        // 连着收到同一个ap的上线不再处理
        if ("ONLINE".equals(type) || "ROAM".equals(type)) {
            if (onlineDataMap.containsKey(clientMac)) {
                AllDataModel onlineData = onlineDataMap.get(clientMac);
                if (!apName.equals(onlineData.getApName()) && pkgTime.getTime() > onlineData.getOnlineTime().getTime()) {
                    onlineDataMap.remove(clientMac);
                    onlineData.setOfflineTime(pkgTime);
                    onlineData.setSessionIntegrity(ONLINE_OFFLINE_FLAG);
                    offlineDataList.add(onlineData);

                    // 上线新增
                    currentDataModel.setOnlineTime(pkgTime);
                    currentDataModel.setOfflineTime(null);
                    currentDataModel.setSessionIntegrity(ONLY_ONLINE_FLAG);
                    onlineDataMap.put(clientMac, currentDataModel);
                }
            } else {
                // 上线新增
                currentDataModel.setOnlineTime(pkgTime);
                currentDataModel.setOfflineTime(null);
                currentDataModel.setSessionIntegrity(ONLY_ONLINE_FLAG);
                onlineDataMap.put(clientMac, currentDataModel);
            }

        } else if ("UPDATE".equals(type)) {

            // 更新终端ip地址
            if (this.onlineDataMap.containsKey(clientMac)) {
                AllDataModel onlineData = onlineDataMap.get(clientMac);
                onlineData.setIp(ip);
                this.onlineDataMap.put(clientMac, onlineData);
            }

        } else if ("OFFLINE".equals(type)) {
            if (onlineDataMap.containsKey(clientMac)) {
                AllDataModel onlineData = onlineDataMap.get(clientMac);

                if (onlineData.getApName().equals(apName) || ("".equals(apName)) && pkgTime.getTime() >= onlineData.getOnlineTime().getTime()) {
                    // 生成下线记录
                    onlineData.setOfflineTime(pkgTime);
                    onlineData.setSessionIntegrity(ONLINE_OFFLINE_FLAG);
                    offlineDataList.add(onlineData);
                    onlineDataMap.remove(clientMac);
                } else if (!onlineData.getApName().equals(apName) && pkgTime.getTime() > onlineData.getOnlineTime().getTime() && !"".equals(apName)) {
                    onlineData.setOfflineTime(pkgTime);
                    onlineData.setSessionIntegrity(ONLINE_OFFLINE_FLAG);
                    offlineDataList.add(onlineData);

                    currentDataModel.setOnlineTime(null);
                    currentDataModel.setOfflineTime(pkgTime);
                    currentDataModel.setSessionIntegrity(ONLY_OFFLINE_FLAG);
                    offlineDataList.add(currentDataModel);
                    onlineDataMap.remove(clientMac);
                }

            } else {
                // 华三v5下线没有ap名称,需要排除
                if (!"".equals(apName)) {
                    currentDataModel.setOnlineTime(null);
                    currentDataModel.setOfflineTime(pkgTime);
                    currentDataModel.setSessionIntegrity(ONLY_OFFLINE_FLAG);
                    offlineDataList.add(currentDataModel);
                }
            }
        } else {
            LogUtil.diagLog(Severity.ERROR, "该报文类型不支持.");
            LogUtil.diagLog(Severity.ERROR, "{}", message.toString());
        }
    }

    @Override
    public void run() {
        final KafkaConsumer<String, String> consumer = KafkaUtil.createKafkaConsumer(confMap, groupId);
        consumer.subscribe(Arrays.asList(confMap.getOrDefault("kafka.topic.src", "wireless_syslog")));

        try {
            int minCommitSize = Integer.parseInt(this.confMap.getOrDefault("kafka.min.commit.size", "100"));
            int icount = 0;

            while (true) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Long.parseLong(confMap.getOrDefault("kafka.poll.time.ms", "1000")));

                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    icount++;
                    String msg = consumerRecord.value();
                    try {
                        if (pkgMd5Map.containsKey(MacUtil.getMD5(msg))) {
                            continue;
                        } else {
                            pkgMd5Map.put(MacUtil.getMD5(msg), (new Date()).getTime());
                        }

                        JSONObject msgObj = JSONObject.parseObject(msg);
                        if (PACKET_LENGTH != msgObj.size()) {
                            LogUtil.diagLog(Severity.ERROR, "{}", msg);
                            LogUtil.diagLog(Severity.ERROR, "该报文JSON格式字段数量错误...");
                            continue;
                        }

                        processSyslog(msgObj);

                    } catch (Exception e) {
                        LogUtil.diagLog(Severity.ERROR, "{}", msg);
                        LogUtil.diagLog(Severity.ERROR, "该报文JSON格式错误...");
                        LogUtil.diagLog(Severity.ERROR, ExceptionUtils.getStackTrace(e));
                    }

                }

                // 提交偏移量
                if (icount >= minCommitSize) {
                    consumer.commitAsync(new OffsetCommitCallback() {
                        public void onComplete(Map<TopicPartition, OffsetAndMetadata> map, Exception e) {
                            if (null != e) {
                                LogUtil.diagLog(Severity.WARN, "{} offset commit failed...", Thread.currentThread().getName());
                                LogUtil.diagLog(Severity.ERROR, "{}", ExceptionUtils.getStackTrace(e));
                                consumer.commitSync();
                            }

                        }
                    });
                    icount = 0;
                }
            }
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, ExceptionUtils.getStackTrace(e));
        } finally {
            consumer.commitSync();
            consumer.close();
            LogUtil.diagLog(Severity.ERROR, "消费者程序关闭...");
        }

    }
}
