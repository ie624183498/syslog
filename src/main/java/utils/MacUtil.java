package utils;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.security.MessageDigest;

public class MacUtil {

    public static String processMac(String temp) {

        if (null == temp || "".equals(temp)) {
            return "";
        }

        String mac = temp.replace("-", "").replace(":", "").replace(".", "").toLowerCase();
        if (12 == mac.length()) {
            mac = mac.substring(0, 2) + ":" + mac.substring(2, 4) + ":" + mac.substring(4, 6) + ":" + mac.substring(6, 8) + ":" + mac.substring(8, 10) + ":" + mac.substring(10, 12);
        } else {
            mac = temp;
        }

        return mac;
    }

    public static String getMD5(String key) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        try {
            byte[] btInput = key.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;

            for (int i = 0; i < j; ++i) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }

            return new String(str);
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "{}", ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
}
