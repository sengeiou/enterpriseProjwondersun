package com.proj.job;

import com.arlen.ebu.job.AbstractJob;
import com.proj.appservice.SyncDataTOLibodAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by ehsure on 2016/11/30.
 * 同步产品信息信息
 */
@Component("SyncMaterialJob")
public class SyncMaterialJob extends AbstractJob {
    private static transient final Logger logger = LoggerFactory.getLogger(SyncMaterialJob.class);
    @Resource
    private SyncDataTOLibodAppService appService;

    @Override
    protected void executeBusiness(String s) {
        logger.info("Name: \" {}\"   开始同步产品信息信息   . . .", s);
        try {
            appService.syncMaterial();
        } catch (Exception e) {
            logger.error("同步产品信息信息----同步异常", e);
        }
    }
}
