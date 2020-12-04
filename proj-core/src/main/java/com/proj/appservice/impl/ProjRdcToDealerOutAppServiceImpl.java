package com.proj.appservice.impl;

import com.alibaba.fastjson.JSON;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.eaf.core.dto.APIResultCode;
import com.arlen.eaf.core.dto.ConditionItem;
import com.arlen.eaf.core.dto.PageReq;
import com.arlen.eaf.core.enums.ConditionFieldType;
import com.arlen.eaf.core.enums.ConditionOperator;
import com.arlen.eaf.core.utils.StringUtil;
import com.arlen.ebc.entity.ProductionCode;
import com.arlen.ebd.entity.Material;
import com.arlen.ebd.repository.MaterialRepository;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebt.dto.BillDetailDTO;
import com.arlen.ebt.dto.BillHeaderDTO;
import com.arlen.ebt.dto.STBillHeaderDTO;
import com.arlen.ebt.entity.BillData;
import com.arlen.ebt.entity.BillDetail;
import com.arlen.ebt.entity.BillHeader;
import com.arlen.ebt.entity.BillSubDetail;
import com.arlen.ebt.enums.BillStatus;
import com.arlen.ebt.repository.BillDataRepository;
import com.arlen.ebt.repository.BillHeaderRepository;
import com.arlen.ebt.util.ComUtils;
import com.arlen.ebt.util.Utils;
import com.arlen.ebu.appservice.IdKeyAppService;
import com.proj.appservice.BillPushToYsRecordAppService;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.appservice.ProjRdcToDealerOutAppService;
import com.proj.repository.ProjBillHeaderRepository;
import com.proj.repository.ProjProductionCodeRepository;
import com.proj.repository.ProjStBillHeaderRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by arlenChen on 2017/5/16.
 * 单据导入
 *
 * @author arlenChen
 */
@Service
public class ProjRdcToDealerOutAppServiceImpl implements ProjRdcToDealerOutAppService {
    private static Logger logger = LoggerFactory.getLogger(ProjRdcToDealerOutAppServiceImpl.class);
    @Resource
    private BillHeaderRepository billHeaderRepository;
    @Resource
    private ProjBillHeaderRepository projBillHeaderRepository;
    @Resource
    private ProjStBillHeaderRepository projStBillHeaderRepository;
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private MaterialRepository materialRepository;
    @Resource
    private SysParaRepository sysParaRepository;
    @Resource
    private IdKeyAppService idKeyAppService;
    @Resource
    private ProjProductionCodeRepository productionCodeRepository;
    @Resource
    private BillDataRepository billDataRepository;
    @Resource
    private BillPushToYsRecordAppService billPushToYsRecordAppService;
    /**
     * map初始化集合大小
     */
    private static final int INITIAL_CAPACITY = 16;
    private static final Pattern PATTERN = Pattern.compile("^[0-9]*$");

