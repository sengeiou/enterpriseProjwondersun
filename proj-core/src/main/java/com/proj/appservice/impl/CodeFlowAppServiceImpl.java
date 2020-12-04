package com.proj.appservice.impl;

import com.alibaba.druid.util.StringUtils;
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
import com.arlen.ebt.entity.BillType;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillTypeRepository;
import com.proj.appservice.CodeFlowAppService;
import com.proj.dto.CodeFlow;
import com.proj.repository.CodeFlowRepository;
import org.dozer.DozerBeanMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 条码流向查询服务实现
 */
@Service
public class CodeFlowAppServiceImpl implements CodeFlowAppService {

    @Resource
    ProductionCodeRepository productionCodeRepository;
    @Resource
    OrganizationRepository organizationRepository;
    @Resource
    CodeFlowRepository codeFlowRepository;
    @Resource
    BillHeaderRepository billHeaderRepository;
    @Resource
    MaterialRepository materialRepository;
    @Resource
    BillTypeRepository billTypeRepository;
    @Resource
    private SysStructureRepository structureRepository;

    @Override
    public APIResult<CodeFlow> getSyncCodeData(String code) {
        APIResult<CodeFlow> result = new APIResult<>();
        code = code.trim();
        CodeFlow codeFlow = new CodeFlow();
        ProductionCode productionCode = productionCodeRepository.load(code);
        if (productionCode == null) {
            return result.fail(404, "条码不存在！");
        }
        DozerBeanMapper mapper = new DozerBeanMapper();
        ProductionCodeDTO productionCodeDTO = mapper.map(productionCode, ProductionCodeDTO.class);
        productionCodeDTO.getMaterialId();
        Material material = materialRepository.load(productionCodeDTO.getMaterialId());
        if (material == null) {
            return result.fail(404, "产品不存在！");
        }
        productionCodeDTO.setMaterialShortName(material.getFullName());
        productionCodeDTO.setAttr1(material.getShortName());
        productionCodeDTO.setMaterialShortCode(material.getShortCode());
        productionCodeDTO.setMaterialSku(material.getSku());
        productionCodeDTO.setPrintDate(productionCode.getPrintDate());
        codeFlow.setCodeInfo(productionCodeDTO);
        List flowList = getFlowInfo(code, productionCode, material);
        codeFlow.setFlowList(flowList);
        return result.succeed().attachData(codeFlow);
    }

    @Override
    public APIResult<CodeFlow> getOrgSyncCodeData(String code, String currentOrgId) {
        APIResult<CodeFlow> result = new APIResult<>();
        code = code.trim();
        CodeFlow codeFlow = new CodeFlow();
        ProductionCode productionCode = productionCodeRepository.load(code);
        if (productionCode == null) {
            return result.fail(404, "条码不存在！");
        }
        DozerBeanMapper mapper = new DozerBeanMapper();
        ProductionCodeDTO productionCodeDTO = mapper.map(productionCode, ProductionCodeDTO.class);
        productionCodeDTO.getMaterialId();
        Material material = materialRepository.load(productionCodeDTO.getMaterialId());
        if (material == null) {
            return result.fail(404, "产品不存在！");
        }
        productionCodeDTO.setMaterialShortName(material.getFullName());
        productionCodeDTO.setAttr1(material.getShortName());
        codeFlow.setCodeInfo(productionCodeDTO);
        List flowList = getFlowInfoForOrg(code, productionCode, material, currentOrgId);
        codeFlow.setFlowList(flowList);
        return result.succeed().attachData(codeFlow);
    }

