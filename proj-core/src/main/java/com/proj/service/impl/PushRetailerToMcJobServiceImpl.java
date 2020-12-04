package com.proj.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebms.utils.CommonUtil;
import com.arlen.ebms.utils.DateTimeTool;
import com.arlen.ebms.utils.ProductionUtil;
import com.arlen.ebp.appservice.OrganizationAppService;
import com.arlen.ebp.appservice.SysForeignPushRecordService;
import com.arlen.ebp.dto.OrganizationDTO;
import com.arlen.ebp.dto.SysForeignPushRecordDTO;
import com.arlen.ebp.entity.SimpleData;
import com.arlen.ebp.entity.SupplyRelation;
import com.arlen.ebp.entity.SysForeignPushRecord;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.SimpleDataRepository;
import com.arlen.ebp.repository.SupplyRelationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebu.utils.StringUtil;
import com.proj.dto.ClientJsonForMC;
import com.proj.service.PushRetailerToMcJobService;
import com.arlen.utils.common.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */
@Service
public class PushRetailerToMcJobServiceImpl implements PushRetailerToMcJobService {

    private static Logger logger = LoggerFactory.getLogger(PushRetailerToMcJobServiceImpl.class);

    private static final String METHOD_YSIF_RETAILERADD = "YSIF.RetailerAdd";

    private static final String YMETHOD_SIF_RETAILERUPDATE = "YSIF.RetailerUpdate";

    @Resource
    private SysForeignPushRecordService sysForeignPushRecordService;

    @Resource
    private OrganizationAppService organizationAppService;

    @Resource
    private SysParaRepository sysParaRepository;

    @Resource
    private SupplyRelationRepository supplyRelationRepository;

    @Resource
    private SimpleDataRepository simpleDataRepository;

