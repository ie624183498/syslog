package utils;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class OuiUtil implements Serializable {

    private static OuiUtil ins;
    private Map<String, String> macVendor;

    public static OuiUtil getIns() {
        if (ins == null) {
            ins = new OuiUtil();
            return ins;
        } else {
            return ins;
        }
    }

    public Map<String, String> getMacVendor() {
        if (macVendor == null) {
            macVendor = new HashMap<>();
            InputStream is = null;
            BufferedReader br = null;

            try {
                is = this.getClass().getResourceAsStream("/oui.csv");
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    String mac = line.substring(0, 8);
                    String vendor = line.substring(9);
                    this.macVendor.put(mac, vendor);
                }

                br.close();
                is.close();
            } catch (IOException ie) {
                LogUtil.diagLog(Severity.ERROR, "", ExceptionUtils.getStackTrace(ie));
            } finally {
                if (null != br) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        LogUtil.diagLog(Severity.ERROR, "", ExceptionUtils.getStackTrace(e));
                    } finally {
                        LogUtil.diagLog(Severity.WARN, "br BufferedReader 流关闭 ... ");
                    }
                }

                if (null != is) {
                    try {
                        is.close();
                    } catch (Exception e) {
                        LogUtil.diagLog(Severity.ERROR, "", ExceptionUtils.getStackTrace(e));
                    } finally {
                        LogUtil.diagLog(Severity.WARN, "is InputStream 流关闭 ... ");
                    }
                }

            }

            return this.macVendor;
        } else {
            return this.macVendor;
        }
    }
}
