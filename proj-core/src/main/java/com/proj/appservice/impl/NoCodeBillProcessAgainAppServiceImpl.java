package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebt.appservice.BillBizAppService;
import com.arlen.ebt.appservice.BillHeaderAppService;
import com.arlen.ebt.appservice.BillPromptlyBizAppService;
import com.arlen.ebt.billqueue.BillQueueItem;
import com.arlen.ebt.dto.BillHeaderDTO;
import com.arlen.ebt.dto.VerifiedCodeDTO;
import com.arlen.ebt.entity.*;
import com.arlen.ebt.enums.BillStatus;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillScanRecordRepository;
import com.arlen.ebt.repository.ExceptionDataRepository;
import com.arlen.ebt.service.BillQueueItemService;
import com.arlen.ebt.util.Utils;
import com.proj.appservice.NoCodeBillProcessAgainAppService;
import com.proj.repository.ProjBillHeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arlenChen on 2019/07/19.
 * 编码不存在异常单据重新处理
 *
 * @author arlenChen
 */
@Service
public class NoCodeBillProcessAgainAppServiceImpl implements NoCodeBillProcessAgainAppService {
    private static Logger logger = LoggerFactory.getLogger(NoCodeBillProcessAgainAppServiceImpl.class);
    @Resource
    private ProjBillHeaderRepository projBillHeaderRepository;
    @Resource
    private ExceptionDataRepository exceptionDataRepository;
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private BillHeaderAppService appService;
    @Resource
    private BillBizAppService billBizAppService;
    @Resource
    private BillPromptlyBizAppService billPromptlyBizAppService;
    @Resource
    private BillScanRecordRepository billScanRecordRepository;
    @Resource
    private BillQueueItemService billQueueItemService;

    /**
     * 重新处理单据
     */
    @Override
    public void noCodeBillProcessAgain() {
        logger.info("重新处理单据->查询重置单据");
        List<BillHeader> noCodeBillList = projBillHeaderRepository.noCodeBillList();
        List<BillHeader> saveList = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        logger.info("重新处理单据->开始重置单据");
        for (BillHeader billHeader : noCodeBillList) {
            logger.info("重新处理单据->查询异常数据：\"{}\"", billHeader.getCode());
            List<ExceptionData> exceptionDataList = exceptionDataRepository.listByHeaderId(billHeader.getId());
            List<ExceptionData> noCodeExceptionDList = exceptionDataList.stream().filter(x -> x.getExceptionCode() == 2).collect(Collectors.toList());
            logger.info("重新处理单据->查询衍生单据：\"{}\"", billHeader.getCode());
            List<BillHeader> headerList = billHeaderRepository.getBySourceId(billHeader.getId());
            BillHeader inHeader = headerList.stream().filter(x -> x.getBillStatus() != BillStatus.Audited.index).findFirst().orElse(null);
            if (inHeader != null) {
                logger.info("重新处理单据->单据重新处理：\"{}\"", billHeader.getCode());
                appService.uploadAgain(billHeader.getId());
            } else {
                logger.info("重新处理单据->新建-1单据：\"{}\"", billHeader.getCode());
                splitBill(billHeader, noCodeExceptionDList);
                billHeader.setAttr1("S" + format.format(new Date()));
                saveList.add(billHeader);
            }
        }
        billHeaderRepository.save(saveList);
        logger.info("重新处理单据->结束重置单据");
    }

