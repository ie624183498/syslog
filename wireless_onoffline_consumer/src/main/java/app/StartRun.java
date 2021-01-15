package app;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import model.AllDataModel;
import model.OdsApLocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import service.KafkaConsumerThread;
import service.KafkaMonitorThread;
import service.OnlineDataProcess;
import utils.DatabaseUtil;
import utils.KafkaUtil;
import utils.OuiUtil;
import utils.PropertyUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class StartRun {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
    // 维护终端在线数据表
    private static Map<String, AllDataModel> onlineDataMap = new ConcurrentHashMap<>(16);
    // 维护1分钟的下线记录
    private static List<AllDataModel> offlineDataList = new CopyOnWriteArrayList<>();
    // 终端品牌解析
    private static Map<String, String> macVendor = OuiUtil.getIns().getMacVendor();
    // AP数据
    private static Map<String, OdsApLocation> apNameLocationMap = new HashMap<>(16);
    private static Map<String, OdsApLocation> apIdLocationMap = new HashMap<>(16);
    // 存放报文的MD5值
    private static Map<String, Long> pkgMd5Map = new ConcurrentHashMap<>(16);

    public static void main(String[] args) {
        LogUtil.diagLog(Severity.WARN, "终端上下线报文采集解析程序启动...");
        if (1 != args.length) {
            LogUtil.diagLog(Severity.ERROR, "传入参数有误,请传入配置文件...");
            return;
        }

        final Map<String, String> confMap = PropertyUtil.readProperties(args[0]);
        String topicName = confMap.getOrDefault("kafka.topic.src", "wireless_syslog");
        // 初始化数据库连接
        DatabaseUtil.initSqlSession(confMap);
        // 获取AP数据
        List<OdsApLocation> apLocationList = DatabaseUtil.selectAllAp();
        LogUtil.diagLog(Severity.WARN, "共获取AP个数:" + apLocationList.size());
        processToMap(apLocationList);
        LogUtil.diagLog(Severity.WARN, "按名字共获取AP个数:" + apLocationList.size());
        LogUtil.diagLog(Severity.WARN, "按序列号共获取AP个数:" + apIdLocationMap.size());

        // 消费者组名
        String groupId = "wireless_" + formatter.format(new Date());
        // 生成者
        KafkaProducer producer = KafkaUtil.createKafkaProducer(confMap);
        // 消费者线程
        KafkaConsumerThread consumerThread = new KafkaConsumerThread(confMap, onlineDataMap, macVendor, groupId, apNameLocationMap, apIdLocationMap, offlineDataList, pkgMd5Map);
        (new Thread(consumerThread)).start();
        LogUtil.diagLog(Severity.WARN, "消费程序线程启动完毕...");

        KafkaMonitorThread monitorThread = new KafkaMonitorThread(topicName, confMap, groupId);
        (new Thread(monitorThread)).start();

        LogUtil.diagLog(Severity.WARN, "监控消费进度线程启动完毕...");

        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDetail jobDetail = newJob(OnlineDataProcess.class)
                    .withDescription("每分钟将在线数据存入ods_online_data_temp表")
                    .withIdentity("job1", "group")
                    .build();
            jobDetail.getJobDataMap().put("producer", producer);
            jobDetail.getJobDataMap().put("topic", confMap.getOrDefault("kafka.topic.dest", "wireless_online"));
            jobDetail.getJobDataMap().put("onlineDataMap", onlineDataMap);
            jobDetail.getJobDataMap().put("offlineDataList", offlineDataList);
            jobDetail.getJobDataMap().put("pkgMd5Map", pkgMd5Map);
            jobDetail.getJobDataMap().put("ageTime", confMap.getOrDefault("data.age.day", "7"));
            jobDetail.getJobDataMap().put("pkgMapAge", confMap.getOrDefault("package.age.time", "20"));
            jobDetail.getJobDataMap().put("enableOthers", confMap.getOrDefault("enable.others.use", "false"));
            LogUtil.diagLog(Severity.INFO, "描述任务:{}", jobDetail.getDescription());

            Trigger trigger = (CronTrigger) newTrigger()
                    .withDescription("创建一个Trigger触发规则")
                    .withIdentity("trigger1", "group")
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 * * * ?"))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "创建定时任务失败:{}", ExceptionUtils.getStackTrace(e));
            producer.close();
        }

        LogUtil.diagLog(Severity.WARN, "主流程执行结束.");
    }

    private static void processToMap(List<OdsApLocation> odsApLocations) {

        for (OdsApLocation ap : odsApLocations) {
            apNameLocationMap.put(ap.getName(), ap);
            apIdLocationMap.put(ap.getApSerialId(), ap);
        }
    }
}
