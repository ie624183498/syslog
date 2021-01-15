package service;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import conf.BasicConstant;
import model.AllDataModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import utils.DatabaseUtil;
import utils.KafkaUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static conf.BasicConstant.BATCH_SIZE;

public class OnlineDataProcess implements Job {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        Date current = new Date();

        String patchTime = formatter.format(current);
        LogUtil.diagLog(Severity.WARN, "开始时间批次:{}", patchTime);

        JobDataMap jdMap = arg0.getJobDetail().getJobDataMap();
        long ageTime = jdMap.getLongFromString("ageTime");
        long pkgMapAge = jdMap.getLongFromString("pkgMapAge");
        String enableOthers = jdMap.getString("enableOthers");
        String topic = jdMap.getString("topic");
        KafkaProducer producer = (KafkaProducer) jdMap.get("producer");
        Map<String, AllDataModel> onlineDataMap = (Map<String, AllDataModel>) jdMap.get("onlineDataMap");
        List<AllDataModel> offlineDataList = (List<AllDataModel>) jdMap.get("offlineDataList");
        Map<String, Long> pkgMd5Map = (Map<String, Long>) jdMap.get("pkgMd5Map");

        List<AllDataModel> onlineDataModelList = new ArrayList<>();
        List<AllDataModel> offlineDataNewList = new ArrayList<>();


        try {
            LogUtil.diagLog(Severity.WARN, "该批次处理在线数据量:" + onlineDataMap.size());

            if ("true".equals(enableOthers)) {
                offlineDataNewList = deepCopy(offlineDataList);
                LogUtil.diagLog(Severity.WARN, "该批次处理上下线记录数据量:" + offlineDataNewList.size());
            }
            // 转存后清空
            offlineDataList.clear();

            Iterator<Map.Entry<String, AllDataModel>> iterator = onlineDataMap.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, AllDataModel> entry = iterator.next();
                AllDataModel allDataModel = entry.getValue();
                // 处理数据老化
                if (allDataModel.getOnlineTime().getTime() <= (current.getTime() - ageTime * BasicConstant.dayMsNum)) {
                    iterator.remove();
                } else {
                    onlineDataModelList.add(allDataModel.clone());
                }
            }

            // 整5时刻发送
            if ("0".equals(patchTime.substring(patchTime.length() - 1)) || "5".equals(patchTime.substring(patchTime.length() - 1))) {
                for (AllDataModel allDataModel : onlineDataModelList) {
                    KafkaUtil.producerSend(producer, topic, allDataModel.getOnlineDataString());
                }

                LogUtil.diagLog(Severity.WARN, "{}向{}发送数据{}条完成.", patchTime, topic, String.valueOf(onlineDataModelList.size()));
            }
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, ExceptionUtils.getStackTrace(e));
        }

        if ("true".equals(enableOthers)) {
            DatabaseUtil.deleteOnlineData();
            LogUtil.diagLog(Severity.WARN, "删除ods_online_data_temp表在线数据完成.");

            insertBatch(onlineDataModelList);
            LogUtil.diagLog(Severity.WARN, "向ods_online_data_temp表存储在线数据{}条数据完成.", String.valueOf(onlineDataModelList.size()));

            insertBatch(offlineDataNewList);
            LogUtil.diagLog(Severity.WARN, "向ods_online_data_temp表存储下线数据{}条数据完成.", String.valueOf((offlineDataNewList).size()));
        }

        LogUtil.diagLog(Severity.WARN, "pkgMd5Map老化前size:" + pkgMd5Map.size());
        Iterator<Map.Entry<String, Long>> iterator = pkgMd5Map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long timestamp = entry.getValue();
            if (timestamp < current.getTime() - pkgMapAge * 1000L) {
                iterator.remove();
            }
        }

        LogUtil.diagLog(Severity.WARN, "pkgMd5Map老化后size:" + pkgMd5Map.size());
        LogUtil.diagLog(Severity.WARN, "时间批次:{},处理完毕...", patchTime);
    }

    private void insertBatch(List<AllDataModel> list) {
        int numberBatch = BATCH_SIZE;
        double number = list.size() * 1.0D / numberBatch;
        int n = Double.valueOf(Math.ceil(number)).intValue();

        for (int i = 0; i < n; ++i) {
            int end = numberBatch * (i + 1);
            if (end > list.size()) {
                end = list.size();
            }

            DatabaseUtil.insertOnlineData(list.subList(numberBatch * i, end));
        }

    }

    private static <T> List<T> deepCopy(List<T> src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        List<T> dest = (List<T>) in.readObject();
        return dest;
    }
}
