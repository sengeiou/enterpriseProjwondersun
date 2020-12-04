package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebd.biz.excel.CommonReadExcelBase;
import com.arlen.ebd.biz.excel.ReadParseExcelFactory;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.*;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.*;
import com.arlen.ebp.util.EbpConstantUtils;
import com.arlen.ebu.appservice.IdKeyAppService;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.appservice.StoreDealerRelationAppService;
import com.proj.biz.BeanValuePartForNull;
import com.proj.biz.ImportStoreDealerRelationProcessor;
import com.proj.dto.ImportStoreDealerRelationDTO;
import com.arlen.utils.common.CommonUtil;
import com.mysql.jdbc.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**门店与经销商供货关系
 * Created by Robert on 2016/8/29.
 */
@Service
public class StoreDealerRelationAppServiceImpl implements StoreDealerRelationAppService {
    private static Logger logger = LoggerFactory.getLogger(StoreDealerRelationAppServiceImpl.class);
    @Resource
    private OrganizationRepository organizationRepository;

    @Resource
    private SupplyRelationRepository supplyRelationRepository;

    @Resource
    private SimpleDataRepository simpleDataRepository;

    @Resource
    private IdKeyAppService idKeyAppService;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private SysParaRepository sysParaRepository;

    @Override
    public APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser) {
        logger.info("经销商和门店开始导入");
        long st=System.currentTimeMillis();
        APIResult<List<ImportStoreDealerRelationDTO>> ret =new APIResult<>();
        //默认处理成功
        ret.setCode(0);
        final String[] SAFE_SUFFIX = { "xls", "xlsx" };
        List<ImportStoreDealerRelationDTO> list = null;
        CommonReadExcelBase<ImportStoreDealerRelationDTO> crpe = null;
        try {
            crpe = ReadParseExcelFactory.createProcessor(ImportStoreDealerRelationProcessor.class);
            if (fileType.equals(SAFE_SUFFIX[0])) {
                list = crpe.readParseExcel2003Version(inputStream);
            } else if (fileType.equals(SAFE_SUFFIX[1])) {
                list = crpe.readParseExcel2007Version(inputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new APIResult<List<ImportStoreDealerRelationDTO>>().fail(404, "解析EXCEL出错:"+e.getMessage());
        } finally {
            crpe = null;
        }
        if(list.isEmpty()){
            ret.setCode(-1);
            ret.setErrMsg("无数据处理");
            return ret;
        }
        ///////
        Set<String> dealerCodeSet=new HashSet<>();//经销商编码
        Map<String,String> dealerCodeMap=new HashMap<>();

        Set<String> storeCodeSet=new HashSet<>();//门店编码
        Map<String,String> storeCodeSetMap=new HashMap<>();
        //Map<String,Integer> dupMap=new HashMap<>();
        Set<String> relSet=new HashSet<>();//对应关系
        /////////
        long curt=System.currentTimeMillis();
        long et=curt-st;
        logger.info("读入消耗秒数-------"+et/1000);
        //校验通过就入库
        if(checkExcelContent(list, dealerCodeSet, dealerCodeMap, storeCodeSet, storeCodeSetMap, relSet)){
            long tt=System.currentTimeMillis();
            et=tt-curt;
            logger.info("校验消耗秒数-------"+et/1000);
            importExcelSave(list, dealerCodeSet, dealerCodeMap, storeCodeSet, storeCodeSetMap, relSet,currentUser);
            et=System.currentTimeMillis()-tt;
            logger.info("导入消耗秒数-------"+et/1000);
        }else{
            ret.setCode(110);
            ret.setErrMsg("导入的数据格式有误");
            //过滤掉没有错误的行
            Set<String> reserveFiled=new HashSet<>();
            reserveFiled.add("errRowNum");
            reserveFiled.add("extAttr");
            list=list.stream().filter(e->e.getErrRowNum()>0).collect(Collectors.toList());
            list=list.stream().map(bean-> (ImportStoreDealerRelationDTO) BeanValuePartForNull.filterField(bean,reserveFiled)).collect(Collectors.toList());
            ret.attachData(list);
        }
        et=System.currentTimeMillis()-st;
        logger.info("消耗秒数-------"+et/1000);
        return ret;
    }

    /**
     * 数据校验
     * @param list
     * @return
     */
    private boolean checkExcelContent(List<ImportStoreDealerRelationDTO> list,Set<String> dealerCodeSet,Map<String,String> dealerCodeMap,Set<String> storeCodeSet,Map<String,String> storeCodeSetMap,Set<String> relSet){
        int errorCount=0;
        int rowNum=2;
        //Set<String> duplicateStoreCodeSet=new HashSet<>();
        Set<String> duplicateStoreNameSet=new HashSet<>();
        try {
        for(ImportStoreDealerRelationDTO bean:list){
            //门店代码
          /*  errorCount = getEmptyErrorCount(rowNum,errorCount, bean,"门店代码",bean.getStoreCode(),"不应为空");

           if(!duplicateStoreCodeSet.contains(bean.getStoreCode())) {
               duplicateStoreCodeSet.add(bean.getStoreCode());
           } else {
               errorCount = getError(rowNum, errorCount, bean, "门店代码", bean.getStoreCode(), "与其他行存在重复");
           }
            //空格
            if(bean.getStoreCode().trim().length()!=bean.getStoreCode().length()) {
                errorCount = getError(rowNum, errorCount, bean, "门店代码", bean.getStoreCode(), "前面或后面都可能存在空格");
            }*/

           //门店名称
           errorCount = getEmptyErrorCount(rowNum,errorCount,bean,"门店名称",bean.getStoreName(),"不应为空");

            //空格
            if(bean.getStoreName().trim().length()!=bean.getStoreName().length()) {
                errorCount = getError(rowNum, errorCount, bean, "门店名称", bean.getStoreName(), "前面或后面都可能存在空格");
            }
            String str=bean.getStoreName();
           if(!duplicateStoreNameSet.contains(str)){
                duplicateStoreNameSet.add(str);
            } else {
                errorCount = getError(rowNum,errorCount, bean,"上级编码为"+ bean.getDealerCode()+"的门店名称",bean.getStoreName(),"EXCEL里存在重复");
            }
            //List<Organization> store = organizationRepository.getByShortName(bean.getStoreName(), OrgType.STORE);
            List<Organization> store =  organizationRepository.getByShortName(bean.getStoreName(), OrgType.STORE);
            if (store.size() > 0 && !store.get(0).getInUse()) {
                errorCount = getError(rowNum, errorCount, bean, "门店名称", bean.getStoreName(), "系统里已禁用");
            }
           /* if(store.size()>0){
                errorCount = getError(rowNum,errorCount, bean,"上级编码为"+ bean.getDealerCode()+"门店名称",bean.getStoreName(),"系统里已存在");
            }*/
            boolean isSupplyRelation=organizationRepository.isStoreExistedSupplyRelation(bean.getStoreName());
            if(store.size()>0 && isSupplyRelation){
                errorCount = getError(rowNum,errorCount, bean,"上级编码为"+ bean.getDealerCode()+"门店名称",bean.getStoreName(),"系统里已存在供货关系");
            }
            //空格
            if(bean.getDealerCode().trim().length()!=bean.getDealerCode().length()) {
                errorCount = getError(rowNum, errorCount, bean, "上级编码", bean.getDealerCode(), "前面或后面都可能存在空格");
            }
           Organization dealer = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER.index);
           if(dealer==null){
               errorCount = getError(rowNum,errorCount, bean,"上级编码",bean.getDealerCode(),"系统里还不存在");
           }
            if(errorCount==0) {
                Organization dealer2 = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER.index);//组织类型：0-总部;1-供应商;2-工厂;3-分发中心;4-经销商;5-分销商;6-门店
                if (dealer2 != null) {//经销商已存在
                    dealerCodeSet.add(bean.getDealerCode());
                    dealerCodeMap.put(bean.getDealerCode(), dealer2.getId());
                }
                //根据查询门店
                String storeId = null;
                //Organization organization = organizationRepository.getByCode(bean.getStoreCode(), OrgType.STORE);
                if (store.size()>0) {
                    Organization organization =store.get(0);
                    storeId = organization.getId();
                    bean.setStoreCode(organization.getCode());
                    storeCodeSet.add(organization.getCode());
                    storeCodeSetMap.put(organization.getCode(), storeId);
                }
                /*
                if (dealer2 != null && storeId != null) {//都在
                    SupplyRelation checkSupplyRelation = supplyRelationRepository.getSupplyRelation(dealer2.getId(), storeId);

                    if (checkSupplyRelation != null) {//关系在
                        relSet.add(dealer2.getId() + "," + storeId);
                    }
                }*/
            }

           rowNum++;
        }

        return ((errorCount>0)?false:true);
        }finally {
            //duplicateStoreCodeSet=null;
            duplicateStoreNameSet=null;
        }
    }

    /**
     * 校验
     * @param errorRowNum
     * @param errorCount
     * @param bean
     * @param desc
     * @param value
     * @return
     */
    private int getError(int errorRowNum,int errorCount, ImportStoreDealerRelationDTO bean,String desc,String value,String tip) {

        if(bean.getExtAttr()!=null){
            bean.setExtAttr(bean.getExtAttr()+";"+desc+"["+value+"]"+tip);
        }else{
            bean.setExtAttr(desc+"["+value+"]"+tip);
        }
        errorCount+=1;
        bean.setErrRowNum(errorRowNum);
        return errorCount;
    }

    /**
     * 校验
     * @param errorRowNum
     * @param errorCount
     * @param bean
     * @param desc
     * @param value
     * @return
     */
    private int getEmptyErrorCount(int errorRowNum,int errorCount, ImportStoreDealerRelationDTO bean,String desc,String value,String tip) {

        if(StringUtils.isEmptyOrWhitespaceOnly(value)){
            if(bean.getExtAttr()!=null){
                bean.setExtAttr(bean.getExtAttr()+";"+desc+"["+value+"]"+tip);
            }else{
                bean.setExtAttr(desc+"["+value+"]"+tip);
            }
            errorCount+=1;
            bean.setErrRowNum(errorRowNum);
        }
        return errorCount;
    }

    @Resource
    private SysGeoCityRepository sysGeoCityRepository;

    /**
     * 保存数据
     * @param list
     * @param dealerCodeSet
     * @param dealerCodeMap
     * @param storeCodeSet
     * @param storeCodeSetMap
     * @param relSet
     * @param currentUser
     */
    @Override
    public void importExcelSave(List<ImportStoreDealerRelationDTO> list,Set<String> dealerCodeSet,Map<String,String> dealerCodeMap,Set<String> storeCodeSet,Map<String,String> storeCodeSetMap,Set<String> relSet, SysUserDTO currentUser){
        SysPara sysPara = sysParaRepository.load("createPushRecord");
        for(ImportStoreDealerRelationDTO bean:list){
            if(dealerCodeSet.contains(bean.getDealerCode()) && !storeCodeSet.contains(bean.getStoreCode())){//仅门店不存在
                //创建门店
                String code = bean.getDealerCode().substring(0,4)+(String)this.idKeyAppService.generateOne("STORECODESUFFIX").getData();
                bean.setStoreCode(code);
                // 门店主代码未填写,主代码与副代码一致
                if(CommonUtil.isNull(bean.getMainCode())){
                    bean.setMainCode(code);
                }
                Organization newOrganization = createOrganization(currentUser, bean,OrgType.STORE,bean.getStoreCode());
                //推送记录
                boolean isCreatePushRecord = sysPara != null && EbpConstantUtils.TRUE.equals(sysPara.getValue()) &&
                        cn.jiguang.common.utils.StringUtils.isNotEmpty(sysPara.getRemark()) && sysPara.getRemark().contains(newOrganization.getOrgType() + EbpConstantUtils.COMMA);
                if (isCreatePushRecord) {
                    excelUtilAppService.createPushRecord(newOrganization, true);
                }
                storeCodeSet.add(bean.getStoreCode());
                storeCodeSetMap.put(bean.getStoreCode(),newOrganization.getId());
                createRel(currentUser, bean.getStoreName(),newOrganization.getCode(), newOrganization.getId(),dealerCodeMap.get(bean.getDealerCode()));
                relSet.add(dealerCodeMap.get(bean.getDealerCode()) + "," + newOrganization.getId());
                continue;

            }else if(dealerCodeSet.contains(bean.getDealerCode()) && storeCodeSet.contains(bean.getStoreCode())){//门店存在

                Organization dbOrg = organizationRepository.load(storeCodeSetMap.get(bean.getStoreCode()));
                //更新门店
                //updateOrganization(dbOrg,currentUser, bean);
                String dealerId=dealerCodeMap.get(bean.getDealerCode());
                if (!relSet.contains(dealerId + "," +dbOrg.getId())){
                    //建立关系
                    createRel(currentUser, bean.getStoreName(),dbOrg.getCode(), dbOrg.getId(),dealerId);
                    relSet.add(dealerId + "," +dbOrg.getId());
                    //推送记录
                    boolean isCreatePushRecord = sysPara != null && EbpConstantUtils.TRUE.equals(sysPara.getValue()) &&
                            cn.jiguang.common.utils.StringUtils.isNotEmpty(sysPara.getRemark()) && sysPara.getRemark().contains(dbOrg.getOrgType() + EbpConstantUtils.COMMA);
                    if (isCreatePushRecord) {
                        excelUtilAppService.createPushRecord(dbOrg, false);
                    }
                }
                continue;

            }else {

            }
        }
    }

    /**
     * 创建门店
     * @param currentUser
     * @param bean
     * @param orgType
     * @param code
     * @return
     */
    private Organization createOrganization(SysUserDTO currentUser, ImportStoreDealerRelationDTO bean,OrgType orgType,String code) {
        Organization newOrganization = new Organization();
        newOrganization.setAddress(bean.getAddress());//详细地址
        newOrganization.setContact(bean.getStoreOwner());//店主
        newOrganization.setPhone(bean.getTel());//联系电话
        newOrganization.setShortName(bean.getStoreName());//门店名称
        newOrganization.setMainCode(bean.getMainCode()); // 门店主代码
        newOrganization.setFullName(bean.getStoreName());
        newOrganization.setCode(code);//门店代码/
        newOrganization.setOrgType(orgType.index);//门店或经销商
        newOrganization.setProvinceId(getId(bean.getProvince(),1));//行政省
        newOrganization.setCityId(getId(bean.getCity(),2));//地级市
        newOrganization.setDistrictId(getId(bean.getDistrict(),3,newOrganization.getCityId()));//县/县级市/区
        newOrganization.setAddBy(currentUser.getUserName());
        newOrganization.setEditBy(currentUser.getUserName());
        newOrganization.setAddTime(new Date());
        newOrganization.setEditTime(newOrganization.getAddTime());
        newOrganization.setInUse(Boolean.valueOf(true));

        Organization dealer = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER.index);
        newOrganization.setCheckCityId(dealer.getCheckCityId()!=null?dealer.getCheckCityId():"999999");//默认城市 值为固定值 在查询门店列表时也用到
        newOrganization.setAttr1(getIdByName(bean.getChannelType(),"STORECHANNELTYPE"));//渠道类型
        newOrganization.setAttr3(getIdByName(bean.getSubChannel(),"STORESUBCHANNELTYPE"));//次渠道
        newOrganization.setAttr2(getIdByName(bean.getAttr3(),"STOREKATYPE"));//所属KA系统
        newOrganization.setExt10(bean.getTown());//乡镇

        newOrganization.setExt1(bean.getProportion());//营业面积