    /**
     * 执行推送
     *
     * @param dateType 3-仓库 4-经销商 5-分销商 6-门店 enum OrgType.java getIndex
     * @param clientType 1-公司仓库 2-经销商 3-终端门店 4-分销商 10-其他
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> doPush(String dateType, String clientType) {
        Map<String, Object> resMap = new HashMap<>();
        logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送"+dateType+"信息，开始：", DateTimeTool.getCurrentTime());
        Date logStartTime = DateTimeTool.getCurDatetime();//计算耗时，开始时间
        String lastExecTime = DateTimeTool.getCurrentTime();//执行时间
        //拼装返回信息
        String msg = "";
        int total = 0;
        int i = 0;
        int success = 0;
        int fail = 0;
        int error = 0;
        try {
            //查询待推送的单据
            //循环处理待推送记录
            List<SysForeignPushRecord> waitPushList = new ArrayList<>();
            if (CommonUtil.isNull(dateType)) {
                throw new RuntimeException("dateType is null");
            }
            int size = 10;
            SysPara sysPara = sysParaRepository.load("pushBaseToMcSize");
            if (sysPara != null && StringUtils.isNotEmpty(sysPara.getValue())) {
                size = Integer.parseInt(sysPara.getValue());
            }
            APIResult<List<SysForeignPushRecord>> pushDataResult = sysForeignPushRecordService.getListBySize(dateType, "0", size);
            if (pushDataResult.getCode() == 0 && pushDataResult.getData() != null) {
                waitPushList = pushDataResult.getData();
            }
            total = waitPushList.size();
            logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType.OrgType."+dateType+"信息，待推送单据数量：[{}]", DateTimeTool.getCurrentTime(), total);

            logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，循环处理待推送记录", DateTimeTool.getCurrentTime());


            for (SysForeignPushRecord sysForeignPushRecord : waitPushList) {
                logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，循环到【" + i + "】条数据", DateTimeTool.getCurrentTime());
                logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，构造DTO", DateTimeTool.getCurrentTime());
                APIResult<SysForeignPushRecordDTO> sysForeignPushRecordDTOAPIResult = this.createDto(sysForeignPushRecord, clientType);
                if (sysForeignPushRecordDTOAPIResult.getCode() != 0) {
                    logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，构造DTO出现异常，异常原因：{}", DateTimeTool.getCurrentTime(), sysForeignPushRecordDTOAPIResult.getErrMsg());
                    fail++;
                    continue;
                }
                SysForeignPushRecordDTO sysForeignPushRecordDTO = sysForeignPushRecordDTOAPIResult.getData();
                try {
                    logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，添加请求日志", DateTimeTool.getCurrentTime());
                    APIResult<SysForeignPushRecord> udpateReqInfoResult = sysForeignPushRecordService.udpateReqInfo(sysForeignPushRecordDTO);
                    if (udpateReqInfoResult.getCode() != 0) {
                        logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，添加请求日志出现异常，异常原因：{}", DateTimeTool.getCurrentTime(), udpateReqInfoResult.getErrMsg());
                        fail++;
                        continue;
                    }
                    logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，OrgType."+dateType+"接口请求", DateTimeTool.getCurrentTime());
                    String xmlStr = this.pushToMC(sysForeignPushRecordDTO);
                    logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，OrgType."+dateType+"接口返回，返回数据：{}", DateTimeTool.getCurrentTime(), xmlStr);
                    logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，添加响应日志", DateTimeTool.getCurrentTime());
                    // 转换xml
                    Document document = DocumentHelper.parseText(xmlStr);
                    Node jsonStrNode = document.getRootElement();
                    // 转换json
                    String jsonStr = jsonStrNode.getText();
                    Map<String, Object> poMap = JSONObject.parseObject(jsonStr);
                    Integer seq = (Integer) poMap.get("Sequence");
                    Integer code = (Integer) poMap.get("Return");
                    String returnInfo = (String) poMap.get("ReturnInfo");
                    String compressFlag = (String) poMap.get("CompressFlag");
                    String contentJsonStr = (String) poMap.get("Result");
                    Map<String, Object> contentMap = JSONObject.parseObject(contentJsonStr);
                    // 更新调用信息 大于等于0成功 小于0	错误
                    if (code >= 0) {
                        sysForeignPushRecordDTO.setPushStatus("2");
                        success++;
                    } else {
                        sysForeignPushRecordDTO.setPushStatus("3");
                        fail++;
                    }
                    sysForeignPushRecordDTO.setEndWorkTime(new Date());
                    sysForeignPushRecordDTO.setResMsg(xmlStr);
                    APIResult<SysForeignPushRecord> udpateResInfoResult = sysForeignPushRecordService.udpateResInfo(sysForeignPushRecordDTO);
                    if (udpateResInfoResult.getCode() != 0) {
                        logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，添加响应日志出现异常，异常原因：{}", DateTimeTool.getCurrentTime(), udpateResInfoResult.getErrMsg());
                        fail++;
                        continue;
                    }
                    logger.warn(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，成功。PushRecord.Id={}", DateTimeTool.getCurrentTime(), sysForeignPushRecord.getId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String errMsg = StringUtil.subStringByMax(ex.toString(), 200);
                    sysForeignPushRecordDTO.setEndWorkTime(new Date());
                    sysForeignPushRecordDTO.setErrMsg(errMsg);
                    sysForeignPushRecordDTO.setPushStatus("9");
                    sysForeignPushRecordService.udpateErrorInfo(sysForeignPushRecordDTO);
                    logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，推送单据异常:[{}]，原因：{}", DateTimeTool.getCurrentTime(), sysForeignPushRecordDTO.getDataId(), errMsg);
                    logger.error("推送OrgType."+dateType+"信息，失败", ex);
                    error++;
                } finally {
                    i++;
                }
            }

            msg += "T" + total + "|";
            msg += "S" + success + "|";
            msg += "F" + fail + "|";
            msg += "E" + error + "|";

            logger.warn(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，完成。执行时间：{}，返回结果：{}", DateTimeTool.getCurrentTime(), lastExecTime, msg);
            resMap.put("msg",msg);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("推送OrgType."+dateType+"信息，失败", e);

            String errMsg = StringUtil.subStringByMax(e.toString(), 200);
            logger.warn(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，失败。执行时间：{}，失败原因", DateTimeTool.getCurrentTime(), lastExecTime, errMsg);
            resMap.put("msg",errMsg);
        } finally {
            Date logEndTime = DateTimeTool.getCurDatetime();//计算耗时，结束时间
            long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
            logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，消耗[{}]秒。", DateTimeTool.getCurrentTime(), interval);
            logger.info(">>>>>>>>>> PushRetailerToMcJobService.doPush >>>>>>>>>> {} 推送OrgType."+dateType+"信息，结束。", DateTimeTool.getCurrentTime());
        }
        return resMap;
    }

    /**
     * 门店
     *
     * dateType=6
     * clientType=3
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> pushStore() {
        Map<String, Object> resMap = new HashMap<>();
        String dateType = String.valueOf(OrgType.STORE.index);
        resMap = doPush(dateType, "3");
        return resMap;
    }

    /**
     * 经销商
     *
     * dateType=4
     * clientType=2
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> pushDealer() {
        Map<String, Object> resMap = new HashMap<>();
        String dateType = String.valueOf(OrgType.DEALER.index);
        resMap = doPush(dateType, "2");
        return resMap;
    }

    /**
     * 分销商
     *
     * dateType=5
     * clientType=4
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> pushDistributor() {
        Map<String, Object> resMap = new HashMap<>();
        String dateType = String.valueOf(OrgType.DISTRIBUTOR.index);
        resMap = doPush(dateType, "4");
        return resMap;
    }

    /**
     * 仓库
     *
     * dateType=3
     * clientType=1
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> pushWarehouse() {
        Map<String, Object> resMap = new HashMap<>();
        String dateType = String.valueOf(OrgType.RDC.index);
        resMap = doPush(dateType, "1");
        return resMap;
    }

    /**
     * 执行推送
     *
     * @param sysForeignPushRecordDTO
     * @return
     */
    @Override
    @Transactional
    public String pushToMC(SysForeignPushRecordDTO sysForeignPushRecordDTO) {
        //参数定义
        Date logStartTime = DateTimeTool.getCurDatetime();
        logger.info(">>>>>>>>>> pushToMC >>>>>>>>>> {} 执行推送，ID[{}]，开始。", DateTimeTool.getCurrentTime(), sysForeignPushRecordDTO.getDataId());
        //组织请求参数
        logger.info(">>>>>>>>>> pushToMC >>>>>>>>>> 执行推送，ID[{}]，发送请求，请求URL：{}，请求参数：{}，请求头信息：{}", sysForeignPushRecordDTO.getDataId(), sysForeignPushRecordDTO.getReqUrl(), sysForeignPushRecordDTO.getReqParam(), sysForeignPushRecordDTO.getReqOtherMsg());
        String url = sysForeignPushRecordDTO.getReqUrl();
        String param = sysForeignPushRecordDTO.getReqParam();
        String xmlStr = ProductionUtil.sendPost(url, param);
        logger.info(">>>>>>>>>> pushToMC >>>>>>>>>> 执行推送，ID[{}]，返回信息：", xmlStr);
        //计算耗时
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info(">>>>>>>>>> pushToMC >>>>>>>>>> {} 执行推送，消耗[{}]秒。", DateTimeTool.getCurrentTime(), interval);
        logger.info(">>>>>>>>>> pushToMC >>>>>>>>>> {} 执行推送，ID[{}]，结束。", DateTimeTool.getCurrentTime(), sysForeignPushRecordDTO.getDataId());
        return xmlStr;
    }

