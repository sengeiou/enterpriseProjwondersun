package com.proj.appservice.impl;

import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebd.biz.excel.CommonReadExcelBase;
import com.arlen.ebd.biz.excel.ReadParseExcelFactory;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SupplyRelation;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SupplyRelationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebp.util.EbpConstantUtils;
import com.proj.appservice.DealerD2RelationAppService;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.biz.BeanValuePartForNull;
import com.proj.biz.ImportDealerD2RelationProcessor;
import com.proj.dto.ImportStoreDealerRelationDTO;
import com.mysql.jdbc.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 导入经销商与分销商供货关系
 * Created by Robert on 2016/9/20.
 */
@Service
public class DealerD2RelationAppServiceImpl implements DealerD2RelationAppService {
    private static Logger logger = LoggerFactory.getLogger(DealerD2RelationAppServiceImpl.class);
    @Resource
    private OrganizationRepository organizationRepository;

    @Resource(name = "sysProperties")
    private Properties sysProperties;
    @Resource
    private SupplyRelationRepository supplyRelationRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private SysParaRepository sysParaRepository;

    @Override
    public APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser) {
        logger.info("分销商和门店开始导入");
        long st = System.currentTimeMillis();
        APIResult<List<ImportStoreDealerRelationDTO>> ret = new APIResult<>();
        ret.setCode(0);//默认处理成功
        final String[] SAFE_SUFFIX = {"xls", "xlsx"};
        List<ImportStoreDealerRelationDTO> list = null;
        CommonReadExcelBase<ImportStoreDealerRelationDTO> crpe = null;
        try {
            crpe = ReadParseExcelFactory.createProcessor(ImportDealerD2RelationProcessor.class);
            if (fileType.equals(SAFE_SUFFIX[0])) {
                list = crpe.readParseExcel2003Version(inputStream);
            } else if (fileType.equals(SAFE_SUFFIX[1])) {
                list = crpe.readParseExcel2007Version(inputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new APIResult<List<ImportStoreDealerRelationDTO>>().fail(404, "解析EXCEL出错:" + e.getMessage());
        } finally {
            crpe = null;
        }
        if (list.isEmpty()) {
            ret.setCode(-1);
            ret.setErrMsg("无数据处理");
            return ret;
        }
        ////
        Map<String, String> d2CodeMap = new HashMap<>();
        Map<String, String> dealerCodeSetMap = new HashMap<>();
        Set<String> relSet = new HashSet<>();//对应关系
        ////
        long curt = System.currentTimeMillis();
        long et = curt - st;
        logger.info("读入消耗秒数-------" + et / 1000);
        //校验通过就入库
        if (checkExcelContent(list, d2CodeMap, dealerCodeSetMap, relSet)) {
            long tt = System.currentTimeMillis();
            et = tt - curt;
            logger.info("校验消耗秒数-------" + et / 1000);
            importExcelSave(list, d2CodeMap, dealerCodeSetMap, relSet, currentUser);
            et = System.currentTimeMillis() - tt;
            logger.info("导入消耗秒数-------" + et / 1000);
        } else {
            ret.setCode(110);
            ret.setErrMsg("导入的数据格式有误");
            //过滤掉没有错误的行
            Set<String> reserveFiled = new HashSet<>();
            reserveFiled.add("errRowNum");
            reserveFiled.add("extAttr");
            list = list.stream().filter(e -> e.getErrRowNum() > 0).collect(Collectors.toList());
            list = list.stream().map(bean -> (ImportStoreDealerRelationDTO) BeanValuePartForNull.filterField(bean, reserveFiled)).collect(Collectors.toList());
            ret.attachData(list);
        }
        et = System.currentTimeMillis() - st;
        logger.info("消耗秒数-------" + et / 1000);
        return ret;
    }

    /**
     * 数据校验
     *
     * @param list
     * @return
     */
    private boolean checkExcelContent(List<ImportStoreDealerRelationDTO> list, Map<String, String> d2CodeMap, Map<String, String> dealerCodeSetMap, Set<String> relSet) {
        int errorCount = 0;
        int rowNum = 2;
        Set<String> duplicateCodeSet = new HashSet<>();
        for (ImportStoreDealerRelationDTO bean : list) {
            errorCount = getEmptyErrorCount(rowNum, errorCount, bean, "经销商代码", bean.getDealerCode(), "不应为空");
            errorCount = getEmptyErrorCount(rowNum, errorCount, bean, "分销商代码", bean.getD2Code(), "不应为空");
            String tmpStr = bean.getD2Code();
            if (!duplicateCodeSet.contains(tmpStr)) {
                duplicateCodeSet.add(tmpStr);
            } else {
                errorCount = getError(rowNum, errorCount, bean, "分销商代码", tmpStr, "存在重复行");
            }
            //空格
            if (bean.getDealerCode().trim().length() != bean.getDealerCode().length()) {
                errorCount = getError(rowNum, errorCount, bean, "经销商代码", bean.getDealerCode(), "前面或后面都可能存在空格");
            }
            //空格
            if (bean.getD2Code().trim().length() != bean.getD2Code().length()) {
                errorCount = getError(rowNum, errorCount, bean, "分销商代码", bean.getD2Code(), "前面或后面都可能存在空格");
            }
            //空格
            if (bean.getD2Name().trim().length() != bean.getD2Name().length()) {
                errorCount = getError(rowNum, errorCount, bean, "分销商名称", bean.getD2Name(), "前面或后面都可能存在空格");
            }
            Organization dealer = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER.index);
            if (dealer == null) {
                errorCount = getError(rowNum, errorCount, bean, "经销商代码", bean.getDealerCode(), "系统里还不存在");
            }
            Organization d2o = organizationRepository.getByCode(bean.getD2Code(), OrgType.DISTRIBUTOR.index);
            if (d2o == null) {
                errorCount = getError(rowNum, errorCount, bean, "分销商代码", bean.getD2Code(), "系统里还不存在");
            }
            if (errorCount == 0) {
                Organization d2 = organizationRepository.getByCode(bean.getD2Code(), OrgType.DISTRIBUTOR.index);//组织类型：0-总部;1-供应商;2-工厂;3-分发中心;4-经销商;5-分销商;6-门店
                if (d2 != null) {//分销商已存在
                    d2CodeMap.put(bean.getD2Code(), d2.getId());
                }
                //查询经销商
                String dealerId = null;
                Organization organization = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER);
                if (organization != null) {
                    dealerId = organization.getId();
                    dealerCodeSetMap.put(bean.getDealerCode(), dealerId);
                }
                if (d2 != null && dealerId != null) {//都在
                    SupplyRelation checkSupplyRelation = supplyRelationRepository.getSupplyRelation(dealerId, d2.getId());
                    if (checkSupplyRelation != null) {//关系在
                        relSet.add(dealerId + "," + d2.getId());
                    }
                }
            }
            rowNum++;
        }
        return ((errorCount > 0) ? false : true);
    }

    /**
     * 校验
     *
     * @param errorRowNum
     * @param errorCount
     * @param bean
     * @param desc
     * @param value
     * @return
     */
    private int getError(int errorRowNum, int errorCount, ImportStoreDealerRelationDTO bean, String desc, String value, String tip) {

        if (bean.getExtAttr() != null) {
            bean.setExtAttr(bean.getExtAttr() + ";" + desc + "[" + value + "]" + tip);
        } else {
            bean.setExtAttr(desc + "[" + value + "]" + tip);
        }
        errorCount += 1;
        bean.setErrRowNum(errorRowNum);
        return errorCount;
    }

    /**
     * 校验
     *
     * @param errorRowNum
     * @param errorCount
     * @param bean
     * @param desc
     * @param value
     * @return
     */
    private int getEmptyErrorCount(int errorRowNum, int errorCount, ImportStoreDealerRelationDTO bean, String desc, String value, String tip) {

        if (StringUtils.isEmptyOrWhitespaceOnly(value)) {
            if (bean.getExtAttr() != null) {
                bean.setExtAttr(bean.getExtAttr() + ";" + desc + "[" + value + "]" + tip);
            } else {
                bean.setExtAttr(desc + "[" + value + "]" + tip);
            }
            errorCount += 1;
            bean.setErrRowNum(errorRowNum);
        }
        return errorCount;
    }


    /**
     * 保存数据
     *
     * @param list
     * @param currentUser
     */
    @Override
    public void importExcelSave(List<ImportStoreDealerRelationDTO> list, Map<String, String> d2CodeMap, Map<String, String> dealerCodeSetMap, Set<String> relSet, SysUserDTO currentUser) {
        for (ImportStoreDealerRelationDTO bean : list) {
            if (!relSet.contains(dealerCodeSetMap.get(bean.getDealerCode()) + "," + d2CodeMap.get(bean.getD2Code())) && d2CodeMap.size() > 0 && dealerCodeSetMap.size() > 0) {
                //建立关系
                createRel(currentUser, bean.getD2Name(), bean.getD2Code(), d2CodeMap.get(bean.getD2Code()), dealerCodeSetMap.get(bean.getDealerCode()));
                relSet.add(dealerCodeSetMap.get(bean.getDealerCode()) + "," + d2CodeMap.get(bean.getD2Code()));
            }
        }
    }

    /**
     * 改变经销商或门店的禁用启用状态
     *
     * @param type 类型 1：启用;2：禁用
     * @param id   id
     * @return 结果
     */
    @Override
    public APIResult<String> changeOrganizationInUse(int type, String id, String userName) {
        APIResult<String> result = new APIResult<>();
        Organization organization = organizationRepository.load(id);
        if (organization == null) {
            return result.fail(404, "数据不存在");
        }
        switch (type) {
            case 1:
                if (organization.getInUse()) {
                    return result.fail(500, "该数据已启用");
                }
                organization.setInUse(true);
                break;
            case 2:
                if (!organization.getInUse()) {
                    return result.fail(500, "该数据已禁用");
                }
                organization.setInUse(false);
                break;
            default:
        }
        organization.setEditBy(userName);
        organization.setEditTime(new Date());
        SysPara sysPara = sysParaRepository.load("createPushRecord");
        boolean isCreatePushRecord = sysPara != null && EbpConstantUtils.TRUE.equals(sysPara.getValue()) &&
                cn.jiguang.common.utils.StringUtils.isNotEmpty(sysPara.getRemark()) && sysPara.getRemark().contains(organization.getOrgType() + EbpConstantUtils.COMMA);
        if (organization.getOrgType() == OrgType.STORE.index) {
            //门店禁用时，经用对应的供货关系
            List<SupplyRelation> relationList = supplyRelationRepository.getParentList(organization.getId());
            for (SupplyRelation supplyRelation : relationList) {
                supplyRelation.setIsUse(organization.getInUse());
                supplyRelation.setEditBy(userName);
                supplyRelation.setEditTime(new Date());
            }
            supplyRelationRepository.save(relationList);
        } else if (organization.getOrgType() == OrgType.DEALER.index) {
            //经销商禁用时，所有的下级供货关系全部禁用。
            List<SupplyRelation> childList = supplyRelationRepository.getChildLst(organization.getId());
            for (SupplyRelation supplyRelation : childList) {
                supplyRelation.setIsUse(organization.getInUse());
                supplyRelation.setEditBy(userName);
                supplyRelation.setEditTime(new Date());
                Organization childOrg = organizationRepository.load(supplyRelation.getChildId());
                if (childOrg != null&&isCreatePushRecord) {
                    excelUtilAppService.createPushRecord(childOrg, false);
                }
            }
            supplyRelationRepository.save(childList);
        }
        organizationRepository.save(organization);
        if (isCreatePushRecord) {
            excelUtilAppService.createPushRecord(organization, false);
        }
        if (!organization.getInUse()) {
            String msg = httpGet(id);
            logger.info("禁用门店时：关闭门店系统结果：\"{}\"", msg);
        }
        return result.succeed();
    }

    private String httpGet(String id) {
        JSONObject jsonObject = null;
        OutputStreamWriter out;
        StringBuilder buffer = new StringBuilder();
        try {
            String urlStr = sysProperties.getProperty("proj.ebmr.url", "");
            String pwd = sysProperties.getProperty("proj.ebmr.pwd", "");
            URL url = new URL(urlStr);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setDoOutput(true);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.setRequestProperty("content-type", "application/x-www-form-urlencoded");

            out = new OutputStreamWriter(httpUrlConn.getOutputStream(), "UTF-8");
            out.write("storeId=" + id + "&pwd=" + pwd);
            out.flush();
            out.close();
            InputStream inputStream = httpUrlConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            httpUrlConn.disconnect();
            jsonObject = JSONObject.parseObject(buffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonObject != null) {
            return jsonObject.toString();
        } else {
            return "";
        }
    }

    /**
     * 建立关联
     *
     * @param currentUser
     * @param customName
     * @param customCode
     * @param childId
     * @param parentId
     */
    private void createRel(SysUserDTO currentUser, String customName, String customCode, String childId, String parentId) {
        //建立关系
        SupplyRelation supplyRelation = new SupplyRelation();
        supplyRelation.setCustomName(customName);
        supplyRelation.setCustomCode(customCode);
        supplyRelation.setChildId(childId);
        supplyRelation.setParentId(parentId);
        supplyRelation.setAddBy(currentUser.getUserName());
        supplyRelation.setEditBy(currentUser.getUserName());
        supplyRelation.setAddTime(new Date());
        supplyRelation.setEditTime(supplyRelation.getAddTime());
        supplyRelation.setIsUse(Boolean.valueOf(true));
        supplyRelationRepository.insert(supplyRelation);
    }
}