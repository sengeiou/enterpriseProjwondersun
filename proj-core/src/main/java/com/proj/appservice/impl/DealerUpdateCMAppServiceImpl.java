package com.proj.appservice.impl;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebd.biz.excel.CommonReadExcelBase;
import com.arlen.ebd.biz.excel.ReadParseExcelFactory;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebp.entity.Organization;
import com.arlen.ebp.entity.SysPara;
import com.arlen.ebp.enums.OrgType;
import com.arlen.ebp.repository.OrganizationRepository;
import com.arlen.ebp.repository.SysParaRepository;
import com.arlen.ebp.util.EbpConstantUtils;
import com.proj.appservice.DealerUpdateCMAppService;
import com.proj.appservice.ExcelUtilAppService;
import com.proj.biz.BeanValuePartForNull;
import com.proj.biz.ImportDealerUpdateCMProcessor;
import com.proj.dto.ImportStoreDealerRelationDTO;
import com.mysql.jdbc.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**导入经销商更新客户经理
 * Created by Robert on 2016/9/30.
 */
@Service
public class DealerUpdateCMAppServiceImpl implements DealerUpdateCMAppService {
    private static Logger logger = LoggerFactory.getLogger(DealerUpdateCMAppServiceImpl.class);
    @Resource
    private OrganizationRepository organizationRepository;
    @Resource
    private ExcelUtilAppService excelUtilAppService;
    @Resource
    private SysParaRepository sysParaRepository;

    @Override
    public APIResult<List<ImportStoreDealerRelationDTO>> importExcel(InputStream inputStream, String fileType, SysUserDTO currentUser) {
        logger.info("经销商更新客户经理开始导入");
        long st=System.currentTimeMillis();
        APIResult<List<ImportStoreDealerRelationDTO>> ret =new APIResult<>();
        ret.setCode(0);//默认处理成功
        final String[] SAFE_SUFFIX = { "xls", "xlsx" };
        List<ImportStoreDealerRelationDTO> list = null;
        CommonReadExcelBase<ImportStoreDealerRelationDTO> crpe = null;
        try {
            crpe = ReadParseExcelFactory.createProcessor(ImportDealerUpdateCMProcessor.class);
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
        long curt=System.currentTimeMillis();
        long et=curt-st;
        logger.info("读入消耗秒数-------"+et/1000);
        //校验通过就入库
        if(checkExcelContent(list)){
            long tt=System.currentTimeMillis();
            et=tt-curt;
            logger.info("校验消耗秒数-------"+et/1000);
            importExcelSave(list,currentUser);
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
    private boolean checkExcelContent(List<ImportStoreDealerRelationDTO> list){
        int errorCount=0;
        int rowNum=2;
        Set<String> duplicateCodeSet=new HashSet<>();
        for(ImportStoreDealerRelationDTO bean:list){
            errorCount = getEmptyErrorCount(rowNum,errorCount, bean,"经销商代码",bean.getDealerCode(),"不应为空");
            String tmpStr=bean.getDealerCode();
            if(!duplicateCodeSet.contains(tmpStr)) {
                duplicateCodeSet.add(tmpStr);
            }else {
                errorCount = getError(rowNum, errorCount, bean, "经销商代码", tmpStr, "存在重复行");
            }
            //空格
            if(bean.getDealerCode().trim().length()!=bean.getDealerCode().length()) {
                errorCount = getError(rowNum, errorCount, bean, "经销商代码", bean.getDealerCode(), "前面或后面都可能存在空格");
            }
            //空格
            if(bean.getMainManager().trim().length()!=bean.getMainManager().length()) {
                errorCount = getError(rowNum, errorCount, bean, "客户经理", bean.getMainManager(), "前面或后面都可能存在空格");
            }
            Organization dealer = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER.index);
            if(dealer==null){
               errorCount = getError(rowNum,errorCount, bean,"经销商代码",bean.getDealerCode(),"系统里还不存在");
            }
            rowNum++;
        }
        return ((errorCount>0)?false:true);
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

    /**
     * 保存数据
     * @param list
     * @param currentUser
     */
    @Override
    public void importExcelSave(List<ImportStoreDealerRelationDTO> list,SysUserDTO currentUser){
        SysPara sysPara = sysParaRepository.load("createPushRecord");
        boolean isCreatePushRecord = sysPara != null && EbpConstantUtils.TRUE.equals(sysPara.getValue()) &&
                cn.jiguang.common.utils.StringUtils.isNotEmpty(sysPara.getRemark()) && sysPara.getRemark().contains(OrgType.DEALER.index + EbpConstantUtils.COMMA);
        for(ImportStoreDealerRelationDTO bean:list){
            Organization dealer = organizationRepository.getByCode(bean.getDealerCode(), OrgType.DEALER.index);
            dealer.setExt5(bean.getMainManager());
            dealer.setPhone(bean.getPhone());
            dealer.setEditBy(currentUser.getUserName());
            dealer.setEditTime(new Date());
            if (isCreatePushRecord) {
                excelUtilAppService.createPushRecord(dealer,false);
            }
            //organizationRepository.save(dealer);
        }
    }
}