package com.proj.appservice.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebc.appservice.ProductionCodeAppService;
import com.arlen.ebc.entity.CodeData;
import com.arlen.ebc.entity.ProductQcRecord;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.CodeDataRepository;
import com.arlen.ebc.repository.ProductQcRecordRepository;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.entity.MaterialImage;
import com.arlen.ebd.entity.SimpleType;
import com.arlen.ebd.repository.MaterialImageRepository;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebd.repository.SimpleTypeRepository;
import com.arlen.ebp.entity.CompanyForFactory;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.CompanyForFactoryRepository;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.emes.entity.WorkPlan;
import com.arlen.emes.repository.WorkPlanRepository;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.appservice.H5CodeQueryAppService;
import com.proj.appservice.ProjMaterialAppService;
import com.proj.dto.H5CodeQueryDTO;
import com.arlen.utils.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by arlenChen on 2019/9/24.
 * h5追溯信息
 *
 * @author arlenChen
 */
@Service
public class H5CodeQueryAppServiceImpl implements H5CodeQueryAppService {
    private static Logger log = LoggerFactory.getLogger(H5CodeQueryAppServiceImpl.class);
    @Resource
    private ProductionCodeAppService appService;

    @Resource
    private MaterialRepository materialRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private CompanyForFactoryRepository companyForFactoryRepository;
    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private ProductQcRecordRepository qcRecordRepository;
    @Resource
    private SimpleTypeRepository simpleTypeRepository;
    private static final String SEPARATOR = "/";
    @Resource
    private MaterialImageRepository materialImageRepository;
    private static final String DEFAULT_IMG_PATH = "/material_Img/";
    @Resource
    private ProjMaterialAppService projMaterialAppService;
    @Resource
    private CodeDataRepository codeDataRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private WorkPlanRepository workPlanRepository;

    /**
     * 追溯码查询数据
     *
     * @param code 追溯码
     * @return 数据
     */
    @Override
    public APIResult<H5CodeQueryDTO> queryByCode(String code) {
        APIResult<H5CodeQueryDTO> result = new APIResult<>();
        ProductionCode productionCode = appService.findProductionCode(code);
        if (productionCode == null) {
            return result.fail(404, "数据不存在");
        }
        H5CodeQueryDTO dto = new H5CodeQueryDTO();
        dto.setCode(code);
        SysPara sysPara = sysParaRepository.load("webUrl");
        String webUrlStr = "";
        if (sysPara != null && StringUtils.isNotEmpty(sysPara.getValue())) {
            webUrlStr = sysPara.getValue();
        }
        Material material = materialRepository.load(productionCode.getMaterialId());
        if (material != null) {
            //产品信息中存储的数据
            queryMaterialData(dto, material, webUrlStr);
        }
        //追溯码相关数据
        queryProductionCodeData(dto, productionCode, webUrlStr);
        //工厂号
        String factoryCode = getFactoryCode(productionCode.getFactoryId(), productionCode.getBatchCode());
        //工厂筛选
        Organization factory = organizationRepository.load(factoryCode);
        if (factory == null || factory.getOrgType() != OrgType.FACTORY.index) {
            factory = organizationRepository.getByCode(factoryCode, OrgType.FACTORY.index);
        }
        if (factory != null) {
            //工厂存储的数据
            queryFactoryData(dto, factory, webUrlStr);
        }
        //系统参数存储的数据
        queryParaData(dto);
        return result.succeed().attachData(dto);
    }


