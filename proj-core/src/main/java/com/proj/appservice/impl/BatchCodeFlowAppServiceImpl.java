package com.proj.appservice.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.dto.ProductionCodeDTO;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebc.repository.ProductionCodeRepository;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysStructure;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysStructureRepository;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.proj.appservice.BatchCodeFlowAppService;
import com.proj.dto.CodeFlow;
import com.proj.repository.BatchCodeFlowRepository;
import org.dozer.DozerBeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批号查询服务实现
 */
@Service
public class BatchCodeFlowAppServiceImpl implements BatchCodeFlowAppService {
    private static Logger logger = LoggerFactory.getLogger(BatchCodeFlowAppServiceImpl.class);

    @Resource
    ProductionCodeRepository productionCodeRepository;
    @Resource
    OrganizationRepository organizationRepository;
    @Resource
    BatchCodeFlowRepository codeFlowRepository;
    @Resource
    MaterialRepository materialRepository;
    @Resource
    SysStructureRepository sysStructureRepository;
    @Resource
    BillHeaderRepository billHeaderRepository;

    @Override
    public APIResult<CodeFlow> getSyncCodeData(String batchCode, String materialId) {
        APIResult<CodeFlow> result = new APIResult<>();
        batchCode = batchCode.trim();
        materialId = materialId.trim();
        CodeFlow codeFlow = new CodeFlow();
        Material material1 = materialRepository.load(materialId);
        if (material1 == null) {
            return result.fail(404, "产品不存在！");
        }
        ProductionCode productionCode;
        logger.info("批次流向查询--开始查询上传数量");
        long beginTime = System.currentTimeMillis();
        int uploadQty = codeFlowRepository.getCountByBatchCodeAndMaterialId(batchCode, material1.getId());
        logger.info("批次流向查询--结束查询上传数量，耗时：" + (System.currentTimeMillis() - beginTime) + "秒");
        logger.info("批次流向查询--开始查询发货数量");
        long beginTime2 = System.currentTimeMillis();
        List<ProductionCode> codeList = codeFlowRepository.getInDealerByBatchAndMaterialId(batchCode, material1.getId());
        logger.info("批次流向查询--结束查询发货数量，耗时：" + (System.currentTimeMillis() - beginTime2) + "秒");
        logger.info("批次流向查询--开始查询仓库库存数量");
        long beginTime3 = System.currentTimeMillis();
        int rdcInQty=codeFlowRepository.getRDCInQty(batchCode,materialId);
        logger.info("批次流向查询--结束查询仓库库存数量，耗时：" + (System.currentTimeMillis() - beginTime3) + "秒");
        if (codeList.size() <= 0) {
            return result.fail(404, "条码不存在！");
        }
        productionCode = codeList.get(0);
        DozerBeanMapper mapper = new DozerBeanMapper();
        ProductionCodeDTO productionCodeDTO = mapper.map(productionCode, ProductionCodeDTO.class);
        productionCodeDTO.setMaterialShortName(material1.getFullName());
        productionCodeDTO.setAttr1(material1.getShortName());
        productionCodeDTO.setMaterialShortCode(material1.getShortCode());
        productionCodeDTO.setMaterialSku(material1.getSku());
        codeFlow.setCodeInfo(productionCodeDTO);
        codeFlow.setUploadQty(uploadQty);
        codeFlow.setRdcInQty(rdcInQty);
        logger.info("批次流向查询--开始构建数据");
        long beginTime4 = System.currentTimeMillis();
        List flowList = getBatchCodeFlowInfo(codeList);
        logger.info("批次流向查询--结束构建数据，耗时：" + (System.currentTimeMillis() - beginTime4) + "秒");
        codeFlow.setFlowList(flowList);
        logger.info("批次流向查询--结束查询，耗时：" + (System.currentTimeMillis() - beginTime) + "秒");
        return result.succeed().attachData(codeFlow);
    }

    @Override
    public APIResult<JSONObject> buildTree(String[] codeStr, String billCode) {
        APIResult<JSONObject> result = new APIResult<>();
        List<ProductionCode> productionCodes = new ArrayList<>();
        BillHeader billHeader = billHeaderRepository.getByCode(billCode, "RDCToDealerOut");
        String headerId = "";
        if (billHeader != null) {
            headerId = billHeader.getId();
        }
        for (String code : codeStr) {
            String id = headerId;
            List<ProductionCode> childCode = productionCodeRepository.getSingleCodes(code);
            List<ProductionCode> codeList = childCode.stream().filter(x -> StringUtil.isNotEmpty(x.getRoute()) && x.getRoute().contains(id)).collect(Collectors.toList());
            productionCodes.addAll(codeList);
        }
        JSONObject json = makeDealerInQtyCodeTreeJson(productionCodes);
        return result.succeed().attachData(json);
    }

