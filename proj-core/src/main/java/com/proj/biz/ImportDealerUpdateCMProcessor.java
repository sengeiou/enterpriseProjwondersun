package com.proj.biz;

import com.ebd.biz.excel.CommonReadExcelBase;
import com.ebp.dto.SysUserDTO;
import com.proj.dto.ImportStoreDealerRelationDTO;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.text.DecimalFormat;

/**
 *导入经销商更新客户经理 POI读取Excel每行处理过程
 *
 */
public class ImportDealerUpdateCMProcessor extends CommonReadExcelBase<ImportStoreDealerRelationDTO> {

    protected SysUserDTO getCurrentUser() {
        Subject subject = SecurityUtils.getSubject();
        return !subject.isAuthenticated()?null:(SysUserDTO)subject.getPrincipal();
    }

    /**解析读到的每行数据存入对象中，可据情况格式化
     * @param bean
     * @param cellStr
     * @param j
     */
    public void toBean(ImportStoreDealerRelationDTO bean, String cellStr, int j) {
       if(j!=0&&j!=2){
          // cellStr = format(cellStr);
       }
        if (j == 0) {//经销商代码
            bean.setDealerCode(cellStr);
        } else if (j == 1) {//经销商名称
            bean.setDealerName(cellStr);
        } else if (j == 2) {//客户经理
            bean.setMainManager(cellStr);
        } else if(j==3){//电话
            bean.setPhone(cellStr);
        }else{

        }
    }

    /**
     * 取整数
     * @param str
     * @return
     */
    private String format(String str){
        try{
            Double.parseDouble(str);
        }catch (Exception e){
            return str;//不是数字原样返回
        }
        Object inputValue = null;
        Long longVal = Math.round(Double.parseDouble(str));
        Double doubleVal = Double.parseDouble(str);
        if(Double.parseDouble(longVal + ".0") == doubleVal){//判断是否含有小数位.0
            inputValue = longVal;
        }
        else{
            inputValue = doubleVal;
        }
        DecimalFormat df = new DecimalFormat("#");//格式化；
        return String.valueOf(df.format(inputValue));//返回String类型
    }

}