package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.proj.entity.BillPushToYsRecord;

import java.util.List;

/**
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */
public interface BillPushToYsRecordAppService {

    /**
     * 创建
     *
     * @param type
     * @param mainId
     * @param remark
     * @return
     */
    APIResult<String> create(String type, String mainId, String remark);

    /**
     * 开始推送
     *
     * @param record
     * @param logId
     */
    void begin(BillPushToYsRecord record, String logId);

    /**
     * 推送
     *
     * @param record
     * @param logId
     */
    void doPush(BillPushToYsRecord record, String logId);

    /**
     * 结束
     *
     * @param record
     * @param logId
     */
    void end(BillPushToYsRecord record, String logId);

    /**
     * 异常
     *
     * @param record
     * @param logId
     * @param message
     */
    void error(BillPushToYsRecord record, String logId, String message);

    /**
     * 获取待推送的记录
     *
     * @return
     */
    List<BillPushToYsRecord> getWaitPushRecordList();
}
