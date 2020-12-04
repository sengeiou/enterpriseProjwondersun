package com.proj.service.impl;

import com.alibaba.fastjson.JSON;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebt.biz.billprocess.STBillProcessContext;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.entity.STBillHeader;
import com.arlen.ebt.service.impl.STBillServiceImpl;
import com.proj.appservice.BillPushToYsRecordAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by arlenChen on 2020/1/17.
 * 盘点处理数据项目定制
 *
 * @author arlenChen
 */
public class ProjSTBillServiceImpl extends STBillServiceImpl {
    private static Logger logger = LoggerFactory.getLogger(ProjSTBillServiceImpl.class);

    @Resource
    private BillPushToYsRecordAppService billPushToYsRecordAppService;

    /**
     * 项目自定义方法
     *
     * @param billProcessContext 上下文
     */
    @Override
    public void projCustom(STBillProcessContext billProcessContext) {
        STBillHeader stBillHeader = billProcessContext.getBillInf().getBillHeader();
        //创建推送到YS的记录
        APIResult<String> result = billPushToYsRecordAppService.create("STBill", stBillHeader.getId(), stBillHeader.getCode());
        logger.info("创建推送到YS的记录-->盘点信息：billType=STBill,billCode=\"{}\"结果,record=\"{}\"", stBillHeader.getCode(), JSON.toJSONString(result));
        List<BillHeader> adjustedBillList = billProcessContext.getAdjustedBillList();
        for (BillHeader header : adjustedBillList) {
            APIResult<String> apiResult = billPushToYsRecordAppService.create(header.getBillTypeId(), header.getId(), header.getCode());
            logger.info("创建推送到YS的记录-->盘点调整信息：billType=\"{}\",billCode=\"{}\",结果,record=\"{}\"", header.getBillTypeId(), header.getCode(), JSON.toJSONString(apiResult));
        }
    }
}