    /**
     * 创建要推送的DTO
     * @param sysForeignPushRecord
     * @param clientType
     * @return
     * @throws DocumentException
     */
    @Override
    @Transactional
    public APIResult<SysForeignPushRecordDTO> createDto(SysForeignPushRecord sysForeignPushRecord, String clientType) throws DocumentException {
        APIResult<SysForeignPushRecordDTO> result = new APIResult<>();
        // 获取数据
        String dataId = sysForeignPushRecord.getDataId();
        APIResult<OrganizationDTO> apiResult = organizationAppService.getById(dataId);
        if (0 != apiResult.getCode()) {
            return result.fail(apiResult.getCode(), apiResult.getErrMsg());
        }
        OrganizationDTO organizationDTO = apiResult.getData();
        SysForeignPushRecordDTO sysForeignPushRecordDTO = new SysForeignPushRecordDTO();
        BeanUtils.copyProperties(sysForeignPushRecord, sysForeignPushRecordDTO);
        sysForeignPushRecordDTO.setId(sysForeignPushRecord.getId());

        // create 新增 update 修改
        String PushProcessType = sysForeignPushRecord.getProcessType();
        // 封装请求参数
        ClientJsonForMC clientJsonForMC = createReqParam(organizationDTO, clientType);

        // 接口登陆获取AuthKey
        String authKey = getAuthKey();
        int sequence = getSequence();
        String xmlStr = "";
        String param = "";
        JSONObject clientJson = new JSONObject();
        JSONObject reqJson = JSONObject.parseObject(JSON.toJSONString(clientJsonForMC));
        clientJson.put("ClientJson", reqJson.toJSONString());
        String Params = clientJson.toJSONString();
        JSONObject reqPackJson = new JSONObject();
        String reqContent = "";
        reqPackJson = new JSONObject();
        reqPackJson.put("Sequence", sequence);//
        reqPackJson.put("AuthKey", authKey);//
        reqPackJson.put("DeviceCode", ""); //
        if ("create".equals(PushProcessType)) {
            reqPackJson.put("Method", METHOD_YSIF_RETAILERADD);
        }
        if ("update".equals(PushProcessType)) {
            reqPackJson.put("Method", YMETHOD_SIF_RETAILERUPDATE);
        }
        reqPackJson.put("Params", Params);
        reqContent = reqPackJson.toJSONString();
        param = "RequestPack=" + reqContent;

        sysForeignPushRecordDTO.setReqParam(param);
        SysPara sysPara_url = sysParaRepository.load("YSIF_URL");
        String urlStr = sysPara_url != null && StringUtils.isNotEmpty(sysPara_url.getValue()) ? sysPara_url.getValue() : "";
        if (StringUtils.isEmpty(urlStr)) {
            logger.error("YSIF_URL--->失败：接口地址未配置");
            return result.fail(403, "YSIF_URL--->失败：接口地址未配置");
        }

        //sysForeignPushRecordDTO.setReqUrl("http://wdsdev.meichis.com/YSIF/MCSWSIAPI.asmx/Call");
        sysForeignPushRecordDTO.setReqUrl(urlStr);
//        sysForeignPushRecordDTO.setReqOtherMsg(createReqOtherMsg().toJSONString());
        sysForeignPushRecordDTO.setPushTime(new Date());
        sysForeignPushRecordDTO.setBeginWorkTime(new Date());
        result.setCode(0);
        result.setData(sysForeignPushRecordDTO);
        return result;
    }

