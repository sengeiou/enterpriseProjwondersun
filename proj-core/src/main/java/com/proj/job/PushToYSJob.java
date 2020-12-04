package com.proj.job;

import com.arlen.ebms.utils.CommonUtil;
import com.arlen.ebms.utils.DateTimeTool;
import com.arlen.ebu.job.AbstractJob;
import com.proj.appservice.BillPushToYsRecordAppService;
import com.proj.entity.BillPushToYsRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * Created by johnny on 2019/12/24.
 *
 * @author johnny
 */
public class PushToYSJob extends AbstractJob {
    private static Logger logger = LoggerFactory.getLogger(PushToYSJob.class);
    @Resource
    private BillPushToYsRecordAppService billPushToYsRecordAppService;

    @Override
    protected void executeBusiness(String s) {
        //本次任务唯一ID
        String logId = CommonUtil.getUuid();
        String logMsg = "推送单据到美驰(YS)系统.LogId=" + logId;

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
                    message = "推送出现异常，请查看日志。";
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
    }
}
