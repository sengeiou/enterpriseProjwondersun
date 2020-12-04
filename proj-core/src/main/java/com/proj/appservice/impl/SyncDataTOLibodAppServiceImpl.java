package com.proj.appservice.impl;

import com.alibaba.fastjson.JSON;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ImportCodeRecordRepository;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebd.dto.MaterialDTO;
import com.arlen.ebd.dto.SimpleTypeDTO;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.entity.SimpleType;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebd.repository.SimpleTypeRepository;
import com.proj.appservice.SyncDataTOLibodAppService;
import com.proj.repository.ProjImportCodeRecordRepository;
import com.proj.repository.SyncDataToLibodRepository;
import com.proj.webservice.impl.SyncData;
import com.proj.webservice.impl.SyncDataService;
import org.dozer.DozerBeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ehsure on 2018/4/17.
 * 同步数据到力博
 */
@Service
public class SyncDataTOLibodAppServiceImpl implements SyncDataTOLibodAppService {
    private static transient final Logger logger = LoggerFactory.getLogger(SyncDataTOLibodAppServiceImpl.class);
    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource()
    private SyncDataToLibodRepository syncDataToLibodRepository;
    @Resource()
    private MaterialRepository materialRepository;
    @Resource()
    private ImportCodeRecordRepository importCodeRecordRepository;
    @Resource()
    private SimpleTypeRepository simpleTypeRepository;
    @Resource()
    private ProjImportCodeRecordRepository projImportCodeRecordRepository;
    @Resource()
    private ProductionCodeRepository repository;

    @Override
    public APIResult<String> syncProductionCode() throws Exception {
        APIResult<String> result = new APIResult<>();
        try {
            logger.info("同步生产数据信息---开始同步生产数据信息");
            String qty = sysProperties.getProperty("proj.sync.SaleDataQty", null);//每次同步的数量
            if (StringUtil.isEmpty(qty)) {
                qty = "10000";
            }
            List<ProductionCode> list = syncDataToLibodRepository.getSyncProductionCode(Integer.parseInt(qty));
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            int continueQty = 0;
            if (list.size() == 0) {
                logger.info("同步生产数据信息---没有需要同步的数据");
                return result.fail(500, "同步成功");
            }
            logger.info("同步生产数据信息---产品不存在的共有" + continueQty + "个批次");
            int num = list.size() / 5000;
            int surplus = list.size() % 5000;
            String status = "";
            SyncData syncData = new SyncData();
            SyncDataService service = syncData.getSyncDataServiceImplPort();
            if (num > 0) {
                for (int i = 1; i <= num; i++) {
                    List<ProductionCode> subList = list.subList((i - 1) * 5000, i * 5000);
                    String jsonStr = JSON.toJSONString(subList);//构建要上传的数据
                    logger.info("同步生产数据信息---开始第" + i + "次数据上传");
                    status = service.syncProductionCode(jsonStr);
                    if ("OK".equals(status)) {
                        logger.info("同步生产数据信息---第" + i + "次数据上传成功");
                    } else {
                        logger.info("同步生产数据信息---第" + i + "次数据上传失败");
                        break;
                    }
                }
            }
            if (surplus > 0) {
                List<ProductionCode> subList = list.subList(num * 5000, list.size());
                String jsonStr = JSON.toJSONString(subList);//构建要上传的数据
                logger.info("同步生产数据信息---开始数据上传");
                status = service.syncProductionCode(jsonStr);
                if ("OK".equals(status)) {
                    logger.info("同步生产数据信息--数据上传成功");
                } else {
                    logger.info("同步生产数据信息---数据上传失败");
                }
            }
            String statusStr;
            if ("OK".equals(status)) {
                statusStr = "succeed;";
                result.succeed();
            } else {
                statusStr = "failed;";
                result.fail(500, "同步失败");
            }
            for (ProductionCode record : list) {
                record.setAttr2(statusStr + format2.format(new Date()));
            }
            repository.save(list);
            return result;
        } catch (Exception e) {
            logger.error("同步生产数据信息---同步失败", e);
            return result.fail(500, e.getMessage());
        }
    }