    //获取流向信息
    public List getFlowInfo(String code, ProductionCode flowCode, Material material) {
        List flowList = new ArrayList<>();
        List<ProductionCode> singleCodes = productionCodeRepository.getSingleCodes(code);
        if (singleCodes == null || singleCodes.size() == 0) {
            return null;
        }
        Map<String, List<ProductionCode>> flowMap = new HashMap<>();
        //
        List<String> billIdList = new ArrayList<>();
        for (ProductionCode productionCode : singleCodes) {
            String route = productionCode.getRoute();
            if (StringUtil.isNotEmpty(route)) {
                String billId = route.split(";")[0].split(",")[0];
                if (!billIdList.contains(billId)) {
                    billIdList.add(billId);
                }
            }
        }
        //查询所有单品码第一个单据发货方列表
        List<Organization> srcOrgList = codeFlowRepository.getSrcOrgByBillIdList(billIdList);
        HashMap<String, Organization> srcOrgMap = new HashMap<>();
        if (srcOrgList != null && srcOrgList.size() > 0) {
            for (Organization org : srcOrgList
                    ) {
                srcOrgMap.put(org.getAttr1(), org); //Attr1中存放了billId
            }
        }
        for (ProductionCode productionCode : singleCodes
                ) {
            String route = productionCode.getRoute();
            String srcOrgId = "";
            if (route != null) {
                String billId = route.split(";")[0].split(",")[0];
                srcOrgId = srcOrgMap.get(billId) != null ? srcOrgMap.get(billId).getId() : "";
            }
//            String key = productionCode.getFactoryId() + "," + productionCode.getShouldOrgId() + "," + productionCode.getIsRunning();
            String key = srcOrgId + "," + productionCode.getShouldOrgId() + "," + productionCode.getIsRunning();
            if (flowMap.containsKey(key)) {
                flowMap.get(key).add(productionCode);
            } else {
                List<ProductionCode> codes = new ArrayList<>();
                codes.add(productionCode);
                flowMap.put(key, codes);
            }
        }
        int i = 1;
        for (Map.Entry<String, List<ProductionCode>> entry : flowMap.entrySet()) {
            List<ProductionCode> productionCodes = entry.getValue();
            if (productionCodes != null || productionCodes.size() > 0) {
                ProductionCode proCode = productionCodes.get(0);
                String route = proCode.getRoute();
                JSONObject json = new JSONObject();
                json.put("index", i); //序号
                //获得发货方名称
                if (StringUtil.isNotEmpty(route)) {
                    String billId = route.split(";")[0].split(",")[0];
                    Organization srcOrg = srcOrgMap.get(billId);
                    if(4 == srcOrg.getOrgType()){
                        json.put("srcOrgName", srcOrg != null ? srcOrg.getFullName() : "");
                    }else{
                        json.put("srcOrgName", srcOrg != null ? srcOrg.getShortName() : "");
                    }
                } else {
                    json.put("srcOrgName", "");
                }
                //发货方名称

//                if (proCode.getFactoryId() == null) {
//                    json.put("srcOrgName", "");
//                } else {
//                    Organization org = organizationRepository.load(proCode.getFactoryId());
//                    json.put("srcOrgName", org != null ? org.getShortName() : "");
//                }
                //收货方名称
                if (proCode.getShouldOrgId() == null) {
                    json.put("shouldOrgName", "");
                } else {
                    Organization org = organizationRepository.load(proCode.getShouldOrgId());
                    if(4 == org.getOrgType()){
                        json.put("shouldOrgName", org != null ? org.getFullName() : " ");
                    }else{
                        json.put("shouldOrgName", org != null ? org.getShortName() : " ");
                    }
                }
                json.put("isRunning", proCode.getIsRunning()); //是否在途
                json.put("qty", productionCodes.size());  //条码数量
                //流向详情
                List flowDetailList = getFlowDetailList(proCode);
                json.put("flowDetailList", flowDetailList);
                //获取单品码id集合
                List codeStrList = new ArrayList<>();
                for (ProductionCode tempCode : productionCodes
                        ) {
                    codeStrList.add(tempCode.getId());
                }
                json.put("codeStrList", codeStrList);
                makeCodeTreeJson(productionCodes, flowCode, material, json); //条码详情

                flowList.add(json);
                i++;
            }
        }

        return flowList;
    }

