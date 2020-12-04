package com.proj.appservice.impl;

import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysForeignPushRecord;
import com.arlen.ebp.enums.PushProcessType;
import com.arlen.ebp.enums.PushStatus;
import com.arlen.ebp.repository.SysForeignPushRecordRepository;
import com.proj.appservice.ExcelUtilAppService;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Created by arlenChen on 2017/5/15.
 * 导入excel工具服务
 *
 * @author arlenChen
 */
@Service
public class ExcelUtilAppServiceImpl implements ExcelUtilAppService {

    @Resource
    private SysForeignPushRecordRepository pushRecordRepository;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 格式化列数据
     *
     * @param cell cell
     * @return String
     */
    @Override
    public String getCellValue(Cell cell) {
        String result;
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC:
                // 数字类型
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    // 处理日期格式、时间格式
                    Date date = cell.getDateCellValue();
                    result = sdf.format(date);
                } else {
                    NumberFormat nf = NumberFormat.getInstance();
                    nf.setGroupingUsed(false);
                    result = nf.format(cell.getNumericCellValue());
                }
                break;
            case HSSFCell.CELL_TYPE_STRING:
                // String类型
                result = cell.getStringCellValue();
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                result = "";
                break;
            default:
                result = "";
                break;
        }
        return result;
    }

    /**
     * 添加错误信息
     *
     * @param map    map
     * @param errMsg errMsg
     */
    @Override
    public void putErrMsg(Map<String, String> map, String errMsg) {
        if (map.containsKey("errMsg") && StringUtil.isNotEmpty(map.get("errMsg"))) {
            map.put("errMsg", map.get("errMsg") + ";" + errMsg);
        } else {
            map.put("errMsg", errMsg);
        }
    }

    /**
     * 检测行数据是否为空
     *
     * @param map map
     * @return boolean
     */
    @Override
    public boolean checkRowAllBlank(Map<String, String> map) {
        boolean flag = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (StringUtil.isNotEmpty(entry.getValue())) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    /**
     * 拼接箱数字符串
     *
     * @param stdQty 标准单位数量
     * @param minQty 最小单位数量
     * @param stdStr 标准单位名称
     * @param minStr 最小单位名称
     * @return 拼接字符串
     */
    @Override
    public String concatQtyStr(int stdQty, int minQty, String stdStr, String minStr) {
        if (stdQty != 0 && minQty == 0) {
            return stdQty + stdStr;
        } else if (stdQty != 0) {
            return stdQty + stdStr + minQty + minStr;
        } else if (minQty != 0) {
            return minQty + minStr;
        } else {
            return "0";
        }
    }

    /**
     * get请求获取数据
     *
     * @param urlStr 接口
     * @return 数据
     * @throws Exception Exception
     */
    @Override
    public String httpGet(String urlStr) throws Exception {
        BufferedReader in = null;
        StringBuffer result;
        try {
            URI uri = new URI(urlStr);
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            //180秒超时时间
            connection.setConnectTimeout(180000);
            //readLine 超时时间
            connection.setReadTimeout(180000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Charset", "utf-8");
            connection.connect();
            result = new StringBuffer();
            //读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "utf-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取JDBC连接
     *
     * @param url        连接
     * @param user       用户
     * @param password   密码
     * @param driverName 驱动
     * @return 连接
     */
    @Override
    public Connection getConnection(String url, String user, String password, String driverName) {
        Connection conn;
        try {
            //初始化驱动包
            Class.forName(driverName);
            //根据数据库连接字符，名称，密码给conn赋值
            conn = DriverManager.getConnection(url, user, password);
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 创建推送记录
     *
     * @param organization 组织
     * @param isCreate     是否新建
     */
    @Override
    public void createPushRecord(Organization organization, boolean isCreate) {
        //推送记录
        SysForeignPushRecord pushRecord = new SysForeignPushRecord();
        pushRecord.setId(UUID.randomUUID().toString());
        pushRecord.setAddBy(organization.getEditBy());
        pushRecord.setEditBy(organization.getEditBy());
        pushRecord.setAddTime(new Date());
        pushRecord.setEditTime(new Date());
        pushRecord.setDataId(organization.getId());
        pushRecord.setDateType(organization.getOrgType() + "");
        pushRecord.setProcessType(isCreate ? PushProcessType.CREATE.index : PushProcessType.UPDATE.index);
        pushRecord.setPushStatus(PushStatus.TO_PUSH.index + "");
        pushRecordRepository.insert(pushRecord);
    }
}
