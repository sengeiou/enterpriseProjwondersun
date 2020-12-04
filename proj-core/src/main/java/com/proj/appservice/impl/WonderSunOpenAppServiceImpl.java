package com.proj.appservice.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebd.dto.MaterialDTO;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.entity.MaterialImage;
import com.arlen.ebd.entity.SimpleType;
import com.arlen.ebd.repository.MaterialImageRepository;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebd.repository.SimpleTypeRepository;
import com.arlen.ebp.dto.SysGeoCityDTO;
import com.arlen.ebp.dto.SysStructureDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebt.entity.BillDetail;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.entity.BillScanInfo;
import com.arlen.ebt.entity.ScanData;
import com.arlen.ebt.enums.BillScanInfoStatus;
import com.arlen.ebt.enums.BillStatus;
import com.arlen.ebt.repository.BillScanInfoRepository;
import com.arlen.ebt.repository.ScanDataRepository;
import com.arlen.ebt.service.BillService;
import com.arlen.ebt.util.ComUtils;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.appservice.ProjMaterialAppService;
import com.proj.appservice.WonderSunOpenAppService;
import com.proj.dto.BillSyncDTO;
import com.proj.dto.ProductionCodeQueryDTO;
import com.proj.dto.ProjSyncBillContext;
import com.proj.dto.RecycleBillDTO;
import com.proj.repository.ProjBillHeaderRepository;
import com.proj.repository.WonderSynOpenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arlenChen on 2016/12/23.
 * 对外数据
 *
 * @author arlenChen
 */
@Service
public class WonderSunOpenAppServiceImpl implements WonderSunOpenAppService {
    private static Logger log = LoggerFactory.getLogger(WonderSunOpenAppServiceImpl.class);
    @Resource
    private ProductionCodeRepository productionCodeRepository;
    @Resource
    private MaterialRepository materialRepository;
    @Resource
    private SimpleTypeRepository simpleTypeRepository;
    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private ProjMaterialAppService projMaterialAppService;
    @Resource
    private MaterialImageRepository materialImageRepository;
    @Resource
    private ProjBillHeaderRepository billHeaderRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private WonderSynOpenRepository openRepository;
    @Resource
    private ScanDataRepository scanDataRepository;
    @Resource
    private BillService billService;
    @Resource
    private BillScanInfoRepository billScanInfoRepository;
    @Resource(name = "sysProperties")
    private Properties sysProperties;

    private static final String DEFAULT_IMG_PATH = "/material_Img/";
    private static final String SEPARATOR = "/";
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private static final String SYNC_SAP = "SYNC_SAP";

    /**
     * 根据单品码查询基础信息
     *
     * @param qrId 单品码
     * @return 返回结果
     */
    @Override
    public APIResult<ProductionCodeQueryDTO> getProductionCodeByQRId(String qrId) {
        APIResult<ProductionCodeQueryDTO> result = new APIResult<>();
        try {
            ProductionCodeQueryDTO queryDTO = new ProductionCodeQueryDTO();
            ProductionCode productionCode = productionCodeRepository.load(qrId);
            if (productionCode == null) {
                return result.fail(404, "单品码不存在");
            } else {
                if (StringUtil.isNotEmpty(productionCode.getMaterialId())) {
                    Material material = materialRepository.load(productionCode.getMaterialId());
                    if (material != null) {
                        queryDTO.setFullName(material.getFullName());
                        if (StringUtil.isNotEmpty(material.getSpecId())) {
                            SimpleType simpleType = simpleTypeRepository.load(material.getSpecId());
                            if (simpleType != null) {
                                queryDTO.setSpec(simpleType.getName() + "g");
                            }
                        }
                        queryDTO.setShelfLife(material.getShelfLife() + "个月");
                        queryDTO.setStandardNumber(material.getAttr3());
                        queryDTO.setLicenseNumber(material.getAttr4());
                        queryDTO.setProductionSite(material.getExt2());
                        queryDTO.setUseAge(material.getExt1());
                        queryDTO.setExplain(material.getExt5());
                        List<Map<String, String>> fileList = new ArrayList<>();
                        List<MaterialImage> materialImageList = materialImageRepository.getByMaterial(material.getId());
                        SysPara sysPara = sysParaRepository.load("webUrl");
                        String webUrlStr = "";
                        if (sysPara != null && StringUtils.isNotEmpty(sysPara.getValue())) {
                            webUrlStr = sysPara.getValue();
                        }
                        String fileBaseDir = projMaterialAppService.getUploadDir().replace("/", SEPARATOR).replace("\\", SEPARATOR);
                        for (MaterialImage image : materialImageList) {
                            if (StringUtils.isNotEmpty(image.getImgPath())) {
                                String webUrl = image.getImgPath().replace(fileBaseDir, webUrlStr + DEFAULT_IMG_PATH);
                                webUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
                                Map<String, String> map = new HashMap<>(1);
                                map.put(image.getAttr1(), webUrl);
                                fileList.add(map);
                            }
                        }
                        queryDTO.getFileList().addAll(fileList);
                    }
                }
                queryDTO.setCode(productionCode.getId());
                queryDTO.setPackDate(format.format(productionCode.getPackDate()));
                SysPara sysPara = sysParaRepository.load("ProducerName");
                if (sysPara != null) {
                    queryDTO.setProducerName(sysPara.getValue());
                }
                return result.succeed().attachData(queryDTO);
            }
        } catch (Exception e) {
            return result.fail(500, e.getMessage());
        }
    }

