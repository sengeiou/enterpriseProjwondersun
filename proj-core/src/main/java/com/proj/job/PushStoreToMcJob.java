package com.proj.job;

import com.ebu.job.AbstractJob;
import com.proj.service.PushRetailerToMcJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 门店-基础数据-接口对接(美驰)
 */
public class PushStoreToMcJob extends AbstractJob {

    private static transient final Logger logger = LoggerFactory.getLogger(PushStoreToMcJob.class);

    @Resource
    private PushRetailerToMcJobService pushRetailerToMcJobService;

    @Override
    synchronized protected void executeBusiness(String jobName) {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushStore();
        String msg = (String) resMap.get("msg");
        logger.info("门店-基础数据-接口对接(美驰)-PushStoreToMcJob-" + msg);
    }


}
