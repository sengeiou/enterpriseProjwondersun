package com.proj.appservice;

import com.arlen.ebp.entity.Organization;
import org.apache.poi.ss.usermodel.Cell;

import java.sql.Connection;
import java.util.Map;

/**
 * Created by arlenChen on 2017/5/15.
 * 导入excel工具服务
 *
 * @author arlenChen
 */
public interface ExcelUtilAppService {
    /**
     * 格式化列数据
     *
     * @param cell cell
     * @return String
     */
    String getCellValue(Cell cell);

    /**
     * 添加错误信息
     *
     * @param map    map
     * @param errMsg errMsg
     */
    void putErrMsg(Map<String, String> map, String errMsg);

    /**
     * 检测行数据是否为空
     *
     * @param map 行数据
     * @return 检测
     */
    boolean checkRowAllBlank(Map<String, String> map);

    /**
     * 拼接箱数字符串
     *
     * @param stdQty 标准单位数量
     * @param minQty 最小单位数量
     * @param stdStr 标准单位名称
     * @param minStr 最小单位名称
     * @return 拼接字符串
     */
    String concatQtyStr(int stdQty, int minQty, String stdStr, String minStr);

    /**
     * get请求获取数据
     *
     * @param urlStr 接口
     * @return 数据
     * @throws Exception Exception
     */
    String httpGet(String urlStr) throws Exception;

    /**
     * 获取JDBC连接
     *
     * @param url        连接
     * @param user       用户
     * @param password   密码
     * @param driverName 驱动
     * @return 连接
     */
    Connection getConnection(String url, String user, String password, String driverName);

    /**
     * 创建推送记录
     *
     * @param organization 组织
     * @param isCreate     是否新建
     */
    void createPushRecord(Organization organization, boolean isCreate);
}
