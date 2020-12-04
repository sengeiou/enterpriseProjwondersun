package com.proj.job;

import com.arlen.ebu.job.AbstractJob;
import com.proj.service.PushRetailerToMcJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 仓库-基础数据-接口对接(美驰)
 */
public class PushWarehouseToMcJob extends AbstractJob {

    private static transient final Logger logger = LoggerFactory.getLogger(PushWarehouseToMcJob.class);

    @Resource
    private PushRetailerToMcJobService pushRetailerToMcJobService;

    @Override
    synchronized protected void executeBusiness(String jobName) {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushWarehouse();
        String msg = (String) resMap.get("msg");
        logger.info("仓库-基础数据-接口对接(美驰)-PushWarehouseToMcJob-" + msg);
    }


}
