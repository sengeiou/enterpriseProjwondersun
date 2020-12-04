package com.proj.appservice.impl;

import com.alibaba.druid.util.StringUtils;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebms.utils.DateTimeTool;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebt.entity.*;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.STBillHeaderRepository;
import com.arlen.ebt.util.Utils;
import com.proj.appservice.BillPushToYsRecordAppService;
import com.arlen.proj.dto.ys.*;
import com.proj.dto.ys.*;
import com.proj.entity.BillPushToYsRecord;
import com.proj.enums.PushType;
import com.proj.repository.BillPushToYsRecordRepository;
import com.proj.repository.CheckInventoryRepository;
import com.proj.repository.DeliveryRepository;
import com.proj.repository.WonderSynOpenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */
@Service
public class BillPushToYsRecordAppServiceImpl implements BillPushToYsRecordAppService {
    private static Logger logger = LoggerFactory.getLogger(BillPushToYsRecordAppService.class);
    @Resource
    private BillPushToYsRecordRepository repository;
    @Resource
    private DeliveryRepository deliveryRepository;
    @Resource
    private CheckInventoryRepository checkInventoryRepository;
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private STBillHeaderRepository stBillHeaderRepository;
    @Resource
    private WonderSynOpenRepository wonderSynOpenRepository;
    @Resource
    private SysParaRepository sysParaRepository;

    private static String logMsg = "推送单据[{},{}]到美驰(YS)系统.LogId={}";

    @Override
    public APIResult<String> create(String type, String mainId, String remark) {
        APIResult<String> result = new APIResult<>();
        if (StringUtils.isEmpty(type)) {
            return result.fail(500, "type is empty");
        }
        if (StringUtils.isEmpty(mainId)) {
            return result.fail(500, "mainId is empty");
        }
        BillPushToYsRecord record = new BillPushToYsRecord();
        record.setId(UUID.randomUUID().toString());
        record.setAddBy("eh");
        record.setAddTime(new Date());
        record.setEditBy("eh");
        record.setEditTime(new Date());
        if (StringUtils.equals("STBill", type)) {
            record.setPushType(PushType.ST_BILL);
        } else {
            record.setPushType(PushType.BILL);
        }
        record.setPushMainId(mainId);
        record.setPushStatus(0);
        record.setRemark(remark);
        repository.insert(record);

        return result.succeed().attachData(record.getId());
    }

    @Override
    public void begin(BillPushToYsRecord record, String logId) {
        logger.info("计划-" + logMsg, record.getPushMainId(), record.getRemark(), logId);
        record.setPushBeginTime(new Date());
        record.setPushStatus(1);
        repository.save(record);
    }

