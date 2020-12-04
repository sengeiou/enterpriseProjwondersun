package com.proj.service.impl;

import com.alibaba.fastjson.JSON;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebt.biz.billprocess.BillProcessContext;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebt.entity.BillData;
import com.arlen.ebt.entity.BillScanInfo;
import com.arlen.ebt.entity.ScanData;
import com.arlen.ebt.enums.BillScanInfoStatus;
import com.arlen.ebt.enums.BillStatus;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillScanInfoRepository;
import com.arlen.ebt.repository.ScanDataRepository;
import com.arlen.ebt.util.ComUtils;
import com.arlen.ebu.appservice.IdKeyAppService;
import com.proj.appservice.DealerSignTypeAppService;
import com.arlen.utils.common.StringUtils;
import com.arlen.ebt.service.impl.BillServiceImpl;
import com.proj.appservice.BillPushToYsRecordAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 重写处理单据后的逻辑
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */
public class OverrideBillServiceImpl extends BillServiceImpl {
    private static Logger logger = LoggerFactory.getLogger(OverrideBillServiceImpl.class);
    @Resource
    private BillPushToYsRecordAppService billPushToYsRecordAppService;
    @Resource
    private DealerSignTypeAppService dealerSignTypeAppService;
    @Resource
    private IdKeyAppService idKeyAppService;
    @Resource
    private ScanDataRepository scanDataRepository;
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private BillScanInfoRepository billScanInfoRepository;

    @Override
    public void projCustom(BillProcessContext billProcessContext) {
        //原单据
        BillHeader billHeader = billProcessContext.getBillInf().getBillHeader();
        //拆单
        List<BillHeader> splitedBillList = billProcessContext.getSplitedBillList();
        if (CollectionUtils.isEmpty(splitedBillList)) {
            //无拆单，则添加原单据
            APIResult<String> result = billPushToYsRecordAppService.create(billHeader.getBillTypeId(), billHeader.getId(), billHeader.getCode());
            logger.info("创建推送到YS的记录：billType=" + billHeader.getBillTypeId() + ",billCode=" + billHeader.getCode() + "结果,record=" + JSON.toJSONString(result));
        } else {
            //有拆单，则添加拆分后的单据
            for (BillHeader header : splitedBillList) {
                APIResult<String> result = billPushToYsRecordAppService.create(header.getBillTypeId(), header.getId(), header.getCode());
                logger.info("创建推送到YS的记录：billType=" + header.getBillTypeId() + ",billCode=" + header.getCode() + "结果,record=" + JSON.toJSONString(result));
            }
        }
        //调整单
        List<BillHeader> adjustedBillList = billProcessContext.getAdjustedBillList();
        if (!CollectionUtils.isEmpty(adjustedBillList)) {
            for (BillHeader header : adjustedBillList) {
                APIResult<String> result = billPushToYsRecordAppService.create(header.getBillTypeId(), header.getId(), header.getCode());
                logger.info("创建推送到YS的记录：billType=" + header.getBillTypeId() + ",billCode=" + header.getCode() + "结果,record=" + JSON.toJSONString(result));
            }
        }
        //强弱校验
        List<BillHeader> newMaterialBillList = billProcessContext.getNewMaterialBillList();
        if (!CollectionUtils.isEmpty(newMaterialBillList)) {
            for (BillHeader header : newMaterialBillList) {
                APIResult<String> result = billPushToYsRecordAppService.create(header.getBillTypeId(), header.getId(), header.getCode());
                logger.info("创建推送到YS的记录：billType=" + header.getBillTypeId() + ",billCode=" + header.getCode() + "结果,record=" + JSON.toJSONString(result));
            }
        }

    }

