package com.proj.job;

import com.arlen.ebu.job.AbstractJob;
import com.proj.appservice.SyncDataTOLibodAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2016/11/30.
 * 同步生产数据信息
 */
@Component("SyncCodeJob")
public class SyncCodeJob extends AbstractJob {
    private static transient final Logger logger = LoggerFactory.getLogger(SyncCodeJob.class);
    @Resource
    private SyncDataTOLibodAppService appService;

    @Override
    protected void executeBusiness(String s) {
        logger.info("Name: \" {}\"   开始同步生产数据信息   . . .", s);
        try {
            appService.syncProductionCode();
        } catch (Exception e) {
            logger.error("同步生产数据信息----同步异常", e);
        }
    }
}
