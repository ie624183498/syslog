package utils;

import com.h3c.bigdata.itoa.adp.log.LogUtil;
import com.h3c.bigdata.itoa.adp.log.Severity;
import dao.OdsApLocationMapper;
import dao.OnlineDataMapper;
import model.AllDataModel;
import model.OdsApLocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.Reader;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DatabaseUtil implements Serializable {
    private static SqlSessionFactory sessionFactory;

    public static void initSqlSession(Map<String, String> paramMap) {
        Reader reader = null;

        try {
            Properties properties = new Properties();
            properties.setProperty("db.ip", paramMap.get("postgre.ip"));
            properties.setProperty("db.port", paramMap.get("postgre.port"));
            properties.setProperty("db.username", paramMap.get("postgre.username"));
            properties.setProperty("db.password", paramMap.get("postgre.password"));
            reader = Resources.getResourceAsReader("mybatis/mybatis.xml");
            sessionFactory = (new SqlSessionFactoryBuilder()).build(reader, properties);
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getMessage(e));
                } finally {
                    LogUtil.diagLog(Severity.INFO, "reader Reader流关闭 ... ");
                }
            }

        }

    }

    public static SqlSession getSession() {
        return sessionFactory.openSession();
    }

    public static List<AllDataModel> patchSelect(String tableName) {
        SqlSession session = getSession();
        List<AllDataModel> onlineDataModelList = new ArrayList<>();

        try {
            onlineDataModelList = new ArrayList<>();
            if ("ods_online_data_temp".equals(tableName)) {
                OnlineDataMapper mapper = session.getMapper(OnlineDataMapper.class);
                onlineDataModelList = mapper.patchSelect();
            }

            session.commit();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
            session.rollback();
        } finally {
            session.close();
        }

        return onlineDataModelList;
    }

    public static List<AllDataModel> selectOldData(String tableName, Timestamp onlineTime) {
        SqlSession session = getSession();
        List<AllDataModel> onlineDataModelList = new ArrayList<>();

        try {
            onlineDataModelList = new ArrayList<>();
            if ("ods_online_data_temp".equals(tableName)) {
                OnlineDataMapper mapper = session.getMapper(OnlineDataMapper.class);
                onlineDataModelList = mapper.selectOldData(onlineTime);
            }

            session.commit();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
            session.rollback();
        } finally {
            session.close();
        }

        return onlineDataModelList;
    }

    public static void deleteOldData(String tableName, Timestamp onlineTime) {
        SqlSession session = getSession();

        try {
            if ("ods_online_data_temp".equals(tableName)) {
                OnlineDataMapper mapper = session.getMapper(OnlineDataMapper.class);
                mapper.deleteOldData(onlineTime);
            }

            session.commit();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
            session.rollback();
        } finally {
            session.close();
        }

    }

    public static void insertOnlineData(List<AllDataModel> allDataModels) {
        SqlSession session = getSession();

        try {
            OnlineDataMapper mapper = session.getMapper(OnlineDataMapper.class);
            mapper.insertOnlineData(allDataModels);
            session.commit();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
            session.rollback();
        } finally {
            session.close();
        }

    }

    public static void deleteOnlineData() {
        SqlSession session = getSession();

        try {
            OnlineDataMapper mapper = session.getMapper(OnlineDataMapper.class);
            mapper.deleteOnlineData();
            session.commit();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
            session.rollback();
        } finally {
            session.close();
        }

    }

    public static List<OdsApLocation> selectAllAp() {
        List<OdsApLocation> odsApLocations = new ArrayList<>();
        SqlSession session = getSession();

        try {
            OdsApLocationMapper mapper = session.getMapper(OdsApLocationMapper.class);
            odsApLocations = mapper.selectAllAp();
            session.commit();
        } catch (Exception e) {
            LogUtil.diagLog(Severity.ERROR, "exception trace {}", ExceptionUtils.getStackTrace(e));
            session.rollback();
        } finally {
            session.close();
        }

        return odsApLocations;
    }
}