    /**
     * 获取默认值
     *
     * @param str
     * @param defval
     * @return
     */
    private String getDefaultValue(String str, String defval) {
        if (CommonUtil.isNull(str)) {
            str = defval;
        }
        return str;
    }

    /**
     * 封装请求参数
     *
     * @param org
     * @param clientType
     * @return
     */
    @Override
    @Transactional
    public ClientJsonForMC createReqParam(OrganizationDTO org, String clientType) {
        String DMSID = org.getId(); //	易溯唯一标识	String	必填
        String Code = org.getCode(); //	编码	String
        String FullName = this.getDefaultValue(org.getFullName(), org.getShortName()); //	全称	String	必填
        String ShortName = org.getShortName(); //	名称	String	必填
        String OfficialCity = this.getDefaultValue(org.getCityId(), "0"); //	所在行政城市	String
        String OfficialCityName = this.getDefaultValue(org.getCityName(), ""); //	所在行政城市名称	String
        String Address = this.getDefaultValue(org.getAddress(), "无" ); //	详细地址	String	必填
        String LinkManName = this.getDefaultValue(org.getContact(), "无"); //	联系人姓名	String
        String TeleNum = this.getDefaultValue(org.getPhone(), "无"); //	电话号码	String
        String Mobile = this.getDefaultValue(org.getPhone(), "无"); //	手机号码	String	必填
        String BusinessLicenseCode = ""; //	营业执照号码	String
        String ClientType = clientType; //	客户类型	Int	必填
        String Supplier = ""; //	上级ID	string
        String SupplierName = ""; //	上级名称	String
        // 分销商和门店，获取上级供货关系
        if (org.getOrgType() == 5 || org.getOrgType() == 6) {
            OrganizationDTO parentOrgDTO = getSupplier(org.getId());
            if (parentOrgDTO != null) {
                Supplier = parentOrgDTO.getId();
                SupplierName = parentOrgDTO.getShortName();
            }
        }
        String Salesman = ""; //	销售代表	string
        String SalesmanName = ""; //	销售代表名称	String
        String OrganizeCity = this.getDefaultValue(org.getCheckCityId(), "0"); //	管理片区	String
        String OrganizeCityName = ""; //	管理片区名称	String
        String ClientManager = ""; //	业务代表	String
        String ClientManagerName = ""; //	业务代表名称	String
        String ClientManagerMobile = ""; //	业务代表手机号	String
        String VestKeyAccount = ""; //	所属KA系统	string
        String VestKeyAccountName = ""; //	所属KA系统名称	String
        Float Latitude = 0F; //	维度	Float
        Float Longitude = 0F; //	经度	Float
        // 3 终端门店 才有销售代表等
        if ("3".equals(clientType)) {
            Salesman = this.getDefaultValue(org.getBak6(), ""); //	销售代表	string
            SalesmanName = this.getDefaultValue(org.getBak5(), ""); //	销售代表名称	String
            ClientManager = this.getDefaultValue(org.getBak10(), ""); //	业务专员	String
            ClientManagerName = this.getDefaultValue(org.getBak9(), ""); //	业务专员名称	String
            ClientManagerMobile = this.getDefaultValue(org.getBak10(), ""); //	业务专员手机号	String
            if (CommonUtil.isNotNull(org.getAttr2())) {
                VestKeyAccount = org.getAttr2(); //	所属KA系统	string
                SimpleData simpleData = simpleDataRepository.load(org.getAttr2());
                if (simpleData != null) {
                    VestKeyAccountName = this.getDefaultValue(simpleData.getName(), ""); //	所属KA系统名称	String
                }
            }
        }
        String DistrictManager = this.getDefaultValue(org.getBak7(),""); //	推广专员	String
        String DistrictManagerMobile = this.getDefaultValue(org.getBak8(),""); //	推广专员手机号	String
        String OfficeManager = this.getDefaultValue(org.getBak5(),""); //	动销专员	String
        String OfficeManagerMobile = this.getDefaultValue(org.getBak6(),""); //	动销专员手机号	String

        // ----------------------------------------------------------------
        ClientJsonForMC clientJsonForMC = new ClientJsonForMC();
        clientJsonForMC.setDMSID(DMSID); //	易溯唯一标识	String	必填
        clientJsonForMC.setCode(Code); //	编码	String
        clientJsonForMC.setFullName(FullName); //	全称	String	必填
        clientJsonForMC.setShortName(ShortName); //	名称	String	必填
        clientJsonForMC.setOfficialCity(OfficialCity); //	所在行政城市	String
        clientJsonForMC.setOfficialCityName(OfficialCityName); //	所在行政城市名称	String
        clientJsonForMC.setAddress(Address); //	详细地址	String	必填
        clientJsonForMC.setLinkManName(LinkManName); //	联系人姓名	String
        clientJsonForMC.setTeleNum(TeleNum); //	电话号码	String
        clientJsonForMC.setMobile(Mobile); //	手机号码	String	必填
        clientJsonForMC.setBusinessLicenseCode(BusinessLicenseCode); //	营业执照号码	String
        clientJsonForMC.setClientType(ClientType); //	客户类型	Int	必填
        clientJsonForMC.setSupplier(Supplier); //	上级ID	string
        clientJsonForMC.setSupplierName(SupplierName); //	上级名称	String
        clientJsonForMC.setSalesman(Salesman); //	销售代表	string
        clientJsonForMC.setSalesmanName(SalesmanName); //	销售代表名称	String
        clientJsonForMC.setOrganizeCity(OrganizeCity); //	管理片区	String
        clientJsonForMC.setOrganizeCityName(OrganizeCityName); //	管理片区名称	String
        clientJsonForMC.setClientManager(ClientManager); //	业务代表	String
        clientJsonForMC.setClientManagerName(ClientManagerName); //	业务代表名称	String
        clientJsonForMC.setClientManagerMobile(ClientManagerMobile); //	业务代表手机号	String
        clientJsonForMC.setDistrictManager(DistrictManager); //	推广专员	String
        clientJsonForMC.setDistrictManagerMobile(DistrictManagerMobile); //	推广专员手机号	String
        clientJsonForMC.setOfficeManager(OfficeManager); //	动销专员	String
        clientJsonForMC.setOfficeManagerMobile(OfficeManagerMobile); //	动销专员手机号	String
        clientJsonForMC.setVestKeyAccount(VestKeyAccount); //	所属KA系统	string
        clientJsonForMC.setVestKeyAccountName(VestKeyAccountName); //	所属KA系统名称	String
        clientJsonForMC.setLatitude(Latitude); //	维度	Float
        clientJsonForMC.setLongitude(Longitude); //	经度	Float
        // 返回
        return clientJsonForMC;
    }

