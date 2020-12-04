package com.proj.job;

import com.alibaba.fastjson.JSON;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebu.job.AbstractJob;
import com.proj.appservice.WonderSunOpenAppService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Created by arlenChen on 2019/12/18.
 * 同步产品信息到美驰
 *
 * @author arlenChen
 */
public class SyncMeiChiMaterialJob extends AbstractJob {
    private static transient final Logger logger = LoggerFactory.getLogger(SyncMeiChiMaterialJob.class);
    @Resource
    private WonderSunOpenAppService appService;

    @Override
    protected void executeBusiness(String s) {
        APIResult<String> result = new APIResult<>();
        logger.info("{}-同步产品信息到美驰--->>开始......", s);
        try {
            result = appService.syncMaterialToMidTable();
        } catch (Exception e) {
            logger.error("{}-同步产品信息到美驰--->>同步异常", s, e);
            result.fail(500, StringUtils.isEmpty(e.getMessage()) ? "java.lang.NullPointerException" : e.getMessage());
        } finally {
            logger.info("{}-同步产品信息到美驰--->>返回结果：{}", s, JSON.toJSONString(result));
            logger.info("{}-同步产品信息到美驰--->>结束......", s);
        }
    }
}