    /**
     * 生产数据统计报表 id
     *
     * @param id id
     * @return 数据
     */
    @Override
    public APIResult<String> syncCodeData(String id) {
        APIResult<String> result = new APIResult<>();
        CodeData codeData = codeDataRepository.load(id);
        if (codeData == null) {
            log.info("生产数据统计报表-->数据不存在");
            return result.fail(404, "数据不存在");
        }
        Material material = materialRepository.load(codeData.getMaterialId());
        if (material == null) {
            log.info("生产数据统计报表-->产品不存在");
            return result.fail(404, "产品不存在");
        }
        SysPara sysPara = sysParaRepository.load("syncCodeDataQtyUrl");
        if (sysPara == null || StringUtils.isEmpty(sysPara.getValue())) {
            log.info("生产数据统计报表-->接口未配置");
            return result.fail(404, "接口未配置");
        }
        StringBuilder url = new StringBuilder(sysPara.getValue());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        url.append("?batchCode=").append(codeData.getBatchCode()).append("&sku=").append(material.getSku()).append("&packDate=").append(format.format(codeData.getPackDate()));
        try {
            log.info("生产数据统计报表-->查询数据:\"{}\"", url.toString());
            String responseData = excelUtilAppService.httpGet(url.toString());
            log.info("生产数据统计报表-->数据:\"{}\"", responseData);
            processData(responseData, codeData, material);
            log.info("生产数据统计报表-->处理结束");
        } catch (Exception e) {
            log.info("生产数据统计报表-->异常:\"{}\"", e.getMessage());
            e.getStackTrace();
            return result.fail(500, e.getMessage());
        }
        return result.succeed();
    }

    /**
     * 确认
     *
     * @param id        d
     * @param remark    原因
     * @param confirmBy 确认
     * @return return
     */
    @Override
    public APIResult<String> confirm(String id, String remark, String confirmBy) {
        APIResult<String> result = new APIResult<>();
        CodeData codeData = codeDataRepository.load(id);
        if (codeData == null) {
            return result.fail(404, "数据不存在");
        }
        codeData.setConfirmStatus(2);
        codeData.setConfirmBy(confirmBy);
        codeData.setConfirmTime(new Date());
        codeData.setRemark(remark);
        codeDataRepository.save(codeData);
        return result.succeed();
    }

    private void processData(String responseData, CodeData codeData, Material material) {
        JSONObject parseObject = JSONObject.parseObject(responseData);
        JSONArray jsonList = parseObject.getJSONArray("data");
        List<WorkPlan> planList = new ArrayList<>();
        for (Object jsonStr : jsonList) {
            JSONObject lipObject = (JSONObject) jsonStr;
            String id = lipObject.getString("id");
            String code = lipObject.getString("code");
            int expectQtyPcs = lipObject.getInteger("expectQtyPcs");
            if (StringUtils.isEmpty(id)) {
                continue;
            }
            WorkPlan workPlan;
            workPlan = workPlanRepository.getByOrderCode(id);
            if (workPlan == null) {
                workPlan = new WorkPlan();
                workPlan.setOrderCode(id);
                workPlan.setCode(code);
                workPlan.setPlanQty(expectQtyPcs);
                workPlan.setAttr1(codeData.getId());
                workPlan.setMaterialId(material.getId());
                workPlan.setPackDate(codeData.getPackDate());
                workPlan.setBatchCode(codeData.getBatchCode());
                planList.add(workPlan);
            }
        }
        if (!planList.isEmpty()) {
            int expectQtyPcs = planList.stream().mapToInt(x -> x.getPlanQty()).sum();
            BigDecimal boxQty = new BigDecimal(expectQtyPcs).divide(new BigDecimal(material.getPcsQty()), 2, BigDecimal.ROUND_HALF_EVEN);
            codeData.setQtyPcs((codeData.getQtyPcs() == null ? 0 : codeData.getQtyPcs()) + expectQtyPcs);
            codeData.setBoxQty(codeData.getBoxQty() == null ? boxQty : codeData.getBoxQty().add(boxQty));
            codeDataRepository.save(codeData);
            workPlanRepository.save(planList);
        }
    }

