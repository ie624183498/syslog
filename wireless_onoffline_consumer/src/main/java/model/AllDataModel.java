package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class AllDataModel implements Cloneable, Serializable {

    private String acName;
    private String sessionIntegrity;
    private Timestamp offlineTime;
    private String mac;
    private String ip;
    private String apName;
    private String apSerialId;
    private String apMac;
    private String dataType;
    private String vendorName;
    private String systemName;
    private int count;
    private Timestamp onlineTime;

    public String getAcName() {
        return acName;
    }

    public void setAcName(String acName) {
        this.acName = acName;
    }

    public String getSessionIntegrity() {
        return sessionIntegrity;
    }

    public void setSessionIntegrity(String sessionIntegrity) {
        this.sessionIntegrity = sessionIntegrity;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getApName() {
        return this.apName;
    }

    public void setApName(String apName) {
        this.apName = apName;
    }

    public String getApSerialId() {
        return this.apSerialId;
    }

    public void setApSerialId(String apSerialId) {
        this.apSerialId = apSerialId;
    }

    public String getApMac() {
        return this.apMac;
    }

    public void setApMac(String apMac) {
        this.apMac = apMac;
    }

    public String getDataType() {
        return this.dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getVendorName() {
        return this.vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getSystemName() {
        return this.systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Timestamp getOnlineTime() {
        return this.onlineTime;
    }

    public void setOnlineTime(Timestamp onlineTime) {
        this.onlineTime = onlineTime;
    }

    public Timestamp getOfflineTime() {
        return this.offlineTime;
    }

    public void setOfflineTime(Timestamp offlineTime) {
        this.offlineTime = offlineTime;
    }

    public String getOnlineDataString() {
        return this.ip + "," + this.mac + "," + this.vendorName + "," + this.apName + "," + this.apSerialId + "," + this.apMac + "," + this.dataType + "," + this.systemName;
    }

    public String toString() {
        return this.ip + "," + this.mac + "," + this.vendorName + "," + this.apName + "," + this.acName + ","+ this.apSerialId + "," + this.apMac + "," + this.dataType + "," + this.systemName + "," + this.onlineTime.toString() + "," + (this.offlineTime == null ? "offlinetimeisnull" : this.offlineTime).toString();
    }

    public AllDataModel clone() throws CloneNotSupportedException {
        return (AllDataModel)super.clone();
    }
}