    /**
     * 同步系列信息
     *
     * @return 处理结果
     */
    @Override
    public APIResult<String> syncSeries() throws Exception {
        APIResult<String> result = new APIResult<>();
        try {
            logger.info("同步系列信息---开始同步生产数据信息");
            List<SimpleType> list = simpleTypeRepository.getListByCategoryId("MATERIALSERIES");
            SimpleType type = simpleTypeRepository.load("MATERIALSERIES");
            if (type != null) {
                list.add(type);
            }
            List<SimpleType> notSyncList = list.stream().filter(x -> StringUtil.isEmpty(x.getAttr1())).collect(Collectors.toList());
            List<SimpleTypeDTO> dtoList = new ArrayList<>();
            //根据批次和产品id分组，根据分组结果进行码的操作
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            if (notSyncList.size() == 0) {
                logger.info("同步系列信息---没有需要同步的数据");
                return result.succeed().attachData("同步成功");
            }
            //修改生产记录状态变为“上传中”
            DozerBeanMapper mapper = new DozerBeanMapper();
            for (SimpleType simpleType : notSyncList) {
                SimpleTypeDTO dto = mapper.map(simpleType, SimpleTypeDTO.class);
                if (simpleType.getParentSimpleType() != null) {
                    dto.setCategoryId(simpleType.getParentSimpleType().getId());
                }
                dtoList.add(dto);
                simpleType.setAttr1("waiting");
            }
            simpleTypeRepository.save(notSyncList);
            SyncData syncData = new SyncData();
            SyncDataService service = syncData.getSyncDataServiceImplPort();
            String xmlStr = JSON.toJSONString(dtoList);//构建要上传的数据
            logger.info("同步系列信息---开始数据上传");
            logger.info("同步系列信息---数据格式为：" + xmlStr);
            String status = service.syncSeries(xmlStr);
            logger.info("同步系列信息--数据状态码" + status);
            String statusStr;
            if (!"OK".equals(status)) {
                statusStr = "failed;";
                result.fail(500, "同步失败");
            } else {
                statusStr = "succeed;";
                result.succeed();
            }
            for (SimpleType simpleType : notSyncList) {
                simpleType.setAttr1(statusStr + format2.format(new Date()));
            }
            simpleTypeRepository.save(notSyncList);
            return result;
        } catch (Exception e) {
            logger.error("同步系列信息---同步失败", e);
            return result.fail(500, e.getMessage());
        }
    }

    /**
     * 同步产品信息信息
     *
     * @return 处理结果
     */
    @Override
    public APIResult<String> syncMaterial() throws Exception {
        APIResult<String> result = new APIResult<>();
        try {
            logger.info("同步产品信息信息---开始同步生产数据信息");
            List<Material> list = syncDataToLibodRepository.getSyncList();
            //根据批次和产品id分组，根据分组结果进行码的操作
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            if (list.size() == 0) {
                logger.info("同步产品信息信息---没有需要同步的数据");
                return result.succeed().attachData("同步成功");
            }
            List<MaterialDTO> dtoList = new ArrayList<>();
            DozerBeanMapper mapper = new DozerBeanMapper();
            //修改生产记录状态变为“上传中”
            for (Material material : list) {
                MaterialDTO dto = mapper.map(material, MaterialDTO.class);
                dtoList.add(dto);
                material.setExt12("waiting");
            }
            materialRepository.save(list);
            String jsonStr = JSON.toJSONString(dtoList);//构建要上传的数据
            logger.info("同步产品信息信息---开始数据上传");
            logger.info("同步产品信息信息---数据格式为：" + jsonStr);
            SyncData syncData = new SyncData();
            SyncDataService service = syncData.getSyncDataServiceImplPort();
            String status = service.syncMaterial(jsonStr);
            String statusStr;
            if (!"OK".equals(status)) {
                statusStr = "failed;";
                result.fail(500, "同步失败");
                logger.info("同步产品信息信息---数据上传失败");
            } else {
                logger.info("同步产品信息信息--数据上传成功");
                statusStr = "succeed;";
                result.succeed();
            }
            for (Material material : list) {
                material.setExt12(statusStr + format2.format(new Date()));
            }
            materialRepository.save(list);
            return result;
        } catch (Exception e) {
            logger.error("同步产品信息信息---同步失败", e);
            return result.fail(500, e.getMessage());
        }
    }
}