    /**
     * 项目自定义方法：是否自动签收
     *
     * @param billProcessContext 上下文
     */
    @Override
    public boolean projIsAutoReceive(BillProcessContext billProcessContext) {
        boolean isAutoReceive = false;
        BillHeader billHeader = billProcessContext.getBillInf().getBillHeader();
        //仓库销售出库单，根据经销商签收方式配置，判断是否自动签收
        String logMsg = "经销商入库单，自动签收：" + billHeader.getCode() + "，";
        logger.info(logMsg + "判断是否自动签收，开始");
        if (StringUtils.equals(ComUtils.RDC_TO_DEALER_OUT, billHeader.getBillTypeId())) {
            Organization destOrg = billProcessContext.getBillInf().getDestOrg();
            if (destOrg != null && destOrg.getOrgType() == OrgType.DEALER.index) {
                isAutoReceive = dealerSignTypeAppService.isAutoReceive(destOrg.getId()).getData();
            }
        }
        logger.info(logMsg + "是否自动签收：isAutoReceive=" + isAutoReceive);
        logger.info(logMsg + "判断是否自动签收，结束");
        return isAutoReceive;
    }

    /**
     * 项目自定义方法：自动发货门店
     *
     * @param billProcessContext 上下文
     */
    @Override
    public void projAutoDeliveryStore(BillProcessContext billProcessContext) {
        BillHeader billHeader = billProcessContext.getBillInf().getBillHeader();
        //经销商入库单，根据经销商签收方式配置，判断是否自动发货
        String logMsg = "经销商入库单，自动发货：" + billHeader.getCode() + "，";
        if (StringUtils.equals(ComUtils.DEALER_FROM_RDC_IN, billHeader.getBillTypeId())) {
            Organization destOrg = billProcessContext.getBillInf().getDestOrg();
            if (destOrg != null && destOrg.getOrgType() == OrgType.DEALER.index) {

                logger.info(logMsg + "判断是否自动签收，开始");
                String storeId = dealerSignTypeAppService.getAutoDeliveryStore(destOrg.getId()).getData();
                logger.info(logMsg + "是否自动签收：storeId=" + storeId);
                boolean isAutoDelivery = StringUtils.isNotEmpty(storeId) ? true : false;
                logger.info(logMsg + "是否自动签收：isAutoDelivery=" + isAutoDelivery);
                logger.info(logMsg + "判断是否自动签收，结束");

                if (StringUtils.isNotEmpty(storeId)) {
                    logger.info(logMsg + "读取自动发编码，开始");
                    //获取单据编码，自动发货到门店
                    List<BillData> billDataList = billProcessContext.getNormalDataList().stream().map(x -> x.getBillData()).collect(Collectors.toList());
                    if (billDataList == null || billDataList.isEmpty()) {
                        throw new RuntimeException("[" + destOrg.getId() + "]，经销商自动发货异常：收货编码为空，无法自动发货");
                    }
                    logger.info(logMsg + "billDataList.size=" + billDataList.size());
                    logger.info(logMsg + "读取自动发编码，结束");

                    logger.info(logMsg + "构建自动发货单，开始");
                    BillHeader newBillHeader = createBillByOriginBill(billHeader, storeId);
                    logger.info(logMsg + "newBillHeader=" + newBillHeader.getId() + "," + newBillHeader.getCode());
                    List<ScanData> scanDataList = createScanCode(billDataList, newBillHeader);
                    logger.info(logMsg + "scanDataList.size=" + scanDataList.size());
                    BillScanInfo billScanInfo = createBillScanInfo(newBillHeader, billDataList.size());
                    logger.info(logMsg + "billScanInfo=" + billScanInfo.getId());
                    logger.info(logMsg + "构建自动发货单，结束");

                    logger.info(logMsg + "保存自动发货单，开始");
                    scanDataRepository.insert(scanDataList);
                    billHeaderRepository.insert(newBillHeader);
                    billScanInfoRepository.insert(billScanInfo);
                    logger.info(logMsg + "保存自动发货单，结束");

                    logger.info(logMsg + "处理自动发货单，开始");
                    this.processBillData(newBillHeader.getId());
                    logger.info(logMsg + "处理自动发货单，结束");
                }
            }
        }
    }