    /**
     * 产品信息中存储的数据
     *
     * @param dto      数据
     * @param material 产品信息
     */
    private void queryMaterialData(H5CodeQueryDTO dto, Material material, String webUrlStr) {
        dto.setSku(material.getSku());
        //产品名称
        dto.setMaterialShortName(material.getShortName());
        //产品名称
        dto.setMaterialFullName(material.getFullName());
        SimpleType simpleType = simpleTypeRepository.load(material.getSpecId());
        if (simpleType != null) {
            //产品规格
            dto.setMaterialSpec(simpleType.getName());
        }
        //产品标准代号
        dto.setStandardCode(material.getAttr3());
        //产品的类别、属性
        dto.setProperty(material.getAttr2());
        //生产工艺
        dto.setTechnology(material.getAttr6());
        //对原料要求的其他说明
        dto.setExtraExplain(material.getExt9());
        //适用年龄
        dto.setAge(material.getExt1());
        //使用说明
        dto.setUseExplain(material.getExt5());
        //警示说明
        dto.setNotice(material.getExt3());
        //贮存条件
        dto.setCondition(material.getExt7());
        //其它质量承诺
        dto.setPromise(material.getExt10());
        //配料表
        dto.setBurden(material.getExt6());
        //主要营养成份
        dto.setNutrition(material.getExt8());
        List<MaterialImage> materialImageList = materialImageRepository.getByMaterial(material.getId());
        List<Map<String, String>> fileList = new ArrayList<>();
        String fileBaseDir = projMaterialAppService.getUploadDir().replace("/", SEPARATOR).replace("\\", SEPARATOR);
        for (MaterialImage image : materialImageList) {
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(image.getImgPath())) {
                String webUrl = image.getImgPath().replace(fileBaseDir, webUrlStr + DEFAULT_IMG_PATH);
                webUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
                Map<String, String> map = new HashMap<>(1);
                fileList.add(map);
                map.put(image.getAttr1(), webUrl);
            }
        }
        dto.setFileList(fileList);
    }

    /**
     * 追溯码相关数据
     *
     * @param dto            数据
     * @param productionCode 追溯码
     */
    private void queryProductionCodeData(H5CodeQueryDTO dto, ProductionCode productionCode, String webUrlStr) {
        //生产日期
        dto.setPackDate(productionCode.getPackDate());
        //有效期
        dto.setValidDate(productionCode.getValidDate());
        //产品批次
        dto.setBatchCode(productionCode.getBatchCode());
        List<ProductQcRecord> qcList = qcRecordRepository.getList(productionCode.getBatchCode(), productionCode.getMaterialId());
        if (qcList != null && !qcList.isEmpty()) {
            ProductQcRecord qcRecord = qcList.get(0);
            // 主要原料来源地
            dto.setRawMaterialSource(qcRecord.getAttr1());
            //主要原料合格证明
            dto.setCertificate(getCertificate(qcRecord, webUrlStr));
            //产品检验报告
            dto.setReport(getReport(qcRecord, webUrlStr));
            //产品标准代码
            dto.setProductStandard(qcRecord.getProductStandard());
            //产品标准PDF
            dto.setProductStandardPdf(getProductStandardPdf(qcRecord, webUrlStr));
        }
    }

    /**
     * 工厂存储的数据
     *
     * @param dto     数据
     * @param factory 工厂
     */
    private void queryFactoryData(H5CodeQueryDTO dto, Organization factory, String webUrlStr) {
        //联系方式
        String producerContact = "";
        SysPara sysPara = sysParaRepository.load("ProducerContact");
        if (sysPara != null && StringUtils.isNotEmpty(sysPara.getValue())) {
            producerContact = sysPara.getValue();
        }
        dto.setProducerContact(producerContact);
        //诚信评价证书地址
        String integrityUrl = "";
        if (StringUtils.isNotEmpty(factory.getAttr4())) {
            String webUrl = webUrlStr + "/" + factory.getAttr4();
            integrityUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
        }
        dto.setIntegrityUrl(integrityUrl);
        CompanyForFactory companyForFactory = companyForFactoryRepository.getByFactoryId(factory.getId());
        if (companyForFactory != null) {
            //食品生产地
            dto.setMadeIn(companyForFactory.getAttr1());
            //生产者名称和联系方式
            dto.setProducerName(companyForFactory.getName());
            //生产许可证编号
            dto.setProduceCode(companyForFactory.getProductionLicenseNumber());
            //生产许可证有效期
            String produceCodeDate = "";
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            if (companyForFactory.getProductionLicenseBeginDate() != null) {
                produceCodeDate = "自" + format.format(companyForFactory.getProductionLicenseBeginDate()) + "起，";
            }
            if (companyForFactory.getProductionLicenseEndDate() != null) {
                produceCodeDate += "至" + format.format(companyForFactory.getProductionLicenseEndDate()) + "止";
            }
            //生产许可证有效期
            dto.setProduceCodeDate(produceCodeDate);
            //诚信评价证书号
            dto.setIntegrity(companyForFactory.getCreditCertificateNumber());
            String integrityDate = "";
            //诚信评价证书有效期
            if (companyForFactory.getCreditCertificateBeginDate() != null) {
                integrityDate = "自" + format.format(companyForFactory.getCreditCertificateBeginDate()) + "起，";
            }
            if (companyForFactory.getCreditCertificateEndDate() != null) {
                integrityDate += "至" + format.format(companyForFactory.getCreditCertificateEndDate()) + "止";
            }
            //诚信评价证书有效期
            dto.setIntegrityDate(integrityDate);
        }
    }


    /**
     * 系统参数存储的数据
     *
     * @param dto 数据
     */
    private void queryParaData(H5CodeQueryDTO dto) {
        //企业名称
        String producerName = "";
        SysPara producerNamePara = sysParaRepository.load("CompanyName");
        if (producerNamePara != null && StringUtils.isNotEmpty(producerNamePara.getValue())) {
            producerName = producerNamePara.getValue();
        }
        //企业名称
        dto.setCompanyName(producerName);
        //企业地址
        String producerAddress = "";
        SysPara producerAddressPara = sysParaRepository.load("ProducerAddress");
        if (producerAddressPara != null && StringUtils.isNotEmpty(producerAddressPara.getValue())) {
            producerAddress = producerAddressPara.getValue();
        }
        //企业地址
        dto.setAddress(producerAddress);
        //企业网址
        String producerWeb = "";
        SysPara producerWebPara = sysParaRepository.load("ProducerWeb");
        if (producerWebPara != null && StringUtils.isNotEmpty(producerWebPara.getValue())) {
            producerWeb = producerWebPara.getValue();
        }
        //企业网址
        dto.setWeb(producerWeb);
    }

    /**
     * 获取工厂号
     *
     * @param defaultFactoryCode 默认工厂号
     * @param batchCode          批次
     * @return 工厂号
     */
    private String getFactoryCode(String defaultFactoryCode, String batchCode) {
        String factoryCode = batchCode.substring(batchCode.length() - 2);
        switch (factoryCode) {
            //双城
            case "49":
                factoryCode = "1";
                break;
            //九三
            case "25":
                factoryCode = "2";
                break;
            //北安
            case "51":
                factoryCode = "3";
                break;
            default:
                factoryCode = defaultFactoryCode;
                break;
        }
        return factoryCode;
    }

    /**
     * 主要原料合格证明
     *
     * @param qcRecord  质检
     * @param webUrlStr web路径
     * @return 合格证明访问地址
     */
    private String getCertificate(ProductQcRecord qcRecord, String webUrlStr) {
        String certificateUrl = qcRecord.getConformityCertificate();
        if (StringUtils.isNotEmpty(qcRecord.getConformityCertificate())) {
            String webUrl = webUrlStr + "/" + qcRecord.getConformityCertificate();
            certificateUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
        }
        return certificateUrl;
    }

    private String getProductStandardPdf(ProductQcRecord qcRecord, String webUrlStr) {
        String productStandardPdfUrl = qcRecord.getProductStandardPdf();
        if (StringUtils.isNotEmpty(qcRecord.getProductStandardPdf())) {
            String webUrl = webUrlStr + "/" + qcRecord.getProductStandardPdf();
            productStandardPdfUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
        }
        return productStandardPdfUrl;
    }

    /**
     * 产品检验报告
     *
     * @param qcRecord  质检
     * @param webUrlStr web路径
     * @return 检验报告访问地址
     */
    private String getReport(ProductQcRecord qcRecord, String webUrlStr) {
        String reportUrl = qcRecord.getProdInspectionReport();
        if (StringUtils.isNotEmpty(qcRecord.getProdInspectionReport())) {
            String webUrl = webUrlStr + "/" + qcRecord.getProdInspectionReport();
            reportUrl = webUrl.replace("/", SEPARATOR).replace("\\", SEPARATOR);
        }
        return reportUrl;
    }
}