    /**
     * 构建新单据信息
     *
     * @param originBillHeader 原始单据
     */
    private void splitBill(BillHeader originBillHeader, List<ExceptionData> noCodeExceptionDList) {
        logger.info("重新处理单据->新建-1单据->单头：\"{}\"", originBillHeader.getCode());
        //生成单据头
        BillHeaderDTO billHeaderDTO = new BillHeaderDTO();
        billHeaderDTO.setId(UUID.randomUUID().toString());
        billHeaderDTO.setCode(originBillHeader.getCode() + "-1");
        billHeaderDTO.setBillTypeId(originBillHeader.getBillTypeId());
        billHeaderDTO.setSrcId(originBillHeader.getSrcId());
        billHeaderDTO.setDestId(originBillHeader.getDestId());
        billHeaderDTO.setAddBy(originBillHeader.getAddBy());
        billHeaderDTO.setAddTime(new Date());
        billHeaderDTO.setEditTime(new Date());
        billHeaderDTO.setEditBy(originBillHeader.getEditBy());
        billHeaderDTO.setOperateTime(new Date());
        billHeaderDTO.setBillReason(originBillHeader.getBillReason());
        BillHeader splitHeader = createBillHeader(billHeaderDTO);
        //修改单据信息
        splitHeader.setRealQtyPcs(0);
        splitHeader.setExpectQtyPcs(0);
        splitHeader.setExpectQty(BigDecimal.ZERO);
        splitHeader.setExpectQtyPcs(0);
        splitHeader.setRealQty(BigDecimal.ZERO);
        //设置实际箱数字符串
        splitHeader.setExpectQtyStr("");
        splitHeader.setRealQtyStr(splitHeader.getExpectQtyStr());
        splitHeader.setExceptionQtyPcs(0);
        splitHeader.setBillStatus(BillStatus.Submitted.index);
        splitHeader.setSubmitTime(new Date());
        //相关单号为原始单号
        splitHeader.setSourceBillCode(originBillHeader.getCode());
        splitHeader.setSubmitBy(originBillHeader.getSubmitBy());
        splitHeader.setSourceBillId(originBillHeader.getId());
        splitHeader.setRefCode(originBillHeader.getCode());
        //统计扫码时间，和原单的信息一致
        splitHeader.setBillScanInfoId(originBillHeader.getBillScanInfoId());
        splitHeader.setScanBy(originBillHeader.getScanBy());
        splitHeader.setScanTime(originBillHeader.getScanTime());
        splitHeader.setScanBeginTime(originBillHeader.getScanBeginTime());
        splitHeader.setScanEndTime(originBillHeader.getScanEndTime());
        splitHeader.setTerminalType(originBillHeader.getTerminalType());
        splitHeader.setAddFrom(originBillHeader.getAddFrom());
        //设置扫描码数量,和原单据一致
        splitHeader.setScanCount(originBillHeader.getScanCount());
        billHeaderRepository.insert(splitHeader);
        //校验码并生成明细
        logger.info("重新处理单据->新建-1单据->异常码校验并生成明细：\"{}\"",originBillHeader.getCode());
        APIResult<VerifiedCodeDTO> result;
        for (ExceptionData exceptionData : noCodeExceptionDList) {
            result = billBizAppService.getVerifiedCode(splitHeader.getBillTypeId(), exceptionData.getScanCode(), splitHeader.getSrcId(), false, false,null);
            //校验成功后直接保存添加到单据中
            if (result.getCode() == 0) {
                billPromptlyBizAppService.addBillData(splitHeader.getId(), result.getData(), splitHeader.getScanBy(), "", 0);
            }
        }
        logger.info("重新处理单据->新建-1单据->提交记录：\"{}\"",originBillHeader.getCode());
        buildBillScanRecord(splitHeader);
        logger.info("重新处理单据->新建-1单据->BillCommand信息：\"{}\"",originBillHeader.getCode());
        saveBillCommand(splitHeader);
    }

    private BillHeader createBillHeader(BillHeaderDTO billHeaderDTO) {
        BillHeader billHeader = Utils.getDozerBeanMapper().map(billHeaderDTO, BillHeader.class);
        if (StringUtil.isEmpty(billHeaderDTO.getId())) {
            billHeader.setId(UUID.randomUUID().toString());
        }
        return billHeader;
    }

    private void buildBillScanRecord(BillHeader splitHeader) {
        // region 生成提交（上传）记录  (单据扫码记录)
        BillScanRecord billScanRecord = new BillScanRecord();
        billScanRecord.setId(UUID.randomUUID().toString());
        billScanRecord.setBillScanInfoId(null);
        billScanRecord.setScanCodeQty(splitHeader.getScanQty());
        billScanRecord.setTerminalId("");
        billScanRecord.setAttr1("");
        billScanRecord.setTerminalType(splitHeader.getTerminalType());
        billScanRecord.setAddTime(new Date());
        billScanRecord.setAddBy(splitHeader.getSubmitBy());
        billScanRecord.setEditTime(new Date());
        billScanRecord.setEditBy(splitHeader.getSubmitBy());
        //持久化
        billScanRecordRepository.insert(billScanRecord);
        //endregion
    }

    /**
     * 保存BillCommand信息
     *
     * @param billHeader 单据
     */
    private void saveBillCommand(BillHeader billHeader) {
        BillQueueItem billQueueItem = new BillQueueItem();
        //队列处理类型
        billQueueItem.setCommandType("bill");
        //扫码信息ID
        billQueueItem.setUploadRecordId(null);
        billQueueItem.setOrgId(billHeader.getSrcId());
        billQueueItem.setAddTime(new Date());
        billQueueItem.setBillType(billHeader.getBillTypeId());
        billQueueItem.setBillCode(billHeader.getCode());
        billQueueItem.setBillId(billHeader.getId());
        //添加到队列
        billQueueItemService.add(billQueueItem);
    }
}