    public List getBatchCodeFlowInfo(List<ProductionCode> codeList) {
        List flowList = new ArrayList<>();
        //获取该批次编码的当前所在地
        List<String> shouldOrg = new ArrayList<>();
        for (ProductionCode productionCode : codeList) {
            if (!shouldOrg.contains(productionCode.getShouldOrgId())) {
                shouldOrg.add(productionCode.getShouldOrgId());
            }
        }
        Map<String, List<ProductionCode>> flowMap = new HashMap<>();
        for (String s : shouldOrg) {
            Organization organization = organizationRepository.load(s);
            if (organization != null && organization.getOrgType() != 4) {
                continue;
            }
            //获取单品码的单品码
            List<ProductionCode> productionCodes = new ArrayList<>();
            for (ProductionCode productionCode : codeList) {
                if (s.equals(productionCode.getShouldOrgId())) {
                    ProductionCode productionCode1 = productionCodeRepository.load(productionCode.getParentCode());
                    productionCodes.add(productionCode1);
                }
            }
            if (flowMap.containsKey(s)) {
                flowMap.get(s).addAll(productionCodes);
            } else {
                List<ProductionCode> codes = new ArrayList<>();
                codes.addAll(productionCodes);
                flowMap.put(s, codes);
            }
        }
        int i = 1;
        for (Map.Entry<String, List<ProductionCode>> entry : flowMap.entrySet()) {
            List<ProductionCode> productionCodes = entry.getValue();
            if (productionCodes.size() <= 0) {
                continue;
            } else {
                ProductionCode proCode = productionCodes.get(0);
                String shouldOrgId = entry.getKey();
                JSONObject json = new JSONObject();
                json.put("index", i); //序号
                //收货方名称
                if (proCode.getShouldOrgId() == null) {
                    json.put("shouldOrgName", "");
                } else {
                    Organization org = organizationRepository.load(shouldOrgId);
//                    json.put("shouldOrgName", org != null ? org.getShortName() : "");
                    json.put("shouldOrgName", org != null ? org.getFullName() : "");
                    json.put("shouldOrgCode", org != null ? org.getCode() : "");
                    if (org == null) {
                        json.put("shouldOrgCheckCityName", "");
                    } else {
                        if (StringUtil.isEmpty(org.getCheckCityId())) {
                            json.put("shouldOrgCheckCityName", "");
                        } else {
                            SysStructure sysStructure = sysStructureRepository.load(org.getCheckCityId());
                            json.put("shouldOrgCheckCityName", sysStructure != null ? sysStructure.getName() : "");
                        }
                    }
                }
                for (int b = 0; b < productionCodes.size(); b++) {
                    for (int c = productionCodes.size() - 1; c > b; c--) {
                        if (productionCodes.get(b) == productionCodes.get(c)) {
                            productionCodes.remove(c);
                        }
                    }
                }
                json.put("qty", productionCodes.size());  //条码数量
                //获取单品码id集合
                List codeStrList = new ArrayList<>();
                for (ProductionCode tempCode : productionCodes
                        ) {
                    codeStrList.add(tempCode.getId());
                }
                json.put("codeStrList", codeStrList);
                flowList.add(json);
                i++;
            }
        }
        return flowList;
    }

    /**
     * 构建条码easyui树JSON
     *
     * @param productionCodes 编码集合
     */
    private JSONObject makeDealerInQtyCodeTreeJson(List<ProductionCode> productionCodes) {
        List<ProductionCode> parentCodeList = new ArrayList<>();
        JSONArray boxTreeArr = new JSONArray();
        JSONObject detailJson = new JSONObject();
//        List<String> singleCodes = new ArrayList<>();
        for (ProductionCode productionCode : productionCodes) {
            String parentStr = productionCode.getParentCode();
            if (StringUtil.isNotEmpty(parentStr)) {
                //获取箱码集合
                ProductionCode parentCode = productionCodeRepository.load(parentStr);
                if (parentCode != null && !parentCodeList.contains(parentCode)) {
                    parentCodeList.add(parentCode);
                }
            }
        }
        Map<String, List<ProductionCode>> parentCodeMap = productionCodes.stream().collect(Collectors.groupingBy(ProductionCode::getParentCode)); //所有单品码按照父码（箱码）分组
        //循环箱码集合
        if (parentCodeList != null && parentCodeList.size() > 0) {
            for (ProductionCode boxCode : parentCodeList
                    ) {
                if (parentCodeMap.containsKey(boxCode.getId())) {
                    List<ProductionCode> boxChildCodes = parentCodeMap.get(boxCode.getId());
                    JSONObject boxTreeJson = new JSONObject();
                    boxTreeJson.put("id", boxCode.getId());
                    boxTreeJson.put("text", boxCode.getId());
                    boxTreeJson.put("state", "closed");
                    boxTreeJson.put("children", makeChildJsonArr(boxCode, boxChildCodes));
                    boxTreeArr.add(boxTreeJson);
                }
            }
        }
//        for (ProductionCode boxCode : parentCodeList
//                ) {
//            if (parentCodeMap.containsKey(boxCode.getId())) {
//                List<ProductionCode> boxChildCodes = parentCodeMap.get(boxCode.getId());
//                JSONObject boxTreeJson = new JSONObject();
//                boxTreeJson.put("id", boxCode.getId());
//                boxTreeJson.put("text", boxCode.getId());
//                boxTreeJson.put("state", "closed");
//                boxTreeJson.put("children", makeChildJsonArr(boxCode, boxChildCodes));
//                boxTreeArr.add(boxTreeJson);
//            }
//        }
        detailJson.put("boxCodes", boxTreeArr);
        detailJson.put("boxCodesCount", boxTreeArr.size());
//        detailJson.put("singleCodes", singleCodes);
        detailJson.put("singCodesCount", productionCodes.size());
        return detailJson;
    }

    private JSONArray makeChildJsonArr(ProductionCode code, List<ProductionCode> childCodes) {
        JSONArray jsonArray = new JSONArray();
        if (!code.isMinSaleUnit()) {
            if (childCodes == null || childCodes.size() == 0) {
                childCodes = productionCodeRepository.getChildren(code.getId());
            }
            for (ProductionCode tempCode : childCodes
                    ) {
                JSONObject json = new JSONObject();
                json.put("id", tempCode.getId());
                json.put("text", tempCode.getId());
                if (!tempCode.isMinSaleUnit()) {
                    json.put("state", "closed");
                    json.put("children", makeChildJsonArr(tempCode, null));
                }
                jsonArray.add(json);
            }
        } else {
            return null;
        }
        return jsonArray;
    }
}