    /**
     * 验证导入的其他出库单数据
     *
     * @param dataList dataList
     * @return APIResult
     */
    @Override
    public APIResult<Map<String, Object>> checkDataIsNormal(List<Map<String, String>> dataList, String importType, String orgId) {
        APIResult<Map<String, Object>> result = new APIResult<>();
        if (dataList == null || dataList.isEmpty()) {
            return result.fail(APIResultCode.UNEXPECTED_ERROR, "导入数据为空");
        }
        Map<String, Object> resultMap = new HashMap<>(INITIAL_CAPACITY);
        //组织数据(key->code;type)
        Map<String, Organization> organizationMap = new HashMap<>(INITIAL_CAPACITY);
        //所有成品产品
        Map<String, Material> materialMap = new HashMap<>(INITIAL_CAPACITY);
        //本次导入结果是否正常
        boolean isNormal = true;
        for (Map<String, String> rowMap : dataList) {
            //出库单号*
            String refCode = rowMap.get("refCode");
            //单据日期*
            String operateTime = rowMap.get("operateTime");
            //发货方编码*
            String srcCode = rowMap.get("srcCode");
            //发货方名称
            String srcName = rowMap.get("srcName");
            //收货方编码*
            String destCode = rowMap.get("destCode");
            //收货方名称
            String destName = rowMap.get("destName");
            //产品ERP代码*
            String sku = rowMap.get("sku");
            //产品名称
            String fullName = rowMap.get("fullName");
            //应发单品数*
            String expectQtyPcs = rowMap.get("expectQtyPcs");
            //校验数据不通过信息
            String errMsg = "";
            rowMap.put("errMsg", errMsg);
            //1.校验 单据日期 yyyy-MM-dd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date operateTime_d = sdf.parse(operateTime);
            } catch (ParseException e) {
                e.printStackTrace();
                excelUtilAppService.putErrMsg(rowMap, "单据日期格式应为:yyyy-MM-dd");
            }
            //2.校验发货方编码
            if (StringUtil.isNotEmpty(importType) && "org".equals(importType)) {
                rowMap.put("srcId", orgId);
            } else {
                if (StringUtil.isEmpty(srcCode)) {
                    excelUtilAppService.putErrMsg(rowMap, "发货方编码不能为空");
                } else {
                    Organization src;
                    if (organizationMap.containsKey(srcCode + ";" + OrgType.RDC.index)) {
                        src = organizationMap.get(srcCode + ";" + OrgType.RDC.index);
                    } else {
                        src = organizationRepository.getByCode(srcCode, OrgType.RDC);
                        organizationMap.put(srcCode + ";" + OrgType.RDC.index, src);
                    }
                    if (src == null) {
                        excelUtilAppService.putErrMsg(rowMap, "发货方不存在");
                    } else {
                        rowMap.put("srcId", src.getId());
                        //发货方名称
                        srcName = src.getShortName();
                        rowMap.put("srcName", srcName);
                    }

                }
            }
            //3.校验收货方编码
            if (StringUtil.isEmpty(destCode)) {
                excelUtilAppService.putErrMsg(rowMap, "收货方编码不能为空");
            } else {
                Organization dest;
                if (organizationMap.containsKey(destCode + ";" + OrgType.DEALER.index)) {
                    dest = organizationMap.get(destCode + ";" + OrgType.DEALER.index);
                } else if (organizationMap.containsKey(destCode + ";" + OrgType.STORE.index)) {
                    dest = organizationMap.get(destCode + ";" + OrgType.STORE.index);
                } else {
                    dest = organizationRepository.getByCode(destCode, OrgType.DEALER);
                    if (dest != null) {
                        organizationMap.put(destCode + ";" + OrgType.DEALER.index, dest);
                    } else {
                        dest = organizationRepository.getByCode(destCode, OrgType.STORE);
                        if (dest != null) {
                            organizationMap.put(destCode + ";" + OrgType.STORE.index, dest);
                        }
                    }
                }
                if (dest == null) {
                    excelUtilAppService.putErrMsg(rowMap, "收货方不存在");
                } else if (!dest.getInUse()) {
                    excelUtilAppService.putErrMsg(rowMap, "收货方已禁用");
                } else {
                    rowMap.put("destId", dest.getId());
                    rowMap.put("orgType", dest.getOrgType() + "");
                    //收货方名称
                    destName = dest.getShortName();
                    rowMap.put("destName", destName);
                }

            }
            //4.校验收货方与发货方是否相同
            boolean isFit = StringUtil.isEmpty(srcCode) && StringUtil.isEmpty(destCode) && destCode.equals(srcCode);
            if (isFit) {
                excelUtilAppService.putErrMsg(rowMap, "收货方与发货方不能相同");
            }
            //6.校验产品
            if (StringUtil.isEmpty(sku)) {
                excelUtilAppService.putErrMsg(rowMap, "产品sku不能为空");
            } else {
                Material material;
                if (materialMap.containsKey(sku)) {
                    material = materialMap.get(sku);
                } else {
                    material = materialRepository.getBySKU(sku);
                    materialMap.put(sku, material);
                }
                if (material == null) {
                    excelUtilAppService.putErrMsg(rowMap, "产品不存在");
                } else if (!material.getInUse()) {
                    excelUtilAppService.putErrMsg(rowMap, "产品已禁用");
                } else {
                    rowMap.put("materialId", material.getId());
                    //产品名称
                    fullName = material.getFullName();
                    rowMap.put("fullName", fullName);
                }

            }
            if (StringUtil.isEmpty(expectQtyPcs)) {
                excelUtilAppService.putErrMsg(rowMap, "应发单罐数不能为空");
            } else {
                Matcher matcher = PATTERN.matcher(expectQtyPcs);
                if (!matcher.matches()) {
                    excelUtilAppService.putErrMsg(rowMap, "应发单罐数不是整数");
                }
            }

            // -------------出库单号不能与现有单据的出库单号重复-------------START
            PageReq pageReq = new PageReq();
            pageReq.setPage(1);
            pageReq.setRows(Integer.MAX_VALUE);
            ConditionItem cond = new ConditionItem();
            cond.setField("RefCode");
            cond.setOperator(ConditionOperator.EQUAL);
            cond.setValue(refCode);
            cond.setType(ConditionFieldType.STRING);
            // -------------add conds--------------
            pageReq.getConditionItems().add(cond);
            Page<Map> page = billHeaderRepository.getPageList(pageReq);
            List<Map> billHeaderList = page.getContent();
            int billHeaderCount = billHeaderList.size();
            if (billHeaderCount > 0) {
                excelUtilAppService.putErrMsg(rowMap, refCode + ":出库单号不能与现有单据的出库单号重复");
            }
            // -------------出库单号不能与现有单据的出库单号重复-------------END

            if (StringUtil.isEmpty(srcCode)) {
                rowMap.put("srcCode", " ");
            }

            if (StringUtil.isEmpty(destCode)) {
                rowMap.put("destCode", " ");
            }

            if (StringUtil.isEmpty(sku)) {
                rowMap.put("sku", " ");
            }

            if (StringUtil.isEmpty(expectQtyPcs)) {
                rowMap.put("expectQtyPcs", " ");
            }
            if (StringUtil.isEmpty(expectQtyPcs)) {
                rowMap.put("expectQtyPcs", " ");
            }
            if (StringUtil.isEmpty(rowMap.get("errMsg"))) {
                rowMap.put("isNormal", "1");
            } else {
                rowMap.put("isNormal", "0");
                isNormal = false;
            }
        }
        resultMap.put("dataList", dataList);
        resultMap.put("isNormal", isNormal);
        return result.succeed().attachData(resultMap);
    }

    /**
     * 根据出库单号分组并校验同一出库单号 的 发货方 和 收货方 是否一致
     *
     * @param dataList
     * @param importType
     * @param orgId
     * @return
     */
    @Override
    public APIResult<Map<String, Object>> groupByRefCodeDataAndCheck(List<Map<String, String>> dataList, String importType, String orgId) {
        APIResult<Map<String, Object>> result = new APIResult<>();
        if (dataList == null || dataList.isEmpty()) {
            return result.fail(APIResultCode.UNEXPECTED_ERROR, "导入数据为空");
        }
        Map<String, Object> resultMap = new HashMap<>(INITIAL_CAPACITY);
        //校验map
        Map<String, List<Map<String, String>>> map = new HashMap<>(INITIAL_CAPACITY);
        //本次导入结果是否正常
        boolean isNormal = true;
        for (Map<String, String> rowMap : dataList) {
            //应发单品数*
            String expectQtyPcs = rowMap.get("expectQtyPcs");
            //发货方Id
            String srcId = rowMap.get("srcId");
            //收货方Id
            String destId = rowMap.get("destId");
            //收货方类型
            String orgType = rowMap.get("orgType");
            //产品Id
            String materialId = rowMap.get("materialId");
            //发货方编码*
            String srcCode = rowMap.get("srcCode");
            //发货方名称
            String srcName = rowMap.get("srcName");
            //收货方编码*
            String destCode = rowMap.get("destCode");
            //收货方名称
            String destName = rowMap.get("destName");
            //出库单号*
            String refCode = rowMap.get("refCode");
            List<Map<String, String>> itemList;
            if (map.containsKey(refCode)) {
                itemList = map.get(refCode);
            } else {
                itemList = new ArrayList<>();
                map.put(refCode, itemList);
            }
            itemList.add(rowMap);
        }

        // 对同一出库单号分组内的 发货方 和 收货方 进行校验：同一单据的发货方、收货方需一致；
        for (Map.Entry<String, List<Map<String, String>>> entry : map.entrySet()) {
            String key_en = entry.getKey();
            List<Map<String, String>> val_en = entry.getValue();
            //发货方编码set 用于校验 同一单据的发货方、收货方需一致；
            Set<String> srcCodeSet = new HashSet<>();
            //收货方编码set 用于校验 同一单据的发货方、收货方需一致；
            Set<String> destCodeSet = new HashSet<>();
            // 循环校验
            for (Map<String, String> rowMap : val_en) {
                //发货方编码*
                String srcCode = rowMap.get("srcCode");
                srcCodeSet.add(srcCode);
                //收货方编码*
                String destCode = rowMap.get("destCode");
                destCodeSet.add(destCode);
            }
            //发货方编码set 长度大于1 证明 同一单据的发货方、收货方需不一致；
            if (srcCodeSet.size() > 1) {
                isNormal = false;
                // 并找出 同一单据的 所有单据 标记异常
                for (Map<String, String> rowMap : dataList) {
                    //出库单号*
                    String refCode = rowMap.get("refCode");
                    if (key_en.equals(refCode)) {
                        rowMap.put("isNormal", "0");
                    }
                }
            }
            //收货方编码set 长度大于1 证明 同一单据的发货方、收货方需不一致；
            if (destCodeSet.size() > 1) {
                isNormal = false;
                // 并找出 同一单据的 所有单据 标记异常
                for (Map<String, String> rowMap : dataList) {
                    //出库单号*
                    String refCode = rowMap.get("refCode");
                    if (key_en.equals(refCode)) {
                        rowMap.put("isNormal", "0");
                        rowMap.put("remark", "");
                        rowMap.put("errMsg", "同一单据的发货方、收货方需不一致");
                    }
                }
            }

        }

        resultMap.put("dataList", dataList);
        resultMap.put("isNormal", isNormal);
        resultMap.put("groupMap", map);
        return result.succeed().attachData(resultMap);
    }


    /**
     * 保存导入结果
     *
     * @param dataList 库存集合
     * @param userDTO  当前用户
     * @return APIResult
     */
    @Override
    public APIResult<String> saveData(List<Map<String, String>> dataList, SysUserDTO userDTO) throws Exception {
        APIResult<String> result = new APIResult<>();
        if (dataList == null || dataList.isEmpty()) {
            return result.fail(APIResultCode.FORBIDDEN, "导入数据为空！");
        }

        //所有成品产品
        Map<String, Material> materialMap = new HashMap<>(INITIAL_CAPACITY);
        Map<String, List<BillDetailDTO>> map = new HashMap<>(INITIAL_CAPACITY);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String operateTime = sdf.format(new Date());
        Date operateTime_d = sdf.parse(operateTime);
        for (Map<String, String> rowMap : dataList) {
            //产品Id
            String materialId = rowMap.get("materialId");
            Material material = materialRepository.getById(materialId);
            // 箱内（中包/单品）数量
            int pcsQty = material.getPcsQty();
            //应发单品数*
            String expectQtyPcs = rowMap.get("expectQtyPcs");
            // SW-15148 新增的仓库出库单导入功能，模板中单品数改为箱数
            int expectQtyPcs_i = Integer.parseInt(expectQtyPcs) * pcsQty;
            // 单据日期*
            operateTime = rowMap.get("operateTime");

            //发货方Id
            String srcId = rowMap.get("srcId");
            //收货方Id
            String destId = rowMap.get("destId");
            //收货方类型
            String orgType = rowMap.get("orgType");
            //出库单号*
            String refCode = rowMap.get("refCode");
            BillDetailDTO dto = new BillDetailDTO();
            dto.setExpectQtyPcs(expectQtyPcs_i);  // SW-15148 新增的仓库出库单导入功能，模板中单品数改为箱数
            dto.setMaterialId(materialId);
            dto.setRemark(orgType);
            dto.setRefCode(refCode);
            dto.setExt7(operateTime);  // 单据日期
            List<BillDetailDTO> dtoList;
            if (map.containsKey(srcId + ";" + destId)) {
                dtoList = map.get(srcId + ";" + destId);
            } else {
                dtoList = new ArrayList<>();
                map.put(srcId + ";" + destId, dtoList);
            }
            dtoList.add(dto);
        }
        List<BillHeader> billHeaderList = new ArrayList<>();
        //获取产品计量单位
        String minStr = "";
        String stdStr = "";
        SysPara minUnit = sysParaRepository.load("min_unit_name");
        SysPara stdUnit = sysParaRepository.load("std_unit_name");
        if (minUnit == null || stdUnit == null) {
            return result.fail(APIResultCode.UNEXPECTED_ERROR, "获取产品计量单位失败！");
        }
        minStr = minUnit.getValue();
        stdStr = stdUnit.getValue();
        for (Map.Entry<String, List<BillDetailDTO>> entry : map.entrySet()) {
            //箱数计数
            int stdCount = 0;
            //个数计数
            int minCount = 0;
            String[] strList = entry.getKey().split(";");
            List<BillDetailDTO> list = entry.getValue();
            BillDetailDTO detailDTO = list.get(0);
            BillHeader billHeader = new BillHeader();
            billHeader.setId(UUID.randomUUID().toString());
            billHeader.setSrcId(strList[0]);
            billHeader.setDestId(strList[1]);
            billHeader.setAddBy(userDTO.getUserName());
            billHeader.setAddTime(new Date());
            billHeader.setEditBy(userDTO.getUserName());
            billHeader.setEditTime(new Date());
            billHeader.setAddFrom("WEB");
            // 1=待下达，2=待扫描，3=扫描中，4=处理中，5=已完成，6=已作废，7=待重扫
            billHeader.setBillStatus(BillStatus.Created.index);
            billHeader.setExt1("excelImport"); // excel导入
            String code;
            if ("6".equals(detailDTO.getRemark())) {
                billHeader.setBillTypeId("RDCToStoreOut");
                //生成单号
                code = idKeyAppService.generateOne("RDCToStoreOut").getData();
            } else {
                billHeader.setBillTypeId("RDCToDealerOut");
                //生成单号
                code = idKeyAppService.generateOne("RDCToDealerOut").getData();
            }
            billHeader.setCode(code);
            billHeader.setRefCode(detailDTO.getRefCode());
            billHeader.setReceiveStatus("N");
            String ext7 = detailDTO.getExt7();  // 单据日期
            operateTime_d = sdf.parse(ext7);
            billHeader.setOperateTime(operateTime_d);
            //根据materialId,attr1(批号）分组。形成一个map集合。java 8.0
            Map<String, List<BillDetailDTO>> pdtLst = list.stream().collect(Collectors.groupingBy(BillDetailDTO::getOnlyKeyString));
            List<BillDetail> detailList = new ArrayList<>();
            Iterator i = pdtLst.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                //BillDetailDTO转换成BillDetail
                BillDetail detail = Utils.getDozerBeanMapper().map(((List<BillDetailDTO>) e.getValue()).get(0), BillDetail.class);
                Material tempMaterial = null;
                if (e.getKey() != null && !e.getKey().toString().isEmpty()) {
                    Material material;
                    if (materialMap.containsKey(e.getKey().toString().split(",")[0])) {
                        material = materialMap.get(e.getKey().toString().split(",")[0]);
                    } else {
                        material = materialRepository.load(e.getKey().toString().split(",")[0]);
                        materialMap.put(e.getKey().toString().split(",")[0], material);
                    }
                    tempMaterial = material;
                    if (material != null) {
                        detail.setMaterial(material);
                        if (detail.getSubList() != null && !detail.getSubList().isEmpty()) {
                            detail.getSubList().forEach(x -> x.setMaterial(material));
                        }
                    }
                }
                detail.getSubList().forEach(x -> x.setParent(detail));
                //所有的detailDTO中应发数的sum添加到detail中
                detail.setExpectQtyPcs(((List<BillDetailDTO>) e.getValue()).stream().mapToInt(x -> x.getExpectQtyPcs()).sum());
                //计算明细预计箱数
                if (tempMaterial != null && tempMaterial.getPcsQty() != 0) {
                    int stdQty = detail.getExpectQtyPcs() / tempMaterial.getPcsQty();
                    int minQty = detail.getExpectQtyPcs() % tempMaterial.getPcsQty();
                    stdCount = stdCount + stdQty;
                    minCount = minCount + minQty;
                    detail.setExpectQtyStr(excelUtilAppService.concatQtyStr(stdQty, minQty, stdStr, minStr));
                    detail.setExpectQtyNew(new BigDecimal(detail.getExpectQtyPcs()).divide(new BigDecimal(tempMaterial.getPcsQty()), 2, BigDecimal.ROUND_HALF_EVEN));
                } else {
                    detail.setExpectQtyStr("0");
                    detail.setExpectQtyPcs(0);
                    detail.setExpectQtyNew(BigDecimal.ZERO);
                }
                detail.setRealQtyStr("0");
                //设置多对一管理关系
                detail.setBillHeader(billHeader);
                detail.setId(UUID.randomUUID().toString());
                detailList.add(detail);
            }
            //设置箱数字符串
            billHeader.setExpectQtyStr(excelUtilAppService.concatQtyStr(stdCount, minCount, stdStr, minStr));
            billHeader.setRealQtyStr("0");
            //设置一对多管理关系
            billHeader.setDetailList(detailList);
            //DetailList中的应发数的sum添加到billHeader中
            billHeader.setExpectQtyPcs(billHeader.getDetailList().stream().mapToInt(x -> x.getExpectQtyPcs()).sum());
            billHeaderList.add(billHeader);
        }
        billHeaderRepository.insert(billHeaderList);
        return result.succeed();
    }

    /**
     * 保存分组导入结果
     *
     * @param groupMap
     * @param userDTO
     * @return
     * @throws Exception
     */
    @Override
    public APIResult<String> saveGroupData(Map<String, List<Map<String, String>>> groupMap, SysUserDTO userDTO) throws Exception {
        APIResult<String> apiResult = new APIResult<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : groupMap.entrySet()) {
            String refCode = entry.getKey();
            List<Map<String, String>> dataList = entry.getValue();
            apiResult = this.saveData(dataList, userDTO);
            if (0 != apiResult.getCode()) {
                throw new RuntimeException("保存分组导入结果异常");
            }
        }
        return apiResult;
    }

    /**
     * 生成明细
     *
     * @param orgId 经销商
     * @return APIResult
     */
    @Override
    public APIResult<List<BillDetailDTO>> getExpectQtyPcsDetailList(String orgId) {
        APIResult<List<BillDetailDTO>> result = new APIResult<>();
        //获取所有产品以及对应经销商库存生成明细
        List<BillDetailDTO> billDetailList = projBillHeaderRepository.generateBillDetailList(orgId);
        for (BillDetailDTO dto : billDetailList) {
            if (StringUtil.isNotEmpty(dto.getMaterialId())) {
                Material material = materialRepository.load(dto.getMaterialId());
                if (material != null) {
                    dto.setMaterialId(material.getId());
                    dto.setMaterialName(material.getShortName());
                    dto.setMaterialCode(material.getShortCode());
                    dto.setMaterialSku(material.getSku());
                    dto.setMaterialSpec(material.getSpec());
                }
            }
        }
        return result.succeed().attachData(billDetailList);
    }

    /**
     * 是否有在处理的单子
     *
     * @param orgId 经销商
     * @return boolean
     */
    @Override
    public boolean haveProcessingBill(String orgId) {
        List<STBillHeaderDTO> stDtoList = projStBillHeaderRepository.listByOrgId(orgId);
        List<BillHeaderDTO> headerDTOList = projBillHeaderRepository.listByOrgId(orgId);
        return !(stDtoList.isEmpty() && headerDTOList.isEmpty());
    }

    /**
     * 迁移单确认
     *
     * @param id       单据Id
     * @param userName 确认人
     * @return APIResult
     */
    @Override
    public APIResult<String> transferBillConfirm(String id, String userName) {
        APIResult<String> result = new APIResult<>();
        BillHeader billHeader = projBillHeaderRepository.load(id);
        if (billHeader == null) {
            return result.fail(404, "单据不存在");
        }
        billHeader.setBillStatus(BillStatus.Submitted.index);
        projBillHeaderRepository.save(billHeader);
        List<ProductionCode> codeList = new ArrayList<>();
        List<BillData> dataList = new ArrayList<>();
        //箱数计数
        int stdCount = 0;
        //个数计数
        int minCount = 0;
        //region获取产品计量单位
        String minStr = "";
        String stdStr = "";
        SysPara minUnit = sysParaRepository.load(ComUtils.MIN_UNIT_NAME);
        SysPara stdUnit = sysParaRepository.load(ComUtils.STD_UNIT_NAME);
        if (minUnit != null && stdUnit != null) {
            minStr = minUnit.getValue();
            stdStr = stdUnit.getValue();
        }
        if (stdUnit == null) {
            stdStr = "-";
        }
        BigDecimal realQty = BigDecimal.ZERO;
        for (BillDetail billDetail : billHeader.getDetailList()) {
            //正常处理的生产数据
            List<ProductionCode> productionCodeList = productionCodeRepository.listByMaterialIdAndOrgId(billDetail.getMaterial().getId(), billHeader.getSrcId());
            codeList.addAll(productionCodeList);
            //生成明细的子明细
            createSubDetail(billDetail, productionCodeList);
            //更新编码的单据链
            renewCodeRoute(productionCodeList, billHeader);
            //创建单据编码
            dataList.addAll(createBillData(billHeader, billDetail, productionCodeList));
            billDetail.setRealQtyPcs(productionCodeList.size());
            if (billDetail.getMaterial().getPcsQty() != 0) {
                int stdQty = billDetail.getRealQtyPcs() / billDetail.getMaterial().getPcsQty();
                int minQty = billDetail.getRealQtyPcs() % billDetail.getMaterial().getPcsQty();
                BigDecimal detailRealQty = new BigDecimal(billDetail.getRealQtyPcs()).divide(new BigDecimal(billDetail.getMaterial().getPcsQty()), 2, 2);
                stdCount = stdCount + stdQty;
                minCount = minCount + minQty;
                realQty = realQty.add(detailRealQty);
                billDetail.setRealQtyNew(detailRealQty);
                billDetail.setRealQtyStr(excelUtilAppService.concatQtyStr(stdQty, minQty, stdStr, minStr));
            } else {
                billDetail.setRealQtyStr("0");
            }
        }
        billHeader.setRealQtyPcs(billHeader.getDetailList().stream().mapToInt(BillDetail::getRealQtyPcs).sum());
        billHeader.setRealQtyStr(excelUtilAppService.concatQtyStr(stdCount, minCount, stdStr, minStr));
        billHeader.setRealQty(realQty);
        billHeader.setBillStatus(BillStatus.Audited.index);
        billHeader.setEditBy(userName);
        billHeader.setEditTime(new Date());
        billHeader.setAuditBy(userName);
        billHeader.setAuditTime(new Date());
        billDataRepository.bulk(dataList);
        productionCodeRepository.save(codeList);
        projBillHeaderRepository.save(billHeader);
        //创建推送YS记录
        APIResult<String> apiResult = billPushToYsRecordAppService.create(billHeader.getBillTypeId(), billHeader.getId(), billHeader.getCode());
        logger.info("创建推送到YS的记录：billType=\"{}\",billHeader.getCode()=\"{}\", 结果,record=\"{}\"", billHeader.getBillTypeId(), billHeader.getCode(), JSON.toJSONString(apiResult));
        return result.succeed();
    }

    /**
     * 生成明细的子明细
     *
     * @param billDetail         明细
     * @param productionCodeList 生产数据
     */
    private void createSubDetail(BillDetail billDetail, List<ProductionCode> productionCodeList) {
        //生成明细的子明细
        Map<Triple<String, Date, Date>, List<ProductionCode>> subDetailGroups = productionCodeList.stream().collect(Collectors.groupingBy(x -> Triple.of(x.getBatchCode(), x.getPackDate(), x.getValidDate())));
        for (Map.Entry<Triple<String, Date, Date>, List<ProductionCode>> subDetailGroup : subDetailGroups.entrySet()) {
            BillSubDetail billSubDetail = new BillSubDetail();
            billSubDetail.setId(UUID.randomUUID().toString());
            billSubDetail.setMaterial(billDetail.getMaterial());
            billSubDetail.setBatchCode(subDetailGroup.getKey().getLeft());
            billSubDetail.setPackDate(subDetailGroup.getKey().getMiddle());
            billSubDetail.setValidDate(subDetailGroup.getKey().getRight());
            billSubDetail.setRealQtyPcs(subDetailGroup.getValue().size());
            billSubDetail.setAddTime(new Date());
            billSubDetail.setEditTime(new Date());
            billSubDetail.setParent(billDetail);
            billSubDetail.setAttr1(subDetailGroup.getValue().get(0).getAttr1());
            billDetail.getSubList().add(billSubDetail);
        }
    }

    /**
     * 更新编码的单据链
     *
     * @param productionCodeList 编码
     * @param billHeader         单据
     */
    private void renewCodeRoute(List<ProductionCode> productionCodeList, BillHeader billHeader) {
        for (ProductionCode productionCode : productionCodeList) {
            String oldRoute = productionCode.getRoute();
            String newRoute = billHeader.getId() + "," + billHeader.getBillTypeId();
            if (StringUtils.isNotEmpty(oldRoute)) {
                newRoute = oldRoute + ";" + newRoute;
            }
            productionCode.setRoute(newRoute);
            productionCode.setShouldOrgId(billHeader.getDestId());
        }
    }

    /**
     * 创建单据编码
     *
     * @param billHeader         单据
     * @param billDetail         单据明细
     * @param productionCodeList 生产数据
     * @return 单据编码
     */
    private List<BillData> createBillData(BillHeader billHeader, BillDetail billDetail, List<ProductionCode> productionCodeList) {
        List<BillData> dataList = new ArrayList<>();
        for (ProductionCode productionCode : productionCodeList) {
            BillData billData = new BillData();
            billData.setId(UUID.randomUUID().toString());
            billData.setProductionCodeId(productionCode.getId());
            billData.setBatchCode(productionCode.getBatchCode());
            billData.setValidDate(productionCode.getValidDate());
            billData.setDetailId(billDetail.getId());
            billData.setHeaderId(billHeader.getId());
            billData.setPackDate(productionCode.getPackDate());
            billData.setScanCode(productionCode.getId());
            billData.setScanTime(new Date());
            billData.setMaterialId(billDetail.getMaterial().getId());
            dataList.add(billData);
        }
        return dataList;
    }
}
