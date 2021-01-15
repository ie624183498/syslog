package dao;

import java.sql.Timestamp;
import java.util.List;
import model.AllDataModel;
import org.apache.ibatis.annotations.Param;

public interface OnlineDataMapper {

    List<AllDataModel> patchSelect();

    List<AllDataModel> selectOldData(@Param("onlineTime") Timestamp onlineTime);

    void deleteOldData(@Param("onlineTime") Timestamp onlineTime);

    void deleteOnlineData();

    void insertOnlineData(List<AllDataModel> allDataModels);
}