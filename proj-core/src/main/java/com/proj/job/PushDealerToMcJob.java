package com.proj.job;

import com.arlen.ebu.job.AbstractJob;
import com.proj.service.PushRetailerToMcJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 经销商-基础数据-接口对接(美驰)
 */
public class PushDealerToMcJob extends AbstractJob {

    private static transient final Logger logger = LoggerFactory.getLogger(PushDealerToMcJob.class);

    @Resource
    private PushRetailerToMcJobService pushRetailerToMcJobService;

    @Override
    synchronized protected void executeBusiness(String jobName) {
        Map<String,Object> resMap = pushRetailerToMcJobService.pushDealer();
        String msg = (String) resMap.get("msg");
        logger.info("经销商-基础数据-接口对接(美驰)-PushDealerToMcJob-" + msg);
    }


}
