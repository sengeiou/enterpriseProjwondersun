package com.proj.web.api;

import com.alibaba.fastjson.JSON;
import com.eaf.core.dto.APIResult;
import com.ebms.utils.CommonUtil;
import com.ebms.utils.DateTimeTool;
import com.proj.appservice.BillPushToYsRecordAppService;
import com.proj.appservice.WonderSunOpenAppService;
import com.proj.entity.BillPushToYsRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 同步基础信息到美驰中间库 手动API
 */
@Controller
@RequestMapping({"api/proj/syncMeiChi"})
public class SyncMeiChiManualApi {
    private static transient final Logger logger = LoggerFactory.getLogger(SyncMeiChiManualApi.class);
    @Resource
    private WonderSunOpenAppService appService;

    @Resource
    private BillPushToYsRecordAppService billPushToYsRecordAppService;

    @RequestMapping(value = "syncMaterial")
    @ResponseBody
    public APIResult<String> syncMaterial() {
        String s = "Manual";
        APIResult<String> result = new APIResult<>();
        logger.info("{}-同步产品信息到美驰--->>开始......", s);
        try {
            result = appService.syncMaterialToMidTable();
        } catch (Exception e) {
            logger.error("{}-同步产品信息到美驰--->>同步异常", s, e);
            result.fail(500, StringUtils.isEmpty(e.getMessage()) ? "java.lang.NullPointerException" : e.getMessage());
        } finally {
            logger.info("{}-同步产品信息到美驰--->>返回结果：{}", s, JSON.toJSONString(result));
            logger.info("{}-同步产品信息到美驰--->>结束......", s);
        }
        return result;
    }

    @RequestMapping(value = "syncGeoCity")
    @ResponseBody
    public APIResult<String> syncGeoCity() {
        String s = "Manual";
        APIResult<String> result = new APIResult<>();
        logger.info("{}-同步地理城市到美驰--->>开始......", s);
        try {
            result = appService.syncGeoCityToMidTable();
        } catch (Exception e) {
            logger.error("{}-同步地理城市到美驰--->>同步异常", s, e);
            result.fail(500, StringUtils.isEmpty(e.getMessage()) ? "java.lang.NullPointerException" : e.getMessage());
        } finally {
            logger.info("{}-同步地理城市到美驰--->>返回结果：{}", s, JSON.toJSONString(result));
            logger.info("{}-同步地理城市到美驰--->>结束......", s);
        }
        return result;
    }

    @RequestMapping(value = "syncCheckCity")
    @ResponseBody
    public APIResult<String> syncCheckCity() {
        String s = "Manual";
        APIResult<String> result = new APIResult<>();
        logger.info("{}-同步考核城市到美驰--->>开始......", s);
        try {
            result = appService.syncCheckCityToMidTable();
        } catch (Exception e) {
            logger.error("{}-同步考核城市到美驰--->>同步异常", s, e);
            result.fail(500, StringUtils.isEmpty(e.getMessage()) ? "java.lang.NullPointerException" : e.getMessage());
        } finally {
            logger.info("{}-同步考核城市到美驰--->>返回结果：{}", s, JSON.toJSONString(result));
            logger.info("{}-同步考核城市到美驰--->>结束......", s);
        }
        return result;
    }

    @RequestMapping(value = "pushBill")
    @ResponseBody
    public APIResult<String> pushBill() {
        APIResult<String> result = new APIResult<>();

        //本次任务唯一ID
        String logId = CommonUtil.getUuid();
        String logMsg = "Manual-推送单据到美驰(YS)系统.LogId=" + logId;

        int total = 0, success = 0, error = 0;

        logger.info("============================================================");
        Date logStartTime = DateTimeTool.getCurDatetime();
        logger.info(logMsg + ",>>>>>>>>>>开始>>>>>>>>>>" + DateTimeTool.getCurrentTime());

        List<BillPushToYsRecord> list = billPushToYsRecordAppService.getWaitPushRecordList();
        logger.info(logMsg + ",获取待推送记录数：" + list.size() + "个。");

        //推送中：统一修改状态
        for (BillPushToYsRecord record : list) {
            billPushToYsRecordAppService.begin(record, logId);
        }

        //开始推送：
        for (BillPushToYsRecord record : list) {
            String billId = record.getPushMainId() + "," + record.getRemark();
            try {
                billPushToYsRecordAppService.doPush(record, logId);
                billPushToYsRecordAppService.end(record, logId);
                success++;
            } catch (Exception e) {
                logger.error(logMsg + ",[" + billId + "],出现异常：", e);
                String message = e.getMessage();
                if (StringUtils.isEmpty(message)) {
                    message = "Manual-推送出现异常，请查看日志。";
                } else {
                    if (message.length() > 2000) {
                        message = message.substring(0, 1500);
                    }
                }
                billPushToYsRecordAppService.error(record, logId, message);
                error++;
            }
        }

        //拼装返回信息
        String msg = "";
        msg += "T" + total + "|";
        msg += "S" + success + "|";
        msg += "E" + error + "|";
        logger.info(logMsg + ",结果：" + msg + "。");

        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info(logMsg + ",耗时：" + interval + "秒。");

        logger.info(logMsg + ",<<<<<<<<<<结束<<<<<<<<<<" + DateTimeTool.getCurrentTime());
        logger.info("============================================================");

        result.succeed().attachData("手动执行成功.LogId=" + logId + ",结果：" + msg + ",耗时：" + interval + "秒。");
        return result;
    }

}