    private BillHeader createBillByOriginBill(BillHeader originBillHeader, String storeId) {
        String code = idKeyAppService.generateOne(ComUtils.DEALER_TO_STORE_OUT).getData();
        BillHeader newBillHeader = new BillHeader();
        newBillHeader.setId(UUID.randomUUID().toString());
        newBillHeader.setCode(code);
        newBillHeader.setBillTypeId(ComUtils.DEALER_TO_STORE_OUT);
        newBillHeader.setSrcId(originBillHeader.getDestId());
        newBillHeader.setDestId(storeId);
        newBillHeader.setRefCode(originBillHeader.getRefCode());
        newBillHeader.setOperateTime(new Date());
        newBillHeader.setOperator(originBillHeader.getSubmitBy());
        newBillHeader.setAddFrom(originBillHeader.getAddFrom());
        newBillHeader.setSourceBillId(null);
        newBillHeader.setSourceBillCode(originBillHeader.getCode());
        newBillHeader.setProcessType(ComUtils.PROMPTLY);
        newBillHeader.setExpectQtyPcs(0);
        newBillHeader.setExpectQtyStr("0");
        newBillHeader.setExpectQty(BigDecimal.ZERO);
        newBillHeader.setBillStatus(BillStatus.Submitted.index);
        newBillHeader.setBillScanInfoId(UUID.randomUUID().toString());
        newBillHeader.setScanBy(originBillHeader.getScanBy());
        newBillHeader.setScanTime(new Date());
        newBillHeader.setSubmitBy(originBillHeader.getSubmitBy());
        newBillHeader.setSubmitTime(new Date());
        newBillHeader.setTerminalType(originBillHeader.getTerminalType());
        newBillHeader.setNewMaterial(originBillHeader.getNewMaterial());
        newBillHeader.setExt6(originBillHeader.getId());//收货单Id
        newBillHeader.setAddBy(originBillHeader.getAddBy());
        newBillHeader.setAddTime(new Date());
        newBillHeader.setEditBy(originBillHeader.getEditBy());
        newBillHeader.setEditTime(new Date());
        return newBillHeader;
    }

    private List<ScanData> createScanCode(List<BillData> billDataList, BillHeader newBillHeader) {
        List<ScanData> scanDataList = new ArrayList<>();
        for (BillData billData : billDataList) {
            ScanData scanData = new ScanData();
            scanData.setId(UUID.randomUUID().toString());
            scanData.setBatchCode(billData.getBatchCode());
            scanData.setCodePath(billData.getCodePath());
            scanData.setDetailId(null);
            scanData.setHeaderId(newBillHeader.getId());
            scanData.setInputType(billData.getInputType());
            scanData.setMaterialId(billData.getMaterialId());
            scanData.setPackDate(billData.getPackDate());
            scanData.setProductionCodeId(billData.getProductionCodeId());
            scanData.setQty(BigDecimal.ZERO);
            scanData.setQtyPcs(1);
            scanData.setRecordBillHeaderCode(null);
            scanData.setScanCode(billData.getProductionCodeId());
            scanData.setScanQty(1);
            scanData.setScanTime(billData.getScanTime());
            scanData.setValidDate(billData.getValidDate());
            scanData.setIsPolicy(null);
            scanData.setInFull(true);
            scanData.setAddBy(newBillHeader.getAddBy());
            scanData.setAddTime(new Date());
            scanData.setEditBy(newBillHeader.getEditBy());
            scanData.setEditTime(new Date());
            scanData.setRemark(null);
            scanDataList.add(scanData);
        }
        return scanDataList;
    }

    private BillScanInfo createBillScanInfo(BillHeader billHeader, int scanQty) {
        BillScanInfo billScanInfo = new BillScanInfo();
        billScanInfo.setId(billHeader.getBillScanInfoId());
        billScanInfo.setBillHeaderId(billHeader.getId());
        billScanInfo.setBillCode(billHeader.getCode());
        billScanInfo.setBillFromServer(true);
        billScanInfo.setBillType(billHeader.getBillTypeId());
        billScanInfo.setNoBill(false);
        billScanInfo.setProcessStatus(BillScanInfoStatus.SCANNING.index);
        billScanInfo.setScanCodeTotalQty(scanQty);
        billScanInfo.setOrgId(billHeader.getSrcId());
        billScanInfo.setSubmitBy(billHeader.getAddBy());
        billScanInfo.setSubmitTime(new Date());
        billScanInfo.setTerminalType(billHeader.getTerminalType());
        billScanInfo.setAddBy(billHeader.getAddBy());
        billScanInfo.setAddTime(new Date());
        billScanInfo.setEditBy(billHeader.getEditBy());
        billScanInfo.setEditTime(new Date());
        return billScanInfo;
    }

}
