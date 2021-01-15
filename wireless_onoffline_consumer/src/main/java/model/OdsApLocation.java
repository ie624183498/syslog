package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class OdsApLocation implements Serializable {
    private Integer mainId;
    private String apSerialId;
    private String name;
    private String mac;
    private String location;
    private String zone;
    private String area;
    private String floor;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String areaId;

    public Integer getMainId() {
        return this.mainId;
    }

    public void setMainId(Integer mainId) {
        this.mainId = mainId;
    }

    public String getApSerialId() {
        return this.apSerialId;
    }

    public void setApSerialId(String apSerialId) {
        this.apSerialId = apSerialId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getZone() {
        return this.zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getArea() {
        return this.area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getFloor() {
        return this.floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public BigDecimal getLongitude() {
        return this.longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return this.latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getAreaId() {
        return this.areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }
}