    /**
     * 分销商和门店，获取上级供货关系
     *
     * @param childId
     * @return
     */
    @Transactional
    public OrganizationDTO getSupplier(String childId) {
        OrganizationDTO parentOrgDTO = null;
        if (CommonUtil.isNotNull(childId)) {
            SupplyRelation parentSupply = null;
            List<SupplyRelation> parentList = supplyRelationRepository.getParentList(childId);
            if (parentList != null && parentList.size() > 0) {
                for (SupplyRelation supplyRelation : parentList) {
                    if (supplyRelation.getIsUse()) {
                        parentSupply = supplyRelation;
                        break;
                    }
                }
            }
            if (parentSupply != null) {
                APIResult<OrganizationDTO> result = organizationAppService.getById(parentSupply.getParentId());
                if (result.getCode() == 0 && result.getData() != null) {
                    parentOrgDTO = result.getData();
                }
            }
        }
        return parentOrgDTO;
    }

    /**
     * 获取序列号
     *
     * @return
     */
    @Override
    @Transactional
    public int getSequence() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String seq_str = sdf.format(new Date()) + com.arlen.ebms.utils.StringUtil.getRandomNum(3);
        int seq = Integer.valueOf(seq_str);
//        System.out.println("getSequence:"+seq);
        return seq;
    }

    /**
     * 接口登陆获取AuthKey
     *
     * @return
     * @throws DocumentException
     */
    @Override
    @Transactional
    public String getAuthKey() throws DocumentException {
        String authKey = "";
        SysPara sysPara_url = sysParaRepository.load("YSIF_URL");
        String invoke_url = sysPara_url != null && StringUtils.isNotEmpty(sysPara_url.getValue()) ? sysPara_url.getValue() : "";
        if (StringUtils.isEmpty(invoke_url)) {
            logger.error("接口登陆获取AuthKey--->失败：接口地址未配置");
        }
        SysPara sysPara_userName = sysParaRepository.load("YSIF_UserName");
        String UserName = sysPara_userName != null && StringUtils.isNotEmpty(sysPara_userName.getValue()) ? sysPara_userName.getValue() : "";
        if (StringUtils.isEmpty(UserName)) {
            logger.error("接口登陆获取AuthKey--->失败：YSIF_UserName未配置");
        }
        SysPara sysPara_Password = sysParaRepository.load("YSIF_Password");
        String Password = sysPara_Password != null && StringUtils.isNotEmpty(sysPara_Password.getValue()) ? sysPara_Password.getValue() : "";
        if (StringUtils.isEmpty(Password)) {
            logger.error("接口登陆获取AuthKey--->失败：YSIF_Password未配置");
        }
        SysPara sysPara_DeviceCode = sysParaRepository.load("YSIF_DeviceCode");
        String DeviceCode = sysPara_DeviceCode != null && StringUtils.isNotEmpty(sysPara_DeviceCode.getValue()) ? sysPara_DeviceCode.getValue() : "";
        if (StringUtils.isEmpty(DeviceCode)) {
            logger.error("接口登陆获取AuthKey--->失败：YSIF_DeviceCode未配置");
        }
//        String invoke_url = "http://wdsdev.meichis.com/YSIF/MCSWSIAPI.asmx/Call";
        // 示例
        /*
        {
            "Sequence": 1,
            "AuthKey": "",
            "DeviceCode": "",
            "Method": "YSIF.Login",
            "Params": "{\"UserName\":\"Ehsure\",\"Password\":\"ehsure2019!\",\"DeviceCode\":\"ehsure\",\"ExtParams\":\"\"}"
        }
         */
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("Sequence", getSequence());
        jsonParam.put("AuthKey", "");
        jsonParam.put("DeviceCode", "");
        jsonParam.put("Method", "YSIF.Login");
        // 对方是json的字符串再一次转换成json的字符串 所以带 \
        String subJsonStr = "{\"UserName\":\""+UserName+"\",\"Password\":\""+Password+"\",\"DeviceCode\":\""+DeviceCode+"\",\"ExtParams\":\"\"}";
        jsonParam.put("Params", subJsonStr);
        // 参数
        String param = "RequestPack=" + jsonParam.toString();
        // 转换xml
        String xmlStr = ProductionUtil.sendPost(invoke_url, param);
        Document document = DocumentHelper.parseText(xmlStr);
        Node jsonStrNode = document.getRootElement();
        // 转换json
        String jsonStr = jsonStrNode.getText();
        Map<String, Object> poMap = JSONObject.parseObject(jsonStr);
        Integer seq = (Integer) poMap.get("Sequence");
        Integer code = (Integer) poMap.get("Return");
        String msg = (String) poMap.get("ReturnInfo");
        String compressFlag = (String) poMap.get("CompressFlag");
        String contentJsonStr = (String) poMap.get("Result");
        Map<String, Object> contentMap = JSONObject.parseObject(contentJsonStr);
        if (code >= 0) {
            authKey = (String) contentMap.get("AuthKey");
        }
        return authKey;
    }


}
