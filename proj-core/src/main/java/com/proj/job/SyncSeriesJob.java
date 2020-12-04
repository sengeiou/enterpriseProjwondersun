package com.proj.job;

import com.arlen.ebu.job.AbstractJob;
import com.proj.appservice.SyncDataTOLibodAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2016/11/30.
 * 同步系列信息
 */
@Component("SyncSeriesJob")
public class SyncSeriesJob extends AbstractJob {
    private static transient final Logger logger = LoggerFactory.getLogger(SyncSeriesJob.class);
    @Resource
    private SyncDataTOLibodAppService appService;

    @Override
    protected void executeBusiness(String s) {
        logger.info("Name: \" {}\"   开始同步系列信息   . . .", s);
        try {
            appService.syncSeries();
        } catch (Exception e) {
            logger.error("同步系列信息----同步异常", e);
        }
    }
}
