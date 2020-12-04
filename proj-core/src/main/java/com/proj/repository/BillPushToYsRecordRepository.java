package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.proj.entity.BillPushToYsRecord;

import java.util.List;

/**
 * 单据推送到美驰系统记录
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */

public interface BillPushToYsRecordRepository extends BaseCrudRepository<BillPushToYsRecord> {

    /**
     * 获取待推送的记录
     *
     * @param size
     * @return
     */
    List<BillPushToYsRecord> getWaitPushRecordList(int size);


}
