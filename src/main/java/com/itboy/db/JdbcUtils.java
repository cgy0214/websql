package com.itboy.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName JdbcUtils
 * @Description 数据库操作
 * @Author 超 boy_0214@sina.com
 * @Date 2019/6/14 0014 16:31
 **/
@Slf4j
public class JdbcUtils {

    public static Map<String,Object> updateByPreparedStatement(String sourceKey, String sql, List<Object> params) {
        Map<String,Object> map = new HashMap<>();
        map.put("code","1");
        map.put("msg","执行成功");
        DruidDataSource source = DataSourceFactory.getDataSource(sourceKey);
        Connection connection = DataSourceFactory.getConnection(source);
        if(source==null){
            throw new NullPointerException(sourceKey+"：未获取到有效数据源");
        }
        if(source==null){
            throw new NullPointerException(sourceKey+"：未获取到有效连接");
        }
        int result = -1;
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(sql);
            int index = 1;
            if (params != null && !params.isEmpty()) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(index++, params.get(i));
                }
            }
            result = pstmt.executeUpdate();
            map.put("data",result);
        } catch (SQLException e) {
            log.error("执行异常,"+e.getMessage());
            map.put("code","2");
            map.put("msg",e.getMessage());
        } finally {
            releaseConn(null, pstmt, connection);
        }
        return map;

    }

    public static Map<String,Object> findMoreResult(String sourceKey, String sql, List<Object> params)  {
        DruidDataSource source = DataSourceFactory.getDataSource(sourceKey);
        if(source==null){
            throw new NullPointerException(sourceKey+"：未获取到有效数据源");
        }
        Connection connection = DataSourceFactory.getConnection(source);
        if(source==null){
            throw new NullPointerException(sourceKey+"：未获取到有效连接");
        }
        Map<String,Object> map = new HashMap<>();
        JSONArray list = new JSONArray();
        map.put("code","1");
        map.put("msg","执行成功");
        int index = 1;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try{
            pstmt = connection.prepareStatement(sql);
            if (params != null && !params.isEmpty()) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(index++, params.get(i));
                }
            }
            resultSet = pstmt.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int cols_len = metaData.getColumnCount();
            String cols_name;
            Object cols_value;
            while (resultSet.next()) {
                JSONObject json = new JSONObject();
                for (int i = 0; i < cols_len; i++) {
                    cols_name = metaData.getColumnLabel(i + 1);
                    cols_value = resultSet.getObject(cols_name);
                    if (cols_value == null) {
                        cols_value = "";
                    }
                    json.put(cols_name, cols_value);
                }
                list.add(json);
            }
            map.put("data",list);
        }catch (Exception e){
            log.error("执行异常,"+e.getMessage());
            map.put("code","2");
            map.put("msg",e.getMessage());
        }finally {
            releaseConn(resultSet, pstmt, connection);
        }
        return map;

    }

    /**
     * 未使用。 通过count(1)获取记录总数
     */
    public static long getCount(String sourceKey, String sql, List<Object> params) {
        DruidDataSource source = DataSourceFactory.getDataSource(sourceKey);
        Connection connection = DataSourceFactory.getConnection(source);
        if(source==null){
            throw new NullPointerException(sourceKey+"：未获取到有效数据源");
        }
        if(source==null){
            throw new NullPointerException(sourceKey+"：未获取到有效连接");
        }
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        long count = 0L;
        try {
            pstmt = connection.prepareStatement(sql);
            int index = 1;
            if (params != null && !params.isEmpty()) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(index++, params.get(i));
                }
            }
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                count = resultSet.getLong(1);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        } finally {
            releaseConn(resultSet, pstmt, connection);
        }
        return count;

    }

    public static void releaseConn(ResultSet resultSet, PreparedStatement pstmt, Connection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
    }

}

