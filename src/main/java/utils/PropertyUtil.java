package utils;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

public class PropertyUtil {

    public static HashMap<String, String> readProperties(String fileName) {
        HashMap<String, String> propertyMap = new HashMap<>();
        Properties prop = new Properties();

        try {
            InputStream fis = new BufferedInputStream(new FileInputStream(fileName));
            prop.load(fis);
            Set<Object> keySet = prop.keySet();
            LogUtil.diagLog(Severity.WARN, "所有配置参数为:");

            for (Object object : keySet) {
                String key = object.toString();
                propertyMap.put(key, prop.getProperty(key));
                LogUtil.diagLog(Severity.WARN, "{}:{}", key, prop.getProperty(key));
                if ("".equals(prop.getProperty(key).trim())) {
                    LogUtil.diagLog(Severity.ERROR, "配置{}为空", key);
                }
            }

            fis.close();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "读取指定路径的配置文件出错!");
            LogUtil.diagLog(Severity.ERROR, "exception trace \n{}", ExceptionUtils.getStackTrace(e));
        }

        return propertyMap;
    }
}