    @Override
    public void doPush(BillPushToYsRecord record, String logId) {
        try {
            logger.info("开始-" + logMsg, record.getPushMainId(), record.getRemark(), logId);
            if (StringUtils.isEmpty(record.getPushMainId())) {
                throw new RuntimeException("单据Id为空");
            }
            //判断推送类型
            if (StringUtils.equals(record.getPushType(), PushType.BILL)) {
                //region 发货单推送
                BillHeader header = billHeaderRepository.load(record.getPushMainId());
                if (header == null) {
                    throw new RuntimeException("单据不存在");
                }
                int count = deliveryRepository.countById(record.getPushMainId());
                if (count != 0) {
                    throw new RuntimeException("单据重复推送");

                }
                PushDeliveryDTO pushDeliveryDTO = buildPushDeliveryDTO(header);
                deliveryRepository.inserMcTable(pushDeliveryDTO);
                //endregion
            } else if (StringUtils.equals(record.getPushType(), PushType.ST_BILL)) {
                //region 盘点单推送
                STBillHeader header = stBillHeaderRepository.load(record.getPushMainId());
                if (header == null) {
                    throw new RuntimeException("单据不存在");
                }
                int count = checkInventoryRepository.countById(record.getPushMainId());
                if (count != 0) {
                    throw new RuntimeException("单据重复推送");
                }
                PushCheckInventoryDTO pushCheckInventoryDTO = buildPushCheckInventoryDTO(header);
                checkInventoryRepository.inserMcTable(pushCheckInventoryDTO);
                //endregion
            } else {
                throw new RuntimeException("暂无[" + record.getPushType() + "]单据类型推送");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void end(BillPushToYsRecord record, String logId) {
        logger.info("完成-" + logMsg, record.getPushMainId(), record.getRemark(), logId);
        record.setPushEndTime(new Date());
        record.setPushStatus(2);
        repository.save(record);
    }

    @Override
    public void error(BillPushToYsRecord record, String logId, String message) {
        logger.info("异常-" + logMsg + "," + message, record.getPushMainId(), record.getRemark(), logId);
        record.setPushEndTime(new Date());
        record.setPushStatus(3);
        record.setErrMsg(message);
        repository.save(record);
    }


    private PushDeliveryDTO buildPushDeliveryDTO(BillHeader header) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        //region 构造DeliveryDTO
        DeliveryDTO delivery = Utils.getDozerBeanMapper().map(header, DeliveryDTO.class);
        //endregion
        //region 构造DeliveryDetailDTO集合
        List<DeliveryDetailDTO> deliveryDetailList = new ArrayList<>();
        for (BillDetail detail : header.getDetailList()) {
            DeliveryDetailDTO dto = Utils.getDozerBeanMapper().map(detail, DeliveryDetailDTO.class);
            dto.setHeaderId(header.getId());
            dto.setMaterialId(detail.getMaterial().getId());
            if (dto.getAddTime() == null) {
                dto.setAddTime(header.getAddTime());
            }
            if (dto.getEditTime() == null) {
                dto.setEditTime(header.getEditTime());
            }
            deliveryDetailList.add(dto);
        }
        //endregion
        //region 构造DeliveryCodeLibDTO集合
        List<BillData> billDataList = wonderSynOpenRepository.getBillData(header.getId());
        List<DeliveryCodeLibDTO> deliveryCodeLibList = new ArrayList<>();
        for (BillData data : billDataList) {
            if (StringUtils.isEmpty(data.getCodePath())) {
                data.setCodePath("");
            }
            deliveryCodeLibList.add(Utils.getDozerBeanMapper().map(data, DeliveryCodeLibDTO.class));
        }
        //endregion
        //返回对象
        PushDeliveryDTO pushDeliveryDTO = new PushDeliveryDTO();
        pushDeliveryDTO.setDelivery(delivery);
        pushDeliveryDTO.setDeliveryDetailList(deliveryDetailList);
        pushDeliveryDTO.setDeliveryCodeLibList(deliveryCodeLibList);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("    构造DeliveryDTO:id=" + header.getId() + ",耗时：" + interval + "秒。");
        return pushDeliveryDTO;
    }

    private PushCheckInventoryDTO buildPushCheckInventoryDTO(STBillHeader header) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        //region 构造CheckInventoryDTO
        CheckInventoryDTO checkInventory = Utils.getDozerBeanMapper().map(header, CheckInventoryDTO.class);
        //endregion
        //region 构造CheckInventoryDetailDTO集合
        List<CheckInventoryDetailDTO> checkInventoryDetailList = new ArrayList<>();
        for (STBillDetail detail : header.getDetailList()) {
            CheckInventoryDetailDTO dto = Utils.getDozerBeanMapper().map(detail, CheckInventoryDetailDTO.class);
            dto.setHeaderId(header.getId());
            dto.setAddTime(header.getAddTime());
            dto.setEditTime(header.getEditTime());
            checkInventoryDetailList.add(dto);
        }
        //endregion
        //region 构造CheckInventoryCodeLibDTO集合
        List<STBillData> billDataList = wonderSynOpenRepository.getSTBillData(header.getId());
        List<CheckInventoryCodeLibDTO> checkInventoryCodeLibList = new ArrayList<>();
        for (STBillData data : billDataList) {
            checkInventoryCodeLibList.add(Utils.getDozerBeanMapper().map(data, CheckInventoryCodeLibDTO.class));
        }
        //endregion
        //返回对象
        PushCheckInventoryDTO pushCheckInventoryDTO = new PushCheckInventoryDTO();
        pushCheckInventoryDTO.setCheckInventory(checkInventory);
        pushCheckInventoryDTO.setCheckInventoryDetailList(checkInventoryDetailList);
        pushCheckInventoryDTO.setCheckInventoryCodeLibList(checkInventoryCodeLibList);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("    构造CheckInventoryDTO:id=" + header.getId() + ",耗时：" + interval + "秒。");
        return pushCheckInventoryDTO;
    }

    /**
     * 获取待推送的记录
     *
     * @return
     */
    @Override
    public List<BillPushToYsRecord> getWaitPushRecordList() {
        int size = 5;
        SysPara sysPara = sysParaRepository.load("pushBillToMcSize");
        if (sysPara != null && !StringUtils.isEmpty(sysPara.getValue())) {
            size = Integer.parseInt(sysPara.getValue());
        }
        return repository.getWaitPushRecordList(size);
    }
}