    /**
     * 新增单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    @Override
    public APIResult<String> createBill(String billData) {
        APIResult<String> result = new APIResult<>();
        log.info("新增单据接口--->接收到的数据：\"{}\"", billData);
        //json数据转化为POJO
        List<BillSyncDTO> billSyncDTOList;
        List<BillSyncDTO> normalList = new ArrayList<>();
        try {
            billSyncDTOList = JSONArray.parseArray(billData, BillSyncDTO.class);
        } catch (Exception e) {
            log.error("新增单据接口--->处理异常;数据格式不正确\"{}\"", e.getMessage());
            return result.fail(500, "数据格式不正确");
        }
        ProjSyncBillContext billContext = new ProjSyncBillContext();
        log.info("新增单据接口--->校验合格数据");
        String errMsg = checkBillData(billSyncDTOList, normalList, billContext);
        try {
            log.info("新增单据接口--->保存数据");
            processBill(normalList, billContext);
        } catch (Exception e) {
            log.error("新增单据接口--->处理异常;\"{}\"", e.getMessage());
            e.getStackTrace();
            return result.fail(500, e.getMessage());
        }
        if (StringUtils.isNotEmpty(errMsg)) {
            return result.fail(4500, errMsg);
        }
        log.info("新增单据接口--->保存数据结束");
        return result.succeed();
    }

    /**
     * 新增退货单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    @Override
    public APIResult<Object> createRecycleBill(String billData) {
        APIResult<Object> result = new APIResult<>();
        log.info("新增退货单据接口--->接收到的数据：\"{}\"", billData);
        //json数据转化为POJO
        List<RecycleBillDTO> billSyncDTOList;
        List<RecycleBillDTO> normalList = new ArrayList<>();
        Map<String, String> errBillCodeMapList = new HashMap<>(16);
        try {
            billSyncDTOList = JSONArray.parseArray(billData, RecycleBillDTO.class);
        } catch (Exception e) {
            log.error("新增退货单据接口--->处理异常;数据格式不正确\"{}\"", e.getMessage());
            return result.fail(500, "数据格式不正确");
        }
        ProjSyncBillContext billContext = new ProjSyncBillContext();
        log.info("新增退货单据接口--->校验合格数据");
        checkRecycleBillData(billSyncDTOList, normalList, billContext, errBillCodeMapList, null);
        try {
            log.info("新增退货单据接口--->保存数据");
            if (errBillCodeMapList.isEmpty()) {
                processRecycleBill(normalList, billContext);
            }
        } catch (Exception e) {
            log.error("新增退货单据接口--->处理异常;\"{}\"", e.getMessage());
            e.getStackTrace();
            return result.fail(500, e.getMessage());
        }
        if (!errBillCodeMapList.isEmpty()) {
            List<Map<String, String>> errList = new ArrayList<>();
            for (Map.Entry<String, String> entry : errBillCodeMapList.entrySet()) {
                Map<String, String> map = new HashMap<>(2);
                map.put("code", entry.getKey());
                map.put("msg", entry.getValue());
                errList.add(map);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            result.fail(4500, "存在异常数据").attachData(errList);
            try {
                log.info("新增退货单据接口--->处理异常;\"{}\"", objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                e.printStackTrace();
                result.fail(403, e.getStackTrace().toString());
            }
            log.info("新增退货单据接口--->保存数据结束");
            return result;
        }
        log.info("新增退货单据接口--->保存数据结束");
        return result.succeed();
    }

    /**
     * 新增门店调拨单单据
     *
     * @param billData 单据
     * @return 新增结果
     */
    @Override
    public APIResult<Object> createStoreAdjustBill(String billData) {
        APIResult<Object> result = new APIResult<>();
        log.info("新增门店调拨单单据接口--->接收到的数据：\"{}\"", billData);
        //json数据转化为POJO
        List<RecycleBillDTO> billSyncDTOList;
        List<RecycleBillDTO> normalList = new ArrayList<>();
        Map<String, String> errBillCodeMapList = new HashMap<>(16);
        try {
            billSyncDTOList = JSON.parseArray(billData, RecycleBillDTO.class);
        } catch (Exception e) {
            log.error("新增门店调拨单单据接口--->处理异常;数据格式不正确\"{}\"", e.getMessage());
            return result.fail(500, "数据格式不正确");
        }
        ProjSyncBillContext billContext = new ProjSyncBillContext();
        log.info("新增门店调拨单单据接口--->校验合格数据");
        checkRecycleBillData(billSyncDTOList, normalList, billContext, errBillCodeMapList, "StoreToStoreOut");
        try {
            log.info("新增门店调拨单单据接口--->保存数据");
            if (errBillCodeMapList.isEmpty()) {
                processRecycleBill(normalList, billContext);
            }
        } catch (Exception e) {
            log.error("新增门店调拨单单据接口--->保存异常;\"{}\"", e.getMessage());
            e.getStackTrace();
            return result.fail(500, e.getMessage());
        }
        if (!errBillCodeMapList.isEmpty()) {
            List<Map<String, String>> errList = new ArrayList<>();
            for (Map.Entry<String, String> entry : errBillCodeMapList.entrySet()) {
                Map<String, String> map = new HashMap<>(2);
                map.put("msg", entry.getValue());
                map.put("code", entry.getKey());
                errList.add(map);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            result.fail(4500, "存在异常数据").attachData(errList);
            try {
                log.info("新增门店调拨单单据接口--->处理异常;\"{}\"", objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.info("新增门店调拨单单据接口--->处理异常;\"{}\"", e);
                result.fail(403, e.getMessage());
            }
            log.info("新增门店调拨单单据接口--->保存数据结束");
            return result;
        }
        log.info("新增门店调拨单单据接口--->保存数据结束");
        return result.succeed();
    }

    /**
     * 取消单据
     *
     * @param billData 单据
     * @return 取消结果
     */
    @Override
    public APIResult<String> cancelBill(String billData) {
        APIResult<String> result = new APIResult<>();
        //json数据转化为POJO
        List<BillSyncDTO> billSyncDTOList;
        try {
            billSyncDTOList = JSONArray.parseArray(billData, BillSyncDTO.class);
        } catch (Exception e) {
            log.error("新增单据接口--->处理异常;数据格式不正确\"{}\"", e.getMessage());
            return result.fail(500, "数据格式不正确");
        }
        Map<String, BillSyncDTO> dtoMap = new HashMap<>(16);
        //根据单号，收发货方过滤出需要删除的数据
        for (BillSyncDTO dto : billSyncDTOList) {
            String key = dto.getBillCode() + ";" + dto.getSrcCode() + ";" + dto.getDestCode();
            if (!dtoMap.containsKey(key)) {
                dtoMap.put(key, dto);
            }
        }
        //本次推送总的错误信息
        StringBuilder errMsg = new StringBuilder();
        List<BillHeader> saveList = new ArrayList<>();
        for (Map.Entry<String, BillSyncDTO> entry : dtoMap.entrySet()) {
            List<BillHeader> headerList = billHeaderRepository.getListByOrgAndCode(entry.getValue().getSrcCode(), entry.getValue().getDestCode(), entry.getValue().getBillCode());
            for (BillHeader header : headerList) {
                //不是待扫描和制单的单据不允许作废
                if (header.getBillStatus() != BillStatus.Created.index && header.getBillStatus() != BillStatus.Confirm.index) {
                    String msg = "当前订单" + header.getCode() + "状态为[" + BillStatus.getEnum(header.getBillStatus()).getName() + "]不允许取消;";
                    if (!errMsg.toString().contains(msg)) {
                        errMsg.append(msg);
                    }
                    continue;
                }
                header.setBillStatus(BillStatus.Abandoned.index);
                header.setEditTime(new Date());
                header.setRemark("取消");
                saveList.add(header);
            }
        }
        billHeaderRepository.save(saveList);
        if (StringUtils.isNotEmpty(errMsg.toString())) {
            return result.fail(4500, errMsg.toString());
        }
        return result.succeed().succeed();
    }

    /**
     * 同步产品信息到美驰中间表
     */
    @Override
    public APIResult<String> syncMaterialToMidTable() {
        APIResult<String> result = new APIResult<>();
        //获取最后同步时间
        SysPara sysPara = getSysPara("materialLastSyncTime", "美驰产品信息最后同步时间");
        String lastSyncTime = "";
        if (StringUtils.isNotEmpty(sysPara.getValue())) {
            lastSyncTime = sysPara.getValue();
        }
        log.info("同步产品信息到美驰---->>获取最后同步时间：[{}]", lastSyncTime);
        List<MaterialDTO> materialList = openRepository.getSyncMaterialList(lastSyncTime);
        log.info("同步产品信息到美驰---->>获取到的记录数：[{}]", materialList.size());
        if (materialList != null && !materialList.isEmpty()) {
            log.info("同步产品信息到美驰---->>插入中间表-开始：{}", JSON.toJSONString(materialList));
            boolean isNormal = insertMidMaterial(materialList);
            log.info("同步产品信息到美驰---->>插入中间表-结束：isNormal={}", isNormal);
            if (isNormal) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                sysPara.setValue(dateFormat.format(materialList.get(0).getEditTime()));
                log.info("同步产品信息到美驰---->>更新最后同步时间：[{}]", sysPara.getValue());
                sysParaRepository.save(sysPara);
            }
        }
        result.succeed().attachData(String.valueOf(materialList.size()));
        return result;
    }

    /**
     * 同步地理城市到美驰中间表
     */
    @Override
    public APIResult<String> syncGeoCityToMidTable() {
        APIResult<String> result = new APIResult<>();
        //获取最后同步时间
        SysPara sysPara = getSysPara("geoCityLastSyncTime", "美驰地理城市最后同步时间");
        String lastSyncTime = "";
        if (StringUtils.isNotEmpty(sysPara.getValue())) {
            lastSyncTime = sysPara.getValue();
        }
        log.info("同步地理城市到美驰---->>获取最后同步时间：[{}]", lastSyncTime);
        List<SysGeoCityDTO> geoCityList = openRepository.getSyncGeoCityList(lastSyncTime);
        log.info("同步地理城市到美驰---->>获取到的记录数：[{}]", geoCityList.size());
        if (geoCityList != null && !geoCityList.isEmpty()) {
            log.info("同步地理城市到美驰---->>插入中间表-开始：{}", JSON.toJSONString(geoCityList));
            boolean isNormal = insertMidGeoCity(geoCityList);
            log.info("同步地理城市到美驰---->>插入中间表-结束：isNormal={}", isNormal);
            if (isNormal) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                sysPara.setValue(dateFormat.format(geoCityList.get(0).getEditTime()));
                log.info("同步地理城市到美驰---->>更新最后同步时间：[{}]", sysPara.getValue());
                sysParaRepository.save(sysPara);
            }
        }
        result.succeed().attachData(String.valueOf(geoCityList.size()));
        return result;
    }

