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
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.repository.BillTypeRepository;
import com.proj.appservice.CallBackFlowAppService;
import com.proj.dto.CallBackDTO;
import com.proj.dto.CallBackFlow;
import com.proj.repository.BatchCodeFlowRepository;
import com.proj.repository.CodeFlowRepository;
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
 * 条码流向查询服务实现
 */
@Service
public class CallBackFlowAppServiceImpl implements CallBackFlowAppService {
    private static Logger logger = LoggerFactory.getLogger(CallBackFlowAppServiceImpl.class);
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
    BatchCodeFlowRepository batchCodeFlowRepository;

    @Override
    public APIResult<CallBackFlow> getSyncCodeData(String batchCode, String materialId) {
        APIResult<CallBackFlow> result = new APIResult<>();
        batchCode = batchCode.trim();
        CallBackFlow codeFlow = new CallBackFlow();
        logger.info("召回查询--开始查询产品");
        long begin = System.currentTimeMillis();
        Material material = materialRepository.load(materialId);
        logger.info("召回查询--结束查询产品，耗时：" + (System.currentTimeMillis() - begin) + "秒");
        if (material == null) {
            return result.fail(404, "产品不存在！");
        }
        //根据批号和产品sku查询所有编码
        logger.info("召回查询--开始根据批号和产品sku查询所有编码，批次为：" + batchCode + ";产品id为:" + materialId);
        long beginTime = System.currentTimeMillis();
        List<ProductionCode> codeList = batchCodeFlowRepository.getListByBatchCodeAndMaterialId(batchCode, material.getId());
        logger.info("召回查询--结束根据批号和产品sku查询所有编码,耗时：" + (System.currentTimeMillis() - beginTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        if (codeList.size() <= 0) {
            return result.fail(404, "条码不存在！");
        }
        ProductionCode productionCode = codeList.get(0);
        //生产总数量
        List<String> packQtyList = new ArrayList<>();
        for (ProductionCode productionCode1 : codeList) {
            if (productionCode1.isMinSaleUnit()) {
                if (!packQtyList.contains(productionCode1.getParentCode())) {
                    packQtyList.add(productionCode1.getParentCode());
                }
            }
        }
        int packQty = packQtyList.size();
        //仓库库存数量
        List<String> RDCReportQtyList = new ArrayList<>();
        for (ProductionCode productionCode1 : codeList) {
            if (productionCode1.isMinSaleUnit() && 3 == productionCode1.getShouldOrgType()) {
                if (!RDCReportQtyList.contains(productionCode1.getParentCode())) {
                    RDCReportQtyList.add(productionCode1.getParentCode());
                }
            }
        }
        int RDCReportQty = RDCReportQtyList.size();
        //经销商召回总数量，从仓库召回的不算。
        logger.info("召回查询--开始查询经销商召回总数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long beginTime2 = System.currentTimeMillis();
        List<CallBackDTO> callbackList = batchCodeFlowRepository.getBillDataByBillTypeId("RDCCallBack", batchCode, materialId);
        logger.info("召回查询--结束查询经销商召回总数量,耗时：" + (System.currentTimeMillis() - beginTime2) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        List<String> callbackBoxList = new ArrayList<>();
        logger.info("召回查询--开始计算经销商召回数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long callBackTime = System.currentTimeMillis();
        for (ProductionCode productionCode1 : codeList) {
            for (CallBackDTO billData : callbackList) {
                if (billData.getProductionCodeId().equals(productionCode1.getId())) {
                    if (!callbackBoxList.contains(productionCode1.getParentCode())) {
                        callbackBoxList.add(productionCode1.getParentCode());
                    }
                }
            }
        }
        int callbackQty = callbackBoxList.size();
        logger.info("召回查询--结束计算经销商召回数量,耗时：" + (System.currentTimeMillis() - callBackTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        //经销商未召回总数量
        HashMap<String, String> notCallbackQtyList = new HashMap();
        logger.info("召回查询--开始计算经销商没有召回的数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long noCallBackTime = System.currentTimeMillis();
        for (ProductionCode productionCode1 : codeList) {
            if (productionCode1.isMinSaleUnit() && 4 == productionCode1.getShouldOrgType()) {
                if (!notCallbackQtyList.containsKey(productionCode1.getParentCode())) {
                    notCallbackQtyList.put(productionCode1.getParentCode(), null);
                }
            }
        }
        int notCallbackQty = notCallbackQtyList.size();
        logger.info("召回查询--结束计算经销商没有召回的数量,耗时：" + (System.currentTimeMillis() - noCallBackTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        DozerBeanMapper mapper = new DozerBeanMapper();
        ProductionCodeDTO productionCodeDTO = mapper.map(productionCode, ProductionCodeDTO.class);
        productionCodeDTO.getMaterialId();
        productionCodeDTO.setMaterialShortName(material.getFullName());
        productionCodeDTO.setMaterialSku(material.getSku());
        productionCodeDTO.setMaterialShortCode(material.getShortCode());
        codeFlow.setCodeInfo(productionCodeDTO);
        codeFlow.setPackQty(packQty);//生产总数量
        codeFlow.setRDCReportQty(RDCReportQty);//仓库总数量
        codeFlow.setCallbackQty(callbackQty);
        codeFlow.setNotCallbackQty(notCallbackQty);
        logger.info("召回查询--开始构建经销商流向信息，批次为：" + batchCode + ";产品id为:" + materialId);
        long flowListTime = System.currentTimeMillis();
        List flowList = getCallBackFlowInfo(codeList, callbackList, material);
        logger.info("召回查询--结束计算经销商没有召回的数量,耗时：" + (System.currentTimeMillis() - flowListTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        codeFlow.setFlowList(flowList);
        logger.info("召回查询--结束查询，耗时:" + (System.currentTimeMillis() - beginTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        return result.succeed().attachData(codeFlow);
    }

    @Override
    public APIResult<CallBackFlow> getNewSyncCodeData(String batchCode, String materialId) {
        APIResult<CallBackFlow> result = new APIResult<>();
        batchCode = batchCode.trim();
        CallBackFlow codeFlow = new CallBackFlow();
        logger.info("召回查询--开始查询产品");
        long begin = System.currentTimeMillis();
        Material material = materialRepository.load(materialId);
        logger.info("召回查询--结束查询产品，耗时：" + (System.currentTimeMillis() - begin) + "秒");
        if (material == null) {
            return result.fail(404, "产品不存在！");
        }
        //根据批号和产品sku查询生产总数量
        logger.info("召回查询--开始查询生产总数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long beginTime = System.currentTimeMillis();
        int packQty = batchCodeFlowRepository.getTotalProductQty(batchCode, materialId);
        logger.info("召回查询--结束查询生产总数量,耗时：" + (System.currentTimeMillis() - beginTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);

        //根据批号和产品sku查询仓库库存总数量
        logger.info("召回查询--开始查询仓库库存总数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long RDCInTime = System.currentTimeMillis();
        int RDCReportQty = batchCodeFlowRepository.getRDCInQty(batchCode, materialId);
        logger.info("召回查询--结束查询仓库库存总数量,耗时：" + (System.currentTimeMillis() - RDCInTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);

        //经销商召回总数量，从仓库召回的不算。
        logger.info("召回查询--开始查询经销商召回总数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long beginTime2 = System.currentTimeMillis();
        List<ProductionCode> callbackList = batchCodeFlowRepository.getDealerCallBackQty(batchCode, materialId);
        int callbackQty = 0;
        for (ProductionCode code : callbackList) {
            callbackQty += code.getCount();
        }
        logger.info("召回查询--结束查询经销商召回总数量,耗时：" + (System.currentTimeMillis() - beginTime2) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);

        //各个经销商的库存数量
        logger.info("召回查询--开始查询各个经销商的库存数量，批次为：" + batchCode + ";产品id为:" + materialId);
        long dealerInQty = System.currentTimeMillis();
        List<ProductionCode> dealerQtyList = batchCodeFlowRepository.getDealerNotCallBackQty(batchCode, materialId);
        logger.info("召回查询--结束查询各个经销商的库存数量,耗时：" + (System.currentTimeMillis() - dealerInQty) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        List<ProductionCode> list = productionCodeRepository.getListByBatchCodeAndMaterialId(batchCode, materialId);
        ProductionCode code = null;
        if (list.size() <= 0) {
            return result.fail(404, "该批次产品没有生产数据！");
        } else {
            code = list.get(0);
        }
        DozerBeanMapper mapper = new DozerBeanMapper();
        ProductionCodeDTO productionCodeDTO = mapper.map(code, ProductionCodeDTO.class);
        productionCodeDTO.setMaterialShortName(material.getFullName());
        productionCodeDTO.setMaterialSku(material.getSku());
        productionCodeDTO.setMaterialShortCode(material.getShortCode());
        codeFlow.setCodeInfo(productionCodeDTO);
        codeFlow.setPackQty(packQty);//生产总数量
        codeFlow.setRDCReportQty(RDCReportQty);//仓库库存总数量
        codeFlow.setCallbackQty(callbackQty);//召回总数量
        codeFlow.setNotCallbackQty(packQty - RDCReportQty);//未召回总数量（生产总数量-仓库库存总数量）
        logger.info("召回查询--开始构建经销商详细信息，批次为：" + batchCode + ";产品id为:" + materialId);
        long flowListTime = System.currentTimeMillis();
        List flowList = getFlowInfoOfQty(dealerQtyList, callbackList);
        logger.info("召回查询--结束构建经销商详细信息,耗时：" + (System.currentTimeMillis() - flowListTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        codeFlow.setFlowList(flowList);
        logger.info("召回查询--结束查询，耗时:" + (System.currentTimeMillis() - beginTime) + "秒，批次为：" + batchCode + ";产品id为:" + materialId);
        return result.succeed().attachData(codeFlow);
    }

    //获取编码当前所在信息
    public List getFlowInfoOfQty(List<ProductionCode> dealerQtyList, List<ProductionCode> callbackList) {
        List flowList = new ArrayList<>();
        Map<String, Map<String, Integer>> dealerMap = new HashMap<>();
        //先提取经销商库存数量---经销商未召回数量
        for (ProductionCode code : dealerQtyList) {
            String orgId = code.getShouldOrgId();
            int orgQty = code.getCount();
            if (!dealerMap.containsKey(orgId)) {
                Map<String, Integer> map1 = new HashMap<>();
                map1.put("notCallbackQty", orgQty);
                dealerMap.put(orgId, map1);
            } else {
                Map<String, Integer> map1 = dealerMap.get(orgId);
                map1.put("notCallbackQty", map1.get("notCallbackQty") + orgQty);
            }
        }
        //再提取经销商召回的数量--召回数量--callbackQty
        for (ProductionCode code : callbackList) {
            String orgId = code.getShouldOrgId();
            int orgQty = code.getCount();
            if (!dealerMap.containsKey(orgId)) {
                Map<String, Integer> map1 = new HashMap<>();
                map1.put("callbackQty", orgQty);
                dealerMap.put(orgId, map1);
            } else {
                Map<String, Integer> map1 = dealerMap.get(orgId);
                if (!map1.containsKey("callbackQty")) {
                    map1.put("callbackQty", orgQty);
                } else {
                    map1.put("callbackQty", map1.get("callbackQty") + orgQty);
                }
            }
        }
        int i = 1;
        logger.info("获取为经销商赋值三个数量");
        for (Map.Entry<String, Map<String, Integer>> entry : dealerMap.entrySet()) {
            Map<String, Integer> integerMap = entry.getValue();
            JSONObject json = new JSONObject();
            String shouldOrgId = entry.getKey();
            json.put("index", i); //序号
            int callbackQty = integerMap.containsKey("callbackQty") ? integerMap.get("callbackQty") : 0;
            int notCallbackQty = integerMap.containsKey("notCallbackQty") ? integerMap.get("notCallbackQty") : 0;
            // 收货方名称
            Organization org = organizationRepository.load(shouldOrgId);
            json.put("shouldOrgName", org != null ? org.getShortName() : "");
            json.put("shouldOrgCode", org != null ? org.getCode() : "");
            json.put("notCallbackQty", notCallbackQty);  //未召回数量
            json.put("callbackQty", callbackQty);  //召回数量
            json.put("dealerInQty", callbackQty + notCallbackQty);  //收货总数量
            flowList.add(json);
            i++;
        }
        return flowList;
    }

    @Override
    public APIResult<JSONObject> getDealerInventoryList(String batchCode, String materialId, String orgCode) {
        APIResult<JSONObject> result = new APIResult<>();
        List<String> codeList = batchCodeFlowRepository.getDealerInventoryList(batchCode, materialId, orgCode);
        JSONObject json = bulidTree(codeList);
        return result.succeed().attachData(json);
    }

    @Override
    public APIResult<JSONObject> getCallBackFromDealerList(String batchCode, String materialId, String orgCode) {
        APIResult<JSONObject> result = new APIResult<>();
        List<String> codeList = batchCodeFlowRepository.getCallBackFromDealerList(batchCode, materialId, orgCode);
        JSONObject json = bulidTree(codeList);
        return result.succeed().attachData(json);
    }

    @Override
    public APIResult<JSONObject> getAllDealerList(String batchCode, String materialId, String orgCode) {
        APIResult<JSONObject> result = new APIResult<>();
        List<String> codeList1 = batchCodeFlowRepository.getDealerInventoryList(batchCode, materialId, orgCode);
        List<String> codeList2 = batchCodeFlowRepository.getCallBackFromDealerList(batchCode, materialId, orgCode);
        List<String> codeList = new ArrayList<>();
        codeList.addAll(codeList1);
        for (String code : codeList2) {
            if (!codeList.contains(code)) {
                codeList.add(code);
            }
        }
        JSONObject json = bulidTree(codeList);
        return result.succeed().attachData(json);
    }

    //获取编码当前所在信息
    public List getCallBackFlowInfo(List<ProductionCode> codeList, List<CallBackDTO> callbackList, Material material) {
        List flowList = new ArrayList<>();
        //获取未召回的编码
        logger.info("获取未召回的编码");
        List<ProductionCode> notCallbackSingleList = new ArrayList<>();
        for (ProductionCode productionCode1 : codeList) {
            if (productionCode1.isMinSaleUnit() && 4 == productionCode1.getShouldOrgType()) {
                if (!notCallbackSingleList.contains(productionCode1)) {
                    notCallbackSingleList.add(productionCode1);
                }
            }
        }
        //获取已召回的编码
        logger.info("获取已召回的编码");
        Map<String, List<ProductionCode>> stringListMap = new HashMap<>();
        for (CallBackDTO billData : callbackList) {
            String key = billData.getOrgId();
            List<ProductionCode> callbackSingleList = new ArrayList<>();
            for (ProductionCode productionCode1 : codeList) {
                for (CallBackDTO billDataDto : callbackList) {
                    if (billDataDto.getProductionCodeId().equals(productionCode1.getId()) && billDataDto.getOrgId().equals(key)) {
                        callbackSingleList.add(productionCode1);
                    }
                }
            }
            if (!stringListMap.containsKey(key)) {
                stringListMap.put(key, callbackSingleList);
            }
        }

        //获取该批次编码的当前所在地
        logger.info("获取该批次编码的当前所在地");
        List<String> shouldOrg = new ArrayList<>();
        for (ProductionCode productionCode : notCallbackSingleList) {
            if (!shouldOrg.contains(productionCode.getShouldOrgId())) {
                shouldOrg.add(productionCode.getShouldOrgId());
            }
        }
        Map<String, Map<String, List<ProductionCode>>> flowMapList = new HashMap<>();
        for (Map.Entry<String, List<ProductionCode>> entry : stringListMap.entrySet()) {
            if (!shouldOrg.contains(entry.getKey())) {
                shouldOrg.add(entry.getKey());
            }
        }
        for (String s : shouldOrg) {
            Organization organization = organizationRepository.load(s);
            if (organization != null && organization.getOrgType() != 4) {
                continue;
            }
            //获取所在地对应的单品码
            Map<String, List<ProductionCode>> qtyListMap = new HashMap<>();
            List<ProductionCode> productionCodes = new ArrayList<>();
            qtyListMap.put("NotCallbackQty", productionCodes);
            qtyListMap.put("callbackQty", stringListMap.get(s));
            for (ProductionCode productionCode : notCallbackSingleList) {
                if (s.equals(productionCode.getShouldOrgId())) {
                    productionCodes.add(productionCode);
                }
            }
            if (flowMapList.containsKey(s)) {
                flowMapList.get(s).putAll(qtyListMap);
            } else {
                Map<String, List<ProductionCode>> qtyList = new HashMap<>();
                qtyList.put("NotCallbackQty", productionCodes);
                qtyList.put("callbackQty", stringListMap.get(s));
                flowMapList.put(s, qtyList);
            }
        }
        int i = 1;
        logger.info("获取为经销商赋值三个数量");
        for (Map.Entry<String, Map<String, List<ProductionCode>>> entry : flowMapList.entrySet()) {
            Map<String, List<ProductionCode>> listMap = entry.getValue();
            JSONObject json = new JSONObject();
            //未能召回的单品码
            List<ProductionCode> NotCallbackQty = listMap.get("NotCallbackQty");
            List<ProductionCode> callbackQtyList = listMap.get("callbackQty");
            //收货方名称
            String shouldOrgId = entry.getKey();
            //召回的单品码
            if (!(callbackQtyList != null && callbackQtyList.size() > 0) && !(NotCallbackQty != null && NotCallbackQty.size() > 0)) {
                continue;
            }
            //收货单品码(召回的集合加上未召回的集合)
//            List<ProductionCode> dealerInQtyCodeList = new ArrayList<>();
//            dealerInQtyCodeList.addAll(NotCallbackQty);
//            if (callbackQtyList != null && callbackQtyList.size() > 0) {
//                for (ProductionCode productionCode : callbackQtyList) {
//                    if (!dealerInQtyCodeList.contains(productionCode)) {
//                        dealerInQtyCodeList.add(productionCode);
//                    }
//                }
//            }
            int callbackQty = 0;
            Map<String, List<ProductionCode>> callbackQtyMap = new HashMap<>();
            if (callbackQtyList != null && callbackQtyList.size() > 0) {
                callbackQty = callbackQtyList.size();
                callbackQtyMap = callbackQtyList.stream().collect(Collectors.groupingBy(ProductionCode::getParentCode)); //所有单品码按照父码（箱码）分组
            }
            List codeStrList = new ArrayList<>();
            int notCallbackQty = 0;
            Map<String, List<ProductionCode>> notCallbackQtyMap = new HashMap<>();
            if (NotCallbackQty.size() > 0) {
                notCallbackQty = NotCallbackQty.size();
                notCallbackQtyMap = NotCallbackQty.stream().collect(Collectors.groupingBy(ProductionCode::getParentCode)); //所有单品码按照父码（箱码）分组
            }
            json.put("index", i); //序号
            // 收货方名称
            Organization org = organizationRepository.load(shouldOrgId);
            json.put("shouldOrgName", org != null ? org.getShortName() : "");
            json.put("shouldOrgCode", org != null ? org.getCode() : "");
            json.put("notCallbackQty", notCallbackQtyMap.size());  //未召回数量
            json.put("notCallbackQtyPcs", notCallbackQty);  //未召回数量
            json.put("callbackQty", callbackQtyMap.size());  //召回数量
            json.put("callbackQtyPcs", callbackQty);  //召回数量
            json.put("dealerInQty", callbackQtyMap.size() + notCallbackQtyMap.size());  //收货总数量
            json.put("dealerInQtyPcs", callbackQty + notCallbackQty);  //收货总数量
            json.put("codeStrList", codeStrList);
//            if (dealerInQtyCodeList.size() > 0) {
//                makeDealerInQtyCodeTreeJson(dealerInQtyCodeList, json, "dealerInQtyDetailList"); //收货条码详情
//            }
//            if (callbackQtyList != null && callbackQtyList.size() > 0) {
//                makeDealerInQtyCodeTreeJson(callbackQtyList, json, "CallbackQtyDetailList"); //召回条码详情
//            }
//            if (NotCallbackQty.size() > 0) {
//                makeDealerInQtyCodeTreeJson(NotCallbackQty, json, "notCallbackQtyDetailList"); //位召回条码详情
//            }
            flowList.add(json);
            i++;
        }
        return flowList;
    }

    /**
     * 构建条码easyui树JSON
     *
     * @param productionCodes 编码集合
     * @param destJson        生成树放到目标JSON
     */
    private void makeDealerInQtyCodeTreeJson(List<ProductionCode> productionCodes, JSONObject destJson, String jasonName) {
        List<ProductionCode> parentCodeList = new ArrayList<>();
        JSONArray boxTreeArr = new JSONArray();
        JSONObject detailJson = new JSONObject();
        JSONArray packTreeArr = new JSONArray();
        List<String> singleCodes = new ArrayList<>();
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
        detailJson.put("packCodes", packTreeArr);
        detailJson.put("packCodesCount", packTreeArr.size());
        detailJson.put("boxCodes", boxTreeArr);
        detailJson.put("boxCodesCount", boxTreeArr.size());
        detailJson.put("singleCodes", singleCodes);
        detailJson.put("singleCodesCount", singleCodes.size());
        destJson.put(jasonName, detailJson);
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

    private JSONObject bulidTree(List<String> codeList) {
        List<ProductionCode> parentCodeList = new ArrayList<>();
        List<ProductionCode> productionCodes = new ArrayList<>();
        JSONArray boxTreeArr = new JSONArray();
        JSONObject detailJson = new JSONObject();
        JSONArray packTreeArr = new JSONArray();
        List<String> singleCodes = new ArrayList<>();
        for (String parentStr : codeList) {
            if (StringUtil.isNotEmpty(parentStr)) {
                //获取箱码集合
                ProductionCode parentCode = productionCodeRepository.load(parentStr);
                List<ProductionCode> childCodeList = productionCodeRepository.getSingleCodes(parentStr);
                if (parentCode != null && !parentCodeList.contains(parentCode)) {
                    parentCodeList.add(parentCode);
                    productionCodes.addAll(childCodeList);
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
        detailJson.put("packCodes", packTreeArr);
        detailJson.put("packCodesCount", packTreeArr.size());
        detailJson.put("boxCodes", boxTreeArr);
        detailJson.put("boxCodesCount", boxTreeArr.size());
        detailJson.put("singleCodes", singleCodes);
        detailJson.put("singleCodesCount", singleCodes.size());
        detailJson.put("qty", productionCodes.size());
        return detailJson;
    }
}