    //
    //获取流向信息
    private List getFlowInfoForOrg(String code, ProductionCode flowCode, Material material, String currentOrgId) {
        boolean flag = false;//是否存在与当前组织ID关联的流向，如果没有，返回“条码不在销售范围之内！”，如果有，则返回与之有关的记录
        List flowList = new ArrayList<>();
        List<ProductionCode> singleCodes = productionCodeRepository.getSingleCodes(code);
        if (singleCodes == null || singleCodes.size() == 0) {
            return null;
        }
        Map<String, List<ProductionCode>> flowMap = new HashMap<>();
        //
        List<String> billIdList = new ArrayList<>();
        for (ProductionCode productionCode : singleCodes) {
            String route = productionCode.getRoute();
            if (StringUtil.isNotEmpty(route)) {
                String billId = route.split(";")[0].split(",")[0];
                if (!billIdList.contains(billId)) {
                    billIdList.add(billId);
                }
            }
        }
        //查询所有单品码第一个单据发货方列表
        List<Organization> srcOrgList = codeFlowRepository.getSrcOrgByBillIdList(billIdList);
        HashMap<String, Organization> srcOrgMap = new HashMap<>();
        if (srcOrgList != null && srcOrgList.size() > 0) {
            for (Organization org : srcOrgList
                    ) {
                srcOrgMap.put(org.getAttr1(), org); //Attr1中存放了billId
            }
        }
        for (ProductionCode productionCode : singleCodes
                ) {
            String route = productionCode.getRoute();
            String srcOrgId = "";
            if (route != null) {
                String billId = route.split(";")[0].split(",")[0];
                srcOrgId = srcOrgMap.get(billId) != null ? srcOrgMap.get(billId).getId() : "";
            }
//            String key = productionCode.getFactoryId() + "," + productionCode.getShouldOrgId() + "," + productionCode.getIsRunning();
            String key = srcOrgId + "," + productionCode.getShouldOrgId() + "," + productionCode.getIsRunning();
            if (flowMap.containsKey(key)) {
                flowMap.get(key).add(productionCode);
            } else {
                List<ProductionCode> codes = new ArrayList<>();
                codes.add(productionCode);
                flowMap.put(key, codes);
            }
        }
        int i = 1;
        for (Map.Entry<String, List<ProductionCode>> entry : flowMap.entrySet()) {
            List<ProductionCode> productionCodes = entry.getValue();
            if (productionCodes != null || productionCodes.size() > 0) {
                ProductionCode proCode = productionCodes.get(0);
                String route = proCode.getRoute();
                JSONObject json = new JSONObject();
                json.put("index", i); //序号
                //获得发货方名称
                if (StringUtil.isNotEmpty(route)) {
                    String billId = route.split(";")[0].split(",")[0];
                    Organization srcOrg = srcOrgMap.get(billId);
//                    json.put("srcOrgName", srcOrg != null ? srcOrg.getShortName() : "");
                    json.put("srcOrgName", srcOrg != null ? srcOrg.getFullName() : "");
                } else {
                    json.put("srcOrgName", "");
                }
                List flowDetailList = new ArrayList<>();
                boolean isSuccess = getFlowDetailListForOrg(flowDetailList, proCode, currentOrgId);
                if (isSuccess) {
                    flag = true;
                } else {
                    continue;
                }
                //收货方名称
                if (proCode.getShouldOrgId() == null) {
                    json.put("shouldOrgName", "");
                } else {
                    Organization org = organizationRepository.load(proCode.getShouldOrgId());
//                    json.put("shouldOrgName", org != null ? org.getShortName() : "");
                    json.put("shouldOrgName", org != null ? org.getFullName() : "");
                }
                json.put("isRunning", proCode.getIsRunning()); //是否在途
                json.put("qty", productionCodes.size());  //条码数量
                json.put("flowDetailList", flowDetailList);
                //获取单品码id集合
                List codeStrList = new ArrayList<>();
                for (ProductionCode tempCode : productionCodes
                        ) {
                    codeStrList.add(tempCode.getId());
                }
                json.put("codeStrList", codeStrList);
                makeCodeTreeJson(productionCodes, flowCode, material, json); //条码详情

                flowList.add(json);
                i++;
            }
        }
        if (!flag) {
            throw new RuntimeException("条码不在销售范围之内！");
        }
        return flowList;
    }