//        newOrganization.setExt2(bean.getOperator());//业务员
//        newOrganization.setExt3(bean.getOperatorTel());//业务员电话
        newOrganization.setExt4(bean.getCooperationTime());//合作时间
//        newOrganization.setExt5(bean.getMainManager());//负责客户经理
//        newOrganization.setExt6(bean.getMainManagerTel());//负责客户经理联系方式
        newOrganization.setExt7(bean.getMainKASpec());//负责KA专员（KA门店填写）
        newOrganization.setExt8(bean.getMainKASpecTel());//负责KA专员联系方式（KA门店填写）
        newOrganization.setExt9(bean.getFixVisitTime());//固定拜访时间

//        newOrganization.setBak3(bean.getBak3());//业务代表
//        newOrganization.setBak4(bean.getBak4());//业务代表电话
        newOrganization.setBak5(bean.getBak5());//动销专员
        newOrganization.setBak6(bean.getBak6());//动销专员电话
        newOrganization.setBak7(bean.getBak7());//推广专员
        newOrganization.setBak8(bean.getBak8());//推广专员电话
        newOrganization.setBak9(bean.getBak9());//业务专员
        newOrganization.setBak10(bean.getBak10());//业务专员电话

        newOrganization.setBak11(bean.getBak11());//区域推广经理
        newOrganization.setBak12(bean.getBak12());//区域推广经理电话
        newOrganization.setBak13(bean.getBak13());//营养代表
        newOrganization.setBak14(bean.getBak14());//营养代表电话

        newOrganization.setBak1(bean.getBak1());//区域经理
        newOrganization.setBak2(bean.getBak2());//区域经理电话

        organizationRepository.insert(newOrganization);
        return newOrganization;
    }

    /**
     * 更新门店
     * @param organization
     * @param currentUser
     * @param bean
     * @return
     */
    private Organization updateOrganization( Organization organization,SysUserDTO currentUser, ImportStoreDealerRelationDTO bean) {
        organization.setAddress(bean.getAddress());//详细地址
        organization.setContact(bean.getStoreOwner());//店主
        organization.setPhone(bean.getTel());//联系电话
        organization.setShortName(bean.getStoreName());//门店名称
        organization.setFullName(bean.getStoreName());
        //newOrganization.setOrgType(ot.index);//门店或经销商
        organization.setProvinceId(getId(bean.getProvince(),1));//行政省
        organization.setCityId(getId(bean.getCity(),2));//地级市
        organization.setDistrictId(getId(bean.getDistrict(),3,organization.getCityId()));//县/县级市/区
        organization.setAddBy(currentUser.getUserName());
        organization.setEditBy(currentUser.getUserName());
        organization.setEditTime(new Date());
        organization.setInUse(Boolean.valueOf(true));

        organization.setAttr1(getIdByName(bean.getChannelType(),"STORECHANNELTYPE"));//渠道类型
        organization.setAttr3(getIdByName(bean.getSubChannel(),"STORESUBCHANNELTYPE"));//次渠道
        organization.setAttr2(getIdByName(bean.getAttr3(),"STOREKATYPE"));//所属KA系统
        organization.setExt10(bean.getTown());//乡镇

        organization.setExt1(bean.getProportion());//营业面积
//        organization.setExt2(bean.getOperator());//业务员
//        organization.setExt3(bean.getOperatorTel());//业务员电话
        organization.setExt4(bean.getCooperationTime());//合作时间
//        organization.setExt5(bean.getMainManager());//负责客户经理
//        organization.setExt6(bean.getMainManagerTel());//负责客户经理联系方式
        organization.setExt7(bean.getMainKASpec());//负责KA专员（KA门店填写）
        organization.setExt8(bean.getMainKASpecTel());//负责KA专员联系方式（KA门店填写）
        organization.setExt9(bean.getFixVisitTime());//固定拜访时间

        organization.setBak11(bean.getBak11());//区域推广经理
        organization.setBak12(bean.getBak12());//区域推广经理电话
        organization.setBak13(bean.getBak13());//营养代表
        organization.setBak14(bean.getBak14());//营养代表电话

        organization.setBak1(bean.getBak1());//区域经理
        organization.setBak2(bean.getBak2());//区域经理电话

        organizationRepository.save(organization);
        return organization;
    }

    /**
     * 建立关联
     * @param currentUser
     * @param customName
     * @param customCode
     * @param childId
     * @param parentId
     */
    private void createRel(SysUserDTO currentUser, String customName, String customCode,String childId,String parentId) {
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

    /**
     * 据名称和级别查询主键
     * @param name
     * @param geoLevel
     * @return
     */
    private String getId(String name,int geoLevel){
        try {
            SysGeoCity sysGeoCity= sysGeoCityRepository.getIdByName(name,geoLevel);
            if(sysGeoCity!=null){
                return sysGeoCity.getId();
            }
        }catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * 据名称和级别、父Id查询主键
     * @param name
     * @param geoLevel
     * @param parentId
     * @return
     */
    private String getId(String name,int geoLevel,String parentId){//级别为3时有重复的数据所以加上父ID
        SysGeoCity sysGeoCity= sysGeoCityRepository.getIdByName(name,geoLevel,parentId);
        if(sysGeoCity!=null){
            return sysGeoCity.getId();
        }
        return null;
    }

    /**
     * 由名称查询代码
     * @param name
     * @param cid
     * @return
     */
    private String getIdByName(String name,String cid){
        if(name==null||name.trim().length()==0){
            return null;
        }
        List<SimpleData> sdList=simpleDataRepository.getListByCategoryId(cid);
        for(SimpleData sd:sdList){
            if(name.equals(sd.getName())){
                return sd.getId();
            }
        }
        return null;
    }
}