    /**
     * 同步考核城市到美驰中间表
     */
    @Override
    public APIResult<String> syncCheckCityToMidTable() {
        APIResult<String> result = new APIResult<>();
        //获取最后同步时间
        SysPara sysPara = getSysPara("checkCityLastSyncTime", "美驰考核城市最后同步时间");
        String lastSyncTime = "";
        if (StringUtils.isNotEmpty(sysPara.getValue())) {
            lastSyncTime = sysPara.getValue();
        }
        log.info("同步考核城市到美驰---->>获取最后同步时间：[{}]", lastSyncTime);
        List<SysStructureDTO> checkCityList = openRepository.getSyncCheckCityList(lastSyncTime);
        log.info("同步考核城市到美驰---->>获取到的记录数：[{}]", checkCityList.size());
        if (checkCityList != null && !checkCityList.isEmpty()) {
            log.info("同步考核城市到美驰---->>插入中间表-开始：{}", JSON.toJSONString(checkCityList));
            boolean isNormal = insertMidCheckCity(checkCityList);
            log.info("同步考核城市到美驰---->>插入中间表-结束：isNormal={}", isNormal);
            if (isNormal) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                sysPara.setValue(dateFormat.format(checkCityList.get(0).getEditTime()));
                log.info("同步考核城市到美驰---->>更新最后同步时间：[{}]", sysPara.getValue());
                sysParaRepository.save(sysPara);
            }
        }
        result.succeed().attachData(String.valueOf(checkCityList.size()));
        return result;
    }

    private SysPara getSysPara(String paraId, String paraName) {
        SysPara sysPara = sysParaRepository.load(paraId);
        if (sysPara == null) {
            sysPara = new SysPara();
            sysPara.setId(paraId);
            sysPara.setIsEnabled(true);
            sysPara.setName(paraName);
            sysParaRepository.insert(sysPara);
        }
        return sysPara;
    }

    private java.sql.Timestamp utilDateTosqlDate(java.util.Date utilDate) {
        Timestamp sqlDate = null;
        if (utilDate != null) {
            sqlDate = new Timestamp(utilDate.getTime());//uilt date转sql date
        }
        return sqlDate;
    }

    /**
     * @param materialList 产品
     */
    private boolean insertMidMaterial(List<MaterialDTO> materialList) {
        String url = sysProperties.getProperty("thisProject.meiChi.url");
        String user = sysProperties.getProperty("thisProject.meiChi.user");
        String password = sysProperties.getProperty("thisProject.meiChi.password");
        String driverName = sysProperties.getProperty("thisProject.meiChi.driverClass");
        boolean isSetParam = StringUtils.isEmpty(url) || StringUtils.isEmpty(user) || StringUtils.isEmpty(password) || StringUtils.isEmpty(driverName);
        if (isSetParam) {
            throw new RuntimeException("数据库连接未设置！");
        }
        Connection conn = excelUtilAppService.getConnection(url, user, password, driverName);
        if (conn == null) {
            throw new RuntimeException("数据库连接失败！");
        }
        /**
         String sql = "INSERT INTO YS_Product " +
         "(ID,foreignId,shortCode,sku,Brank,shortName,fullName,englishName,exworkPrice,retailPrice,spec,shelfLife,shelfLifeUnit,forwardDays,description," +
         "comQty,pcsQty,boxQty,pcsGtin,collecQty,comGtin,boxGtin,pcsVolume,volumeUnit,pcsWeight,weightUnit,origin,specId,stageId,seriesId,inUse," +
         "onSale,syncTime,packageId,grossWeight,errorRange,noCode,unitId,categoryId,materialType,LastUpdateTime,traceType )" +
         "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,getDate(),?)";
         String brand = "完达山";
         */
        String sql = "INSERT INTO YS_Product (" +
                "Id,AddTime,EditTime,BoxQty,CategoryId,Description,ExworkPrice,FullName,GrossWeight,InUse,MaterialType,OnSale,Origin,PackageId,PcsQty,PcsVolume," +
                "PcsWeight,RetailPrice,ShelfLife,ShelfLifeUnit,ShortCode,ShortName,Sku,Spec,SpecId,StageId,UnitId,VolumeUnit,WeightUnit,SeriesId,TraceType,LastUpdateTime) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // 设置connection.setAutoCommit为OFF来开启事务，再通过connection.commit/connection.rollback来提交/回滚事务。
            conn.setAutoCommit(false);
            int index = 0;
            for (MaterialDTO dto : materialList) {
                ps.setString(1, dto.getId());
                ps.setTimestamp(2, utilDateTosqlDate(dto.getAddTime()));
                ps.setTimestamp(3, utilDateTosqlDate(dto.getEditTime()));
                ps.setInt(4, dto.getBoxQty());
                ps.setString(5, dto.getCategoryId());
                ps.setString(6, dto.getDescription());
                ps.setBigDecimal(7, dto.getExworkPrice());
                ps.setString(8, StringUtils.isEmpty(dto.getFullName()) ? "" : dto.getFullName());
                ps.setBigDecimal(9, dto.getGrossWeight());
                ps.setBoolean(10, dto.getInUse());
                ps.setInt(11, dto.getMaterialType());
                ps.setBoolean(12, dto.getOnSale());
                ps.setString(13, dto.getOrigin());
                ps.setString(14, dto.getPackageId());
                ps.setInt(15, dto.getPcsQty());
                ps.setBigDecimal(16, dto.getPcsVolume());
                ps.setBigDecimal(17, dto.getPcsWeight());
                ps.setBigDecimal(18, dto.getRetailPrice());
                ps.setInt(19, dto.getShelfLife());
                ps.setInt(20, dto.getShelfLifeUnit());
                ps.setString(21, dto.getShortCode());
                ps.setString(22, dto.getShortName());
                ps.setString(23, dto.getSku());
                ps.setString(24, dto.getSpec());
                ps.setString(25, dto.getSpecId());
                ps.setString(26, StringUtils.isEmpty(dto.getStageId()) ? "" : dto.getStageId());
                ps.setString(27, dto.getUnitId());
                ps.setString(28, dto.getVolumeUnit());
                ps.setString(29, dto.getWeightUnit());
                ps.setString(30, StringUtils.isEmpty(dto.getSeriesId()) ? "" : dto.getSeriesId());
                ps.setInt(31, dto.getNewMaterial() != null && dto.getNewMaterial() ? 2 : 1);
                /**
                 ps.setString(1, dto.getId());
                 ps.setString(2, dto.getForeignId());
                 ps.setString(3, dto.getShortCode());
                 ps.setString(4, dto.getSku());
                 ps.setString(5, brand);
                 ps.setString(6, dto.getShortName());
                 ps.setString(7, dto.getFullName());
                 ps.setString(8, dto.getEnglishName());
                 ps.setBigDecimal(9, dto.getExworkPrice());
                 ps.setBigDecimal(10, dto.getRetailPrice());
                 ps.setString(11, dto.getSpec());
                 ps.setInt(12, dto.getShelfLife());
                 ps.setInt(13, dto.getShelfLifeUnit());
                 ps.setInt(14, dto.getForwardDays());
                 ps.setString(15, dto.getDescription());
                 ps.setInt(16, dto.getComQty());
                 ps.setInt(17, dto.getPcsQty());
                 ps.setInt(18, dto.getBoxQty());
                 ps.setString(19, dto.getPcsGtin());
                 ps.setInt(20, dto.getCollecQty());
                 ps.setString(21, dto.getComGtin());
                 ps.setString(22, dto.getBoxGtin());
                 ps.setBigDecimal(23, dto.getPcsVolume());
                 ps.setString(24, dto.getVolumeUnit());
                 ps.setBigDecimal(25, dto.getPcsWeight());
                 ps.setString(26, dto.getWeightUnit());
                 ps.setString(27, dto.getOrigin());
                 ps.setString(28, dto.getSpecId());
                 ps.setString(29, dto.getStageId());
                 ps.setString(30, dto.getSeriesId());
                 ps.setBoolean(31, dto.getInUse());
                 ps.setBoolean(32, dto.getOnSale());
                 ps.setString(33, dto.getSyncTime() == null ? "" : dateFormat.format(dto.getSyncTime()));
                 ps.setString(34, dto.getPackageId());
                 ps.setBigDecimal(35, dto.getGrossWeight());
                 ps.setBigDecimal(36, dto.getErrorRange());
                 ps.setBoolean(37, dto.getNoCode() == null ? false : dto.getNoCode());
                 ps.setString(38, dto.getUnitId());
                 ps.setString(39, dto.getCategoryId());
                 ps.setInt(40, dto.getMaterialType());
                 ps.setInt(41, dto.getNewMaterial() != null && dto.getNewMaterial() ? 2 : 1);
                 */
                ps.addBatch();
                index++;
                if (index > 0 && index % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            // 提交
            conn.commit();
            return true;
        } catch (SQLException e) {
            log.error("同步产品到中间表,数据插入中间表失败：\"{}\"", e.getMessage());
            log.error("同步产品到中间表,数据插入中间表失败：\"{}\"", e);
            // 回滚
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
                log.error("同步产品到中间表,数据插入中间表失败：\"{}\"", ex);
            }
            return false;
        }
    }

    /**
     * @param geoCityList 地理城市
     */
    private boolean insertMidGeoCity(List<SysGeoCityDTO> geoCityList) {
        String url = sysProperties.getProperty("thisProject.meiChi.url");
        String user = sysProperties.getProperty("thisProject.meiChi.user");
        String password = sysProperties.getProperty("thisProject.meiChi.password");
        String driverName = sysProperties.getProperty("thisProject.meiChi.driverClass");
        boolean isSetParam = StringUtils.isEmpty(url) || StringUtils.isEmpty(user) || StringUtils.isEmpty(password) || StringUtils.isEmpty(driverName);
        if (isSetParam) {
            throw new RuntimeException("数据库连接未设置！");
        }
        Connection conn = excelUtilAppService.getConnection(url, user, password, driverName);
        if (conn == null) {
            throw new RuntimeException("数据库连接失败！");
        }
        /**
         String sql = "INSERT INTO YS_OfficialCity " +
         "(ID,name,superId,level,code,LastUpdateTime )" +
         "VALUES (?,?,?,?,?,getDate())";
         */
        String sql = "INSERT INTO YS_OfficialCity (Id,AddTime,EditTime,GeoLevel,Name,Ordinal,ShortName,ParentId,LastUpdateTime) VALUES (?,?,?,?,?,?,?,?,GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 0;
            for (SysGeoCityDTO dto : geoCityList) {
                ps.setString(1, dto.getId());
                ps.setTimestamp(2, utilDateTosqlDate(dto.getAddTime()));
                ps.setTimestamp(3, utilDateTosqlDate(dto.getEditTime()));
                ps.setInt(4, dto.getGeoLevel());
                ps.setString(5, dto.getName());
                ps.setInt(6, dto.getOrdinal());
                ps.setString(7, dto.getName());
                ps.setString(8, StringUtils.isEmpty(dto.getParentId()) ? "" : dto.getParentId());
                /**
                 ps.setString(1, dto.getId());
                 ps.setString(2, dto.getName());
                 ps.setString(3, dto.getParentId());
                 ps.setInt(4, dto.getGeoLevel());
                 ps.setString(5, dto.getId());
                 */
                ps.addBatch();
                index++;
                if (index > 0 && index % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            log.error("同步地理城市到美驰--->>数据插入中间表失败：\"{}\"", e.getMessage());
            log.error("同步地理城市到美驰--->>数据插入中间表失败：\"{}\"", e);
            return false;
        }
    }

    /**
     * @param checkCityList 考核城市
     */
    private boolean insertMidCheckCity(List<SysStructureDTO> checkCityList) {
        String url = sysProperties.getProperty("thisProject.meiChi.url");
        String user = sysProperties.getProperty("thisProject.meiChi.user");
        String password = sysProperties.getProperty("thisProject.meiChi.password");
        String driverName = sysProperties.getProperty("thisProject.meiChi.driverClass");
        boolean isSetParam = StringUtils.isEmpty(url) || StringUtils.isEmpty(user) || StringUtils.isEmpty(password) || StringUtils.isEmpty(driverName);
        if (isSetParam) {
            throw new RuntimeException("数据库连接未设置！");
        }
        Connection conn = excelUtilAppService.getConnection(url, user, password, driverName);
        if (conn == null) {
            throw new RuntimeException("数据库连接失败！");
        }
        /**
         String sql = "INSERT INTO YS_OrganizeCity " +
         "(ID,name,superId,level,LastUpdateTime )" +
         "VALUES (?,?,?,?,getDate())";
         */
        String sql = "INSERT INTO YS_OrganizeCity (Id,AddTime,EditTime,Deleted,Name,Ordinal,AreaId,ParentId,RegionId,LevelNum,LastUpdateTime) VALUES (?,?,?,?,?,?,?,?,?,?,GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 0;
            for (SysStructureDTO dto : checkCityList) {
                ps.setString(1, dto.getId());
                ps.setTimestamp(2, utilDateTosqlDate(dto.getAddTime()));
                ps.setTimestamp(3, utilDateTosqlDate(dto.getEditTime()));
                ps.setBoolean(4, dto.isDeleted());
                ps.setString(5, dto.getName());
                ps.setInt(6, dto.getOrdinal());
                ps.setString(7, dto.getAreaId());
                ps.setString(8, StringUtils.isEmpty(dto.getParentId()) ? "" : dto.getParentId());
                ps.setString(9, dto.getRegionId());
                ps.setInt(10, dto.getLevelNum() == null ? -1 : dto.getLevelNum());
                /**
                 ps.setString(1, dto.getId());
                 ps.setString(2, dto.getName());
                 ps.setString(3, dto.getParentId());
                 ps.setInt(4, dto.getLevelNum());
                 */
                ps.addBatch();
                index++;
                if (index > 0 && index % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            log.error("同步考核城市到美驰--->>数据插入中间表失败：\"{}\"", e.getMessage());
            log.error("同步考核城市到美驰--->>数据插入中间表失败：\"{}\"", e);
            return false;
        }
    }

    /**
     * 检查数据是否合格
     *
     * @param dtoList    单据数据
     * @param normalList 正常的单据数据
     * @return return
     */
    private String checkBillData(List<BillSyncDTO> dtoList, List<BillSyncDTO> normalList, ProjSyncBillContext billContext) {
        //1.收发货方是否存在,收发货方不能一致
        //2.产品是否存在
        //3.单据是否存在
        //4.同单号的唯一信息是否一致
        StringBuilder errMsg = new StringBuilder();
        Map<String, List<BillHeader>> existedBillMap = new HashMap<>(16);
        Map<String, String> stringMap = new HashMap<>(16);
        for (BillSyncDTO dto : dtoList) {
            boolean isNormal = true;
            String detailMsg;
            if (StringUtils.isEmpty(dto.getSrcCode())) {
                isNormal = false;
                detailMsg = "单号(" + dto.getBillCode() + ")发货方未填写;";
                if (!errMsg.toString().contains(detailMsg)) {
                    errMsg.append(detailMsg);
                }
            } else {
                Organization src;
                if (billContext.getOrgMap().containsKey(dto.getSrcCode())) {
                    src = billContext.getOrgMap().get(dto.getSrcCode());
                } else {
                    src = getSrcOrg(dto.getSrcCode());
                    billContext.getOrgMap().put(dto.getSrcCode(), src);
                }
                //1.1发货方校验
                if (src == null) {
                    isNormal = false;
                    detailMsg = "单号(" + dto.getBillCode() + ")发货方不存在(" + dto.getSrcCode() + ");";
                    if (!errMsg.toString().contains(detailMsg)) {
                        errMsg.append(detailMsg);
                    }
                } else {
                    dto.setBillTypeId(src.getOrgType() == OrgType.DEALER.index ? "DealerWantToStoreOut" : "D2WantToStoreOut");
                }
            }
            if (StringUtils.isEmpty(dto.getDestCode())) {
                isNormal = false;
                detailMsg = "单号(" + dto.getBillCode() + ")收货方未填写;";
                if (!errMsg.toString().contains(detailMsg)) {
                    errMsg.append(detailMsg);
                }
            } else {
                //1.2发货方校验
                Organization dest;
                if (billContext.getOrgMap().containsKey(dto.getDestCode() + ";" + OrgType.STORE.index)) {
                    dest = billContext.getOrgMap().get(dto.getDestCode() + ";" + OrgType.STORE.index);
                } else {
                    dest = organizationRepository.getByCode(dto.getDestCode(), OrgType.STORE.index);
                    billContext.getOrgMap().put(dto.getDestCode() + ";" + OrgType.STORE.index, dest);
                }
                if (dest == null) {
                    isNormal = false;
                    detailMsg = "单号(" + dto.getBillCode() + ")收货方不存在(" + dto.getDestCode() + ");";
                    if (!errMsg.toString().contains(detailMsg)) {
                        errMsg.append(detailMsg);
                    }
                }
            }
            if (StringUtils.isNotEmpty(dto.getSrcCode()) && StringUtils.isNotEmpty(dto.getDestCode()) && dto.getDestCode().equals(dto.getSrcCode())) {
                isNormal = false;
                detailMsg = "单号(" + dto.getBillCode() + ")收发货方不能一致;";
                if (!errMsg.toString().contains(detailMsg)) {
                    errMsg.append(detailMsg);
                }
            }
            Material material;
            if (billContext.getMaterialMap().containsKey(dto.getSku())) {
                material = billContext.getMaterialMap().get(dto.getSku());
            } else {
                material = materialRepository.getBySKU(dto.getSku());
                billContext.getMaterialMap().put(dto.getSku(), material);
            }
            //2.产品是否存在
            if (material == null) {
                isNormal = false;
                detailMsg = "单号(" + dto.getBillCode() + ")产品不存在(" + dto.getSku() + ");";
                if (!errMsg.toString().contains(detailMsg)) {
                    errMsg.append(detailMsg);
                }
            }
            //4.唯一校验(一个单号只能对应一个收货方、发货方和订单类型)
            if (stringMap.containsKey(dto.getBillCode())) {
                if (!stringMap.get(dto.getBillCode()).equals(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getDataType())) {
                    isNormal = false;
                    detailMsg = "单号(" + dto.getBillCode() + ")明细收发货方、订单类型不一致;";
                    if (!errMsg.toString().contains(detailMsg)) {
                        errMsg.append(detailMsg);
                    }
                }
            } else {
                stringMap.put(dto.getBillCode(), dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getDataType());
            }
            //3.单据是否存在
            List<BillHeader> headerList;
            if (existedBillMap.containsKey(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getBillCode())) {
                headerList = existedBillMap.get(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getBillCode());
            } else {
                headerList = billHeaderRepository.getListByOrgAndCode(dto.getSrcCode(), dto.getDestCode(), dto.getBillCode());
                existedBillMap.put(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getBillCode(), headerList);
            }
            if (headerList != null && !headerList.isEmpty()) {
                isNormal = false;
                detailMsg = "单号(" + dto.getBillCode() + ")已存在;";
                if (!errMsg.toString().contains(detailMsg)) {
                    errMsg.append(detailMsg);
                }
            }
            try {
                dto.setAddTime(format.parse(dto.getOperateTime()));
            } catch (ParseException e) {
                isNormal = false;
                detailMsg = "单号(" + dto.getBillCode() + ")订单日期不正确(" + dto.getOperateTime() + ");";
                if (!errMsg.toString().contains(detailMsg)) {
                    errMsg.append(detailMsg);
                }
            }
            if (isNormal) {
                normalList.add(dto);
            }
        }
        return errMsg.toString();
    }

    /**
     * 检查数据是否合格
     *
     * @param dtoList    单据数据
     * @param normalList 正常的单据数据
     * @param errMap     异常信息
     */
    private void checkRecycleBillData(List<RecycleBillDTO> dtoList, List<RecycleBillDTO> normalList,
                                      ProjSyncBillContext billContext, Map<String, String> errMap, String billTypeId) {
        //1.收发货方是否存在,收发货方不能一致
        //2.产品是否存在
        //3.单据是否存在
        //4.同单号的唯一信息是否一致
        //已存在的单据
        Map<String, List<BillHeader>> existedBillMap = new HashMap<>(16);
        //4.唯一校验(一个单号只能对应一个收货方、发货方和订单类型)
        Map<String, String> stringMap = new HashMap<>(16);
        //每个单据的扫码信息
        Map<String, Map<String, String>> billScanCodeMap = new HashMap<>(16);
        Map<String, List<RecycleBillDTO>> map = dtoList.stream().collect(Collectors.groupingBy(x -> x.getScanCode()));
        for (RecycleBillDTO dto : dtoList) {
            List<RecycleBillDTO> codeList = map.get(dto.getScanCode());
            if (!codeList.isEmpty() && codeList.size() > 1) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "扫码重复");
                }
                return;
            }
            Organization src;
            if (StringUtils.isEmpty(dto.getScanCode())) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "扫码信息未填写");
                }
                return;
            }
            if (StringUtils.isEmpty(dto.getBillCode())) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "单号未填写");
                }
                return;
            }
            if (StringUtils.isEmpty(dto.getSrcCode())) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "发货方未填写");
                }
                return;
            }
            if (billContext.getStoreMap().containsKey(dto.getSrcCode())) {
                src = billContext.getStoreMap().get(dto.getSrcCode());
            } else {
                src = getOrgByOrgType(dto.getSrcCode(), OrgType.STORE.index);
                billContext.getStoreMap().put(dto.getSrcCode(), src);
            }
            //1.1发货方校验
            if (src == null) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "发货方不存在(" + dto.getSrcCode() + ")");
                }
                return;
            }
            if (StringUtils.isEmpty(dto.getDestCode())) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "收货方未填写");
                }
                return;
            }
            //1.2收货方校验
            Organization dest;
            //门店调拨单使用这个字段
            if (StringUtils.isNotEmpty(billTypeId)) {
                if (billContext.getStoreMap().containsKey(dto.getDestCode())) {
                    dest = billContext.getStoreMap().get(dto.getDestCode());
                } else {
                    dest = getOrgByOrgType(dto.getDestCode(), OrgType.STORE.index);
                    if (dest != null) {
                        billContext.getStoreMap().put(dto.getDestCode(), dest);
                    }
                }
            } else {
                if (billContext.getDealerMap().containsKey(dto.getDestCode())) {
                    dest = billContext.getDealerMap().get(dto.getDestCode());
                } else if (billContext.getD2Map().containsKey(dto.getDestCode())) {
                    dest = billContext.getD2Map().get(dto.getDestCode());
                } else {
                    dest = getOrgByOrgType(dto.getDestCode(), OrgType.DEALER.index);
                    if (dest == null) {
                        dest = getOrgByOrgType(dto.getDestCode(), OrgType.DISTRIBUTOR.index);
                        billContext.getD2Map().put(dto.getDestCode(), dest);
                    } else {
                        billContext.getDealerMap().put(dto.getDestCode(), dest);
                    }
                }
            }
            if (dest == null) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "收货方不存在(" + dto.getDestCode() + ")");
                }
                return;
            }
            //门店调拨单使用这个字段
            if (StringUtils.isNotEmpty(billTypeId)) {
                dto.setBillTypeId(billTypeId);
            } else {
                dto.setBillTypeId(dest.getOrgType() == OrgType.DEALER.index ? "StoreToDealerOutMeiChi" : "StoreToD2OutMeiChi");
            }
            if (StringUtils.isNotEmpty(dto.getSrcCode()) && StringUtils.isNotEmpty(dto.getDestCode()) && dto.getDestCode().equals(dto.getSrcCode())) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "收发货方不能相同");
                }
                return;
            }
            if (StringUtils.isEmpty(dto.getSku())) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "产品未填写");
                }
                return;
            }
            Material material;
            if (billContext.getMaterialMap().containsKey(dto.getSku())) {
                material = billContext.getMaterialMap().get(dto.getSku());
            } else {
                material = materialRepository.getBySKU(dto.getSku());
                billContext.getMaterialMap().put(dto.getSku(), material);
            }
            //2.产品是否存在
            if (material == null) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), "产品不存在(" + dto.getSku() + ")");
                }
                return;
            }
            //扫码失败校验
            String msg = checkScanCodeForBill(dto, billScanCodeMap, material);
            if (StringUtils.isNotEmpty(msg)) {
                if (!errMap.containsKey(dto.getScanCode())) {
                    errMap.put(dto.getScanCode(), msg);
                }
                return;
            }
            //4.唯一校验(一个单号只能对应一个收货方、发货方和订单类型)
            if (stringMap.containsKey(dto.getBillCode())) {
                if (!stringMap.get(dto.getBillCode()).equals(dto.getSrcCode() + ";" + dto.getDestCode())) {
                    if (!errMap.containsKey(dto.getBillCode())) {
                        errMap.put(dto.getBillCode(), "单据对应的发货方不一致");
                    }
                    return;
                }
            } else {
                stringMap.put(dto.getBillCode(), dto.getSrcCode() + ";" + dto.getDestCode());
            }
            //3.单据是否存在
            List<BillHeader> headerList;
            if (existedBillMap.containsKey(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getBillCode())) {
                headerList = existedBillMap.get(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getBillCode());
            } else {
                headerList = billHeaderRepository.getListByOrgAndCode(dto.getSrcCode(), dto.getDestCode(), dto.getBillCode());
                existedBillMap.put(dto.getSrcCode() + ";" + dto.getDestCode() + ";" + dto.getBillCode(), headerList);
            }
            if (headerList != null && !headerList.isEmpty()) {
                if (!errMap.containsKey(dto.getBillCode())) {
                    errMap.put(dto.getBillCode(), "单号已存在");
                }
                return;
            }
            if (StringUtils.isEmpty(dto.getOperateTime())) {
                if (!errMap.containsKey(dto.getBillCode())) {
                    errMap.put(dto.getBillCode(), "订单日期未填写");
                }
                return;
            }
            try {
                dto.setAddTime(format.parse(dto.getOperateTime()));
            } catch (ParseException e) {
                if (!errMap.containsKey(dto.getBillCode())) {
                    errMap.put(dto.getBillCode(), "订单日期不正确(" + dto.getOperateTime() + ")");
                }
                return;
            }
            normalList.add(dto);
        }
    }

    /**
     * 校验整单失败
     *
     * @param dto             扫码信息
     * @param billScanCodeMap 单据对应的扫码信息
     * @return 异常信息
     */
    private String checkScanCodeForBill(RecycleBillDTO dto, Map<String, Map<String, String>> billScanCodeMap, Material material) {
        //根据单号获取到本单的所有扫码
        Map<String, String> scanCodeMap = billScanCodeMap.get(dto.getBillCode());
        if (scanCodeMap == null) {
            scanCodeMap = new HashMap<>(16);
        }
        //扫码是否重复
        if (scanCodeMap.containsKey(dto.getScanCode())) {
            return "扫码重复";
        }
        //扫码是否存在
        ProductionCode code = productionCodeRepository.load(dto.getScanCode());
        if (code == null) {
            return "扫码不存在";
        }
        //不是单品码时，箱码下的单品码是否在本单中被扫描
        if (!code.isMinSaleUnit()) {
            List<ProductionCode> childList = productionCodeRepository.getSingleCodes(dto.getScanCode());
            for (ProductionCode childCode : childList) {
                if (scanCodeMap.containsKey(childCode.getId())) {
                    return "箱码与箱内单品码重复";
                }
                scanCodeMap.put(childCode.getId(), dto.getBillCode());
            }
        }
        if (!code.getMaterialId().equals(material.getId())) {
            return "扫码不属于该SKU";
        }
        scanCodeMap.put(dto.getScanCode(), dto.getBillCode());
        billScanCodeMap.put(dto.getBillCode(), scanCodeMap);
        return null;
    }

    private Organization getSrcOrg(String orgCode) {
        Organization src = organizationRepository.getByCode(orgCode, OrgType.DEALER.index);
        if (src == null) {
            src = organizationRepository.getByCode(orgCode, OrgType.DISTRIBUTOR.index);
        }
        return src;
    }

    private Organization getOrgByOrgType(String orgCode, int orgType) {
        return organizationRepository.getByCode(orgCode, orgType);
    }

    /**
     * 处理单据
     *
     * @param normalList  接口数据
     * @param billContext 数据上下文
     */
    private void processBill(List<BillSyncDTO> normalList, ProjSyncBillContext billContext) {
        Map<String, List<BillSyncDTO>> dtoMap = normalList.stream().collect(Collectors.groupingBy(x -> x.getBillCode()));
        List<BillHeader> insertList = new ArrayList<>();
        //获取物料计量单位
        String minStr = "个";
        String stdStr = "箱";
        SysPara minUnit = sysParaRepository.load("min_unit_name");
        SysPara stdUnit = sysParaRepository.load("std_unit_name");
        if (minUnit != null && stdUnit != null) {
            minStr = minUnit.getValue();
            stdStr = stdUnit.getValue();
        }
        for (Map.Entry<String, List<BillSyncDTO>> entry : dtoMap.entrySet()) {
            List<BillSyncDTO> dtoList = entry.getValue();
            BillSyncDTO dto = dtoList.get(0);
            BillHeader billHeader = new BillHeader();
            billHeader.setId(UUID.randomUUID().toString());
            billHeader.setAddBy(SYNC_SAP);
            billHeader.setAddTime(new Date());
            billHeader.setEditBy(SYNC_SAP);
            billHeader.setEditTime(new Date());
            billHeader.setAddFrom("SYNC");
            billHeader.setBillStatus(BillStatus.Confirm.index);
            billHeader.setInOutId(dto.getDataType());
            billHeader.setBillTypeId(dto.getBillTypeId());
            billHeader.setCode(dto.getBillCode());
            billHeader.setRefCode(dto.getBillCode());
            Organization destOrg = billContext.getOrgMap().get(dto.getDestCode() + ";" + OrgType.STORE.index);
            Organization srcOrg = billContext.getOrgMap().get(dto.getSrcCode());
            billHeader.setDestId(destOrg.getId());
            billHeader.setSrcId(srcOrg.getId());
            billHeader.setOperateTime(dto.getAddTime());
            billHeader.setReceiveStatus("N");
            //创建单据明细
            List<BillDetail> detailList = buildBillDetail(dtoList, billHeader, billContext, minStr, stdStr);
            billHeader.setDetailList(detailList);
            billHeader.setExpectQtyPcs(billHeader.getDetailList().stream().mapToInt(x -> x.getExpectQtyPcs()).sum());
            insertList.add(billHeader);
        }
        billHeaderRepository.insert(insertList);
    }

    private List<BillDetail> buildBillDetail(List<BillSyncDTO> dtoList, BillHeader billHeader, ProjSyncBillContext billContext, String minStr, String stdStr) {
        //箱数计数
        int stdCount = 0;
        //个数计数
        int minCount = 0;
        //创建单据明细
        List<BillDetail> detailList = new ArrayList<>();
        Map<String, String> existDetailMap = new HashMap<>(dtoList.size());
        //应发箱数和单品数据，在统计完明细之后计算，数据先存放备用字段中
        for (BillSyncDTO dto : dtoList) {
            if (existDetailMap.containsKey(dto.getBatchCode() + "," + dto.getSku())) {
                BillDetail existDetail = detailList.stream().filter(x -> (x.getAttr1().equals(dto.getBatchCode()) || StringUtil.isEmpty(x.getAttr1())) && x.getMaterial().getSku().equals(dto.getSku())).findFirst().orElse(null);
                if (existDetail != null) {
                    existDetail.setRemark((Integer.parseInt(existDetail.getRemark()) + dto.getExpectQtyPcs()) + "");
                }
            } else {
                BillDetail billDetail = new BillDetail();
                billDetail.setId(UUID.randomUUID().toString());
                //批号到Attr1
                billDetail.setAttr1(dto.getBatchCode());
                Material material = billContext.getMaterialMap().get(dto.getSku());
                billDetail.setMaterial(material);
                billDetail.setRemark(dto.getExpectQtyPcs() + "");
                existDetailMap.put(dto.getBatchCode() + "," + dto.getSku(), null);
                billDetail.setBillHeader(billHeader);
                detailList.add(billDetail);
            }
        }
        BigDecimal expectQty = BigDecimal.ZERO;
        for (BillDetail billDetail : detailList) {
            billDetail.setExpectQtyPcs(StringUtils.isEmpty(billDetail.getRemark()) ? 0 : Integer.parseInt(billDetail.getRemark()));
            Material material = billDetail.getMaterial();
            int stdQty = billDetail.getExpectQtyPcs() / material.getPcsQty();
            int minQty = billDetail.getExpectQtyPcs() % material.getPcsQty();
            stdCount = stdCount + stdQty;
            minCount = minCount + minQty;
            billDetail.setExpectQtyStr(excelUtilAppService.concatQtyStr(stdQty, minQty, stdStr, minStr));
            billDetail.setExpectQtyNew(BigDecimal.valueOf(billDetail.getExpectQtyPcs()).divide(BigDecimal.valueOf(material.getPcsQty()), 2, BigDecimal.ROUND_HALF_UP));
            expectQty = expectQty.add(billDetail.getExpectQtyNew());
        }
        billHeader.setExpectQtyStr(excelUtilAppService.concatQtyStr(stdCount, minCount, stdStr, minStr));
        billHeader.setExpectQty(expectQty);
        return detailList;
    }

    /**
     * 处理单据
     *
     * @param normalList  接口数据
     * @param billContext 数据上下文
     */
    private void processRecycleBill(List<RecycleBillDTO> normalList, ProjSyncBillContext billContext) {
        Map<String, List<RecycleBillDTO>> dtoMap = normalList.stream().collect(Collectors.groupingBy(x -> x.getBillCode()));
        List<BillHeader> insertList = new ArrayList<>();
        //获取物料计量单位
        List<ScanData> scanDataList = new ArrayList<>();
        List<BillScanInfo> billScanInfoList = new ArrayList<>();
        for (Map.Entry<String, List<RecycleBillDTO>> entry : dtoMap.entrySet()) {
            List<RecycleBillDTO> dtoList = entry.getValue();
            RecycleBillDTO dto = dtoList.get(0);
            BillHeader billHeader = new BillHeader();
            billHeader.setId(UUID.randomUUID().toString());
            billHeader.setAddBy(SYNC_SAP);
            billHeader.setAddTime(new Date());
            billHeader.setEditBy(SYNC_SAP);
            billHeader.setEditTime(new Date());
            billHeader.setAddFrom("SYNC");
            billHeader.setBillStatus(BillStatus.Confirm.index);
            billHeader.setBillTypeId(dto.getBillTypeId());
            billHeader.setCode(dto.getBillCode());
            billHeader.setRefCode(dto.getBillCode());
            Organization destOrg = null;
            switch (dto.getBillTypeId()) {
                case "StoreToDealerOutMeiChi":
                    destOrg = billContext.getDealerMap().get(dto.getDestCode());
                    break;
                case "StoreToD2OutMeiChi":
                    destOrg= billContext.getD2Map().get(dto.getDestCode());
                    break;
                case "StoreToStoreOut":
                    destOrg= billContext.getStoreMap().get(dto.getDestCode());
                    break;
                default:
            }
            Organization srcOrg = billContext.getStoreMap().get(dto.getSrcCode());
            billHeader.setDestId(destOrg == null ? dto.getDestCode() : destOrg.getId());
            billHeader.setSrcId(srcOrg.getId());
            billHeader.setOperateTime(dto.getAddTime());
            billHeader.setReceiveStatus("N");
            billHeader.setProcessType(ComUtils.PROMPTLY);
            //创建单据明细
            billHeader.setExpectQtyPcs(0);
            billHeader.setExpectQtyStr("0");
            billHeader.setExpectQty(BigDecimal.ZERO);
            billHeader.setBillStatus(BillStatus.Submitted.index);
            billHeader.setBillScanInfoId(UUID.randomUUID().toString());
            createScanCode(dtoList, billHeader, scanDataList);
            insertList.add(billHeader);
            billScanInfoList.add(createRecycleBillScanInfo(billHeader));
        }
        scanDataRepository.insert(scanDataList);
        billHeaderRepository.insert(insertList);
        billScanInfoRepository.insert(billScanInfoList);
        for (BillHeader billHeader : insertList) {
            billService.processBillData(billHeader.getId());
        }
    }

    private void createScanCode(List<RecycleBillDTO> dtoList, BillHeader billHeader, List<ScanData> scanDataList) {
        for (RecycleBillDTO dto : dtoList) {
            ScanData scanData = new ScanData();
            scanData.setId(UUID.randomUUID().toString());
            scanData.setHeaderId(billHeader.getId());
            scanData.setScanCode(dto.getScanCode());
            scanData.setScanTime(dto.getAddTime());
            scanData.setScanCode(dto.getScanCode());
            scanDataList.add(scanData);
        }
    }

    private BillScanInfo createRecycleBillScanInfo(BillHeader billHeader) {
        //region 生成单据扫码信息记录BillScanInfo
        BillScanInfo billScanInfo = new BillScanInfo();
        billScanInfo.setId(billHeader.getBillScanInfoId());
        billScanInfo.setBillHeaderId(billHeader.getId());
        billScanInfo.setBillCode(billHeader.getCode());
        billScanInfo.setBillFromServer(true);
        billScanInfo.setBillType(billHeader.getBillTypeId());
        billScanInfo.setNoBill(false);
        billScanInfo.setProcessStatus(BillScanInfoStatus.SCANNING.index);
        billScanInfo.setScanCodeTotalQty(0);
        billScanInfo.setAddBy("MeiChi");
        billScanInfo.setAddTime(new Date());
        billScanInfo.setEditBy("MeiChi");
        billScanInfo.setEditTime(new Date());
        billScanInfo.setOrgId(billHeader.getSrcId());
        return billScanInfo;
        //endregion
    }
}