    /**
     * 获取流向详情集合
     *
     * @param code 单品码
     * @return 流向
     */
    private List getFlowDetailList(ProductionCode code) {
        List<JSONObject> list = new ArrayList<>();
        String route = code.getRoute();
        if (StringUtils.isEmpty(route)) {
            return list;
        }
        String[] billAttr = route.split(";");
        for (int i = 0; i < billAttr.length; i++) {
            String billId = billAttr[i].split(",")[0];
            JSONObject json = new JSONObject();
            String billCode = "";
            String srcOrgName = "";
            String destOrgName = "";
            String billTypeName = "";
            String billTypeId = "";
            //考核城市
            String checkCityName = "";
            //发货方类型
            String srcType = "";
            //收货方类型
            String destType = "";
            Date auditTime = null;
            String addFrom = "";
            Organization srcOrg = new Organization(); // 发货方组织
            Organization destOrg = new Organization(); // 发货方组织
            BillHeader header = billHeaderRepository.load(billId);
            if (header != null) {
                billCode = header.getRefCode();
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(header.getSrcId())) {
                    srcOrg = organizationRepository.load(header.getSrcId());
                    if (srcOrg != null) {
                        //发货方名称
                        srcOrgName = srcOrg.getShortName();
                        switch (srcOrg.getOrgType()) {
                            case 3:
                                srcType = "完达山";
                                break;
                            case 4:
                                srcType = "经销商";
                                if (StringUtil.isNotEmpty(srcOrg.getCheckCityId())) {
                                    SysStructure structure = structureRepository.load(srcOrg.getCheckCityId());
                                    if (structure != null) {
                                        checkCityName = structure.getName();
                                    }
                                }
                                break;
                            case 5:
                                srcType = "分销商";
                                break;
                            case 6:
                                srcType = "门店";
                                break;
                            default:
                        }
                    }
                }
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(header.getDestId())) {
                    destOrg = organizationRepository.load(header.getDestId());
                    if (destOrg != null) {
                        destOrgName = destOrg.getShortName();
                        switch (destOrg.getOrgType()) {
                            case 3:
                                destType = "完达山";
                                break;
                            case 4:
                                destType = "经销商";
                                if (StringUtil.isEmpty(checkCityName) && StringUtil.isNotEmpty(destOrg.getCheckCityId())) {
                                    SysStructure structure = structureRepository.load(destOrg.getCheckCityId());
                                    if (structure != null) {
                                        checkCityName = structure.getName();
                                    }
                                }
                                break;
                            case 5:
                                destType = "分销商";
                                break;
                            case 6:
                                destType = "门店";
                                break;
                            default:
                        }
                    }
                }

                if (org.apache.commons.lang3.StringUtils.isNotEmpty(header.getBillTypeId())) {
                    BillType billType = billTypeRepository.load(header.getBillTypeId());
                    if (billType != null) {
                        billTypeName = billType.getName();
                        billTypeId = billType.getId();
                    }
                }
                auditTime = header.getAuditTime();
                addFrom = header.getAddFrom() == null ? "" : header.getAddFrom();
                switch (addFrom) {
                    case "PDA":
                        addFrom = "采集器";
                        break;
                    case "WEB":
                        addFrom = "网页";
                        break;
                    case "SYNC":
                        addFrom = "同步";
                        break;
                    case "ADJUST":
                        addFrom = "自动调整";
                        break;
                    default:
                        break;
                }
            }
            //单号
            json.put("billCode", billCode);
            // 经销商 取全称
            if("经销商".equals(srcType)){
                //发货方名称
                json.put("srcOrgName", srcOrg.getFullName());
            }else{
                //发货方名称
                json.put("srcOrgName", srcOrgName);
            }

