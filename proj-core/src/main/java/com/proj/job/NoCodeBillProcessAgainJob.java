package com.proj.job;

import com.arlen.ebu.job.AbstractJob;
import com.proj.appservice.NoCodeBillProcessAgainAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Created by arlenChen on 2019/07/19.
 * 编码不存在异常单据重新处理JOB
 *
 * @author arlenChen
 */
public class NoCodeBillProcessAgainJob extends AbstractJob {
    private static transient final Logger logger = LoggerFactory.getLogger(NoCodeBillProcessAgainJob.class);
    @Resource
    private NoCodeBillProcessAgainAppService appService;

    @Override
    protected void executeBusiness(String s) {
        logger.info("Name: \" {}\"   重新处理单据   . . .", s);
        try {
            appService.noCodeBillProcessAgain();
        } catch (Exception e) {
            logger.error("重新处理单据----处理异常", e);
        }
    }
}