            if("经销商".equals(destType)){
                //收货方名称
                json.put("destOrgName", destOrg.getFullName());
            }else{
                //收货方名称
                json.put("destOrgName", destOrgName);
            }

            //单据类型
            json.put("billTypeName", billTypeName);
            //单据类型
            json.put("billType", billTypeId);
            //时间
            json.put("auditTime", auditTime);
            //单据来源
            json.put("addFrom", addFrom);
            //办事处
            json.put("checkCityName", checkCityName);
            //发货方类型
            json.put("srcType", srcType);
            //收货方类型
            json.put("destType", destType);
            list.add(json);
        }
        return list;
    }

    /**
     * 经销商后台查询流向详情
     * @param  detailList detailList
     * @param code code
     * @param orgId orgId
     * @return boolean
     */
    private boolean getFlowDetailListForOrg(List detailList, ProductionCode code, String orgId) {
        boolean flag = false;
        List list = detailList;
        String route = code.getRoute();
        if (StringUtils.isEmpty(route)) {
            return flag;
        }
        String[] billAttr = route.split(";");
        for (int i = 0; i < billAttr.length; i++) {
            String billId = billAttr[i].split(",")[0];
            JSONObject json = new JSONObject();
            String billCode = "";
            String srcOrgName = "";
            String destOrgName = "";
            String billTypeName = "";
            String billTypeId = "";
            Date auditTime = null;
            String addFrom = "";
            BillHeader header = billHeaderRepository.load(billId);
            if (header != null) {
                billCode = header.getRefCode();
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(header.getSrcId())) {
                    Organization srcOrg = organizationRepository.load(header.getSrcId());
                    if (srcOrg != null) {
                        //发货方名称
                        srcOrgName = srcOrg.getShortName();
                        if (orgId.equals(srcOrg.getId())) {
                            flag = true;
                        }
                    }
                }
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(header.getDestId())) {
                    Organization destOrg = organizationRepository.load(header.getDestId());
                    if (destOrg != null) {
                        destOrgName = destOrg.getShortName();
                        if (orgId.equals(destOrg.getId())) {
                            flag = true;
                        }
                    }
                }
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(header.getBillTypeId())) {
                    BillType billType = billTypeRepository.load(header.getBillTypeId());
                    if (billType != null) {
                        billTypeId = billType.getId();
                        billTypeName = billType.getName();
                    }
                }
                auditTime = header.getAuditTime();
                addFrom = header.getAddFrom() == null ? "" : header.getAddFrom();
                switch (addFrom) {
                    case "PDA":
                        addFrom = "采集器";
                        break;
                    case "WEB":
                        addFrom = "网页";
                        break;
                    case "SYNC":
                        addFrom = "同步";
                        break;
                    case "ADJUST":
                        addFrom = "自动调整";
                        break;
                    default:
                }
            }
            json.put("billCode", billCode); //单号
            json.put("srcOrgName", srcOrgName);//发货方名称
            json.put("destOrgName", destOrgName); //收货方名称
            json.put("billType", billTypeName); //单据类型
            json.put("billTypeId", billTypeId); //单据类型
            json.put("auditTime", auditTime); //时间
            json.put("addFrom", addFrom);//单据来源
            list.add(json);
        }
        return flag;
    }

    /**
     * 构建条码easyui树JSON
     *
     * @param productionCodes 编码集合
     * @param flowCode        查询的编码
     * @param material        产品
     * @param destJson        生成树放到目标JSON
     */
    private void makeCodeTreeJson(List<ProductionCode> productionCodes, ProductionCode flowCode, Material material, JSONObject destJson) {
        if (flowCode.isMinSaleUnit()) {
            if (StringUtil.isNotEmpty(flowCode.getParentCode())) {
                flowCode = productionCodeRepository.load(flowCode.getParentCode());
                productionCodes = productionCodeRepository.getChildren(flowCode.getId());
                destJson.put("qty", productionCodes.size());  //条码数量
            }
            destJson.put("flowQty", 1);  //条码数量
        } else {
            destJson.put("flowQty", productionCodes.size());  //条码数量
        }
        JSONObject detailJson = new JSONObject();
        JSONArray packTreeArr = new JSONArray();
        JSONArray boxTreeArr = new JSONArray();
        List<String> singleCodes = new ArrayList<>();
        if (flowCode.isMinSaleUnit()) {       //如果查询码是单品码
            if (StringUtil.isNotEmpty(flowCode.getParentCode())) {
                List<ProductionCode> codeList = productionCodeRepository.getCodeByParentCode(flowCode.getParentCode());
                if (codeList.size() == 0) {
                    for (ProductionCode tempCode : productionCodes
                            ) {
                        singleCodes.add(tempCode.getId());
                    }
                } else {
                    for (ProductionCode tempCode : codeList
                            ) {
                        singleCodes.add(tempCode.getId());
                    }
                }
            } else {
                for (ProductionCode tempCode : productionCodes
                        ) {
                    singleCodes.add(tempCode.getId());
                }
            }
        } else if (flowCode.getCodeLevel() == 3) {    //如果查询码是托码
            if (productionCodes.size() == material.getBoxQty() * material.getPcsQty()) {
                JSONArray childJsonArr = makeChildJsonArr(flowCode, null);
                JSONObject packTreeJson = new JSONObject();

                packTreeJson.put("id", flowCode.getId());
                packTreeJson.put("text", flowCode.getId());
                packTreeJson.put("state", "closed");
                packTreeJson.put("children", childJsonArr);

                packTreeArr.add(packTreeJson);
            } else {
                List<ProductionCode> boxCodes = productionCodeRepository.getChildren(flowCode.getId());
                Map<String, List<ProductionCode>> codeMap = productionCodes.stream().collect(Collectors.groupingBy(ProductionCode::getParentCode)); //所有单品码按照父码（箱码）分组
                for (ProductionCode boxCode : boxCodes
                        ) {
                    if (codeMap.containsKey(boxCode.getId())) {
                        List<ProductionCode> boxChildCodes = codeMap.get(boxCode.getId());
                        if (boxChildCodes.size() == material.getPcsQty()) {
                            JSONObject boxTreeJson = new JSONObject();

                            boxTreeJson.put("id", boxCode.getId());
                            boxTreeJson.put("text", boxCode.getId());
                            boxTreeJson.put("state", "closed");
                            boxTreeJson.put("children", makeChildJsonArr(boxCode, null));

                            boxTreeArr.add(boxTreeJson);
                        } else {
                            for (ProductionCode singleCode : boxChildCodes
                                    ) {
                                singleCodes.add(singleCode.getId());
                            }
                        }
                    }
                }
            }
        } else {  //剩下箱码的情况
            List<ProductionCode> boxChildCodes = productionCodeRepository.getChildren(flowCode.getId());
            if (productionCodes.size() == material.getPcsQty()) {
                JSONObject boxTreeJson = new JSONObject();

                boxTreeJson.put("id", flowCode.getId());
                boxTreeJson.put("text", flowCode.getId());
                boxTreeJson.put("state", "closed");
                boxTreeJson.put("children", makeChildJsonArr(flowCode, boxChildCodes));

                boxTreeArr.add(boxTreeJson);
            } else {
                for (ProductionCode singleCode : productionCodes
                        ) {
                    singleCodes.add(singleCode.getId());
                }
            }
        }
        detailJson.put("packCodes", packTreeArr);
        detailJson.put("packCodesCount", packTreeArr.size());
        detailJson.put("boxCodes", boxTreeArr);
        detailJson.put("boxCodesCount", boxTreeArr.size());
        detailJson.put("singleCodes", singleCodes);
        detailJson.put("singleCodesCount", singleCodes.size());

        destJson.put("codesDetail", detailJson);
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
