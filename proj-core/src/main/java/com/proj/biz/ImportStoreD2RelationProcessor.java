package com.proj.biz;

import com.ebd.biz.excel.CommonReadExcelBase;
import com.ebp.dto.SysUserDTO;
import com.proj.dto.ImportStoreDealerRelationDTO;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.text.DecimalFormat;

/**
 *分销商供货关系 POI读取Excel每行处理过程
 *
 */
public class ImportStoreD2RelationProcessor extends CommonReadExcelBase<ImportStoreDealerRelationDTO> {

    protected SysUserDTO getCurrentUser() {
        Subject subject = SecurityUtils.getSubject();
        return !subject.isAuthenticated()?null:(SysUserDTO)subject.getPrincipal();
    }

//    /**解析读到的每行数据存入对象中，可据情况格式化
//     * @param bean
//     * @param cellStr
//     * @param j
//     */
//    public void toBean(ImportStoreDealerRelationDTO bean, String cellStr, int j) {
//       if(j!=0&&j!=5) {
//           //cellStr = format(cellStr);
//       }
//       if (j == 0) {//行政省
//            bean.setProvince(cellStr);
//        } else if (j == 1) {//地级市
//            bean.setCity(cellStr);
//        } else if (j == 2) {//县/县级市/区
//            bean.setDistrict(cellStr);
//        } else if (j == 3) {//乡镇
//            bean.setTown(cellStr);
//        } else if (j == 4) {//门店名称
//            bean.setStoreName(cellStr);
//        } else if (j == 5) {//门店主代码
//            bean.setMainCode(cellStr);
//        } else if (j == 6) {//所属分销商编码
//            bean.setD2Code(cellStr);
//        }  else if (j == 7) {//渠道类型
//            bean.setChannelType(cellStr);
//        } else if (j == 8) {//所属KA系统
//            bean.setD2Name(cellStr);
//        } else if (j == 9) {//营业面积
//            bean.setProportion(cellStr);
//        } else if (j == 10) {//详细地址
//            bean.setAddress(cellStr);
//        } else if (j == 11) {//主联系人
//            bean.setStoreOwner(cellStr);
//        } else if (j == 12) {//主联系人电话
//            bean.setTel(cellStr);
//        } else if (j == 13) {//业务员
//            bean.setOperator(cellStr);
//        } else if (j == 14) {//业务员电话
//            bean.setOperatorTel(cellStr);
//        } else if (j == 15) {//合作时间
//            bean.setCooperationTime(cellStr);
//        } else if (j == 16) {//次渠道
//            bean.setSubChannel(cellStr);
//        } else if (j == 17) {//负责客户经理
//            bean.setMainManager(cellStr);
//        } else if (j == 18) {//负责客户经理联系方式
//            bean.setMainManagerTel(cellStr);
//        } else if (j == 19) {//负责KA专员 (KA门店填写）
//            bean.setMainKASpec(cellStr);
//        } else if (j == 20) {//负责KA专员联系方式（KA门店填写）
//            bean.setMainKASpecTel(cellStr);
//        } else if (j == 21) {//固定拜访时间
//            bean.setFixVisitTime(cellStr);
//        } else if (j == 22) {//业务代表
//           bean.setBak3(cellStr);
//        } else if (j == 23) {//业务代表电话
//           bean.setBak4(cellStr);
//        } else if (j == 24) {//动销专员
//           bean.setBak5(cellStr);
//        } else if (j == 25) {//动销专员电话
//           bean.setBak6(cellStr);
//        } else if (j == 26) {//业务专员
//           bean.setBak9(cellStr);
//        } else if (j == 27) {//业务专员电话
//           bean.setBak10(cellStr);
//        } else if (j == 28) {//推广专员
//           bean.setBak7(cellStr);
//        } else if (j == 29) {//推广专员电话
//           bean.setBak8(cellStr);
//        } else{
//
//        }
//    }

    /**解析读到的每行数据存入对象中，可据情况格式化
     * @param bean
     * @param cellStr
     * @param j
     */
    public void toBean(ImportStoreDealerRelationDTO bean, String cellStr, int j) {
        if(j!=0&&j!=5) {
            //cellStr = format(cellStr);
        }
        if (j == 0) {//行政省
            bean.setProvince(cellStr);
        } else if (j == 1) {//地级市
            bean.setCity(cellStr);
        } else if (j == 2) {//县/县级市/区
            bean.setDistrict(cellStr);
        } else if (j == 3) {//乡镇
            bean.setTown(cellStr);
        } else if (j == 4) {//门店名称
            bean.setStoreName(cellStr);
        } else if (j == 5) {//门店主代码
            bean.setMainCode(cellStr);
        } else if (j == 6) {//所属分销商编码
            bean.setD2Code(cellStr);
        }  else if (j == 7) {//渠道类型
            bean.setChannelType(cellStr);
        } else if (j == 8) {//所属KA系统
            bean.setD2Name(cellStr);
        } else if (j == 9) {//营业面积
            bean.setProportion(cellStr);
        } else if (j == 10) {//详细地址
            bean.setAddress(cellStr);
        } else if (j == 11) {//主联系人
            bean.setStoreOwner(cellStr);
        } else if (j == 12) {//主联系人电话
            bean.setTel(cellStr);
        } else if (j == 13) {//合作时间
            bean.setCooperationTime(cellStr);
        } else if (j == 14) {//次渠道
            bean.setSubChannel(cellStr);
        } else if (j == 15) {//负责KA专员 (KA门店填写）
            bean.setMainKASpec(cellStr);
        } else if (j == 16) {//负责KA专员联系方式（KA门店填写）
            bean.setMainKASpecTel(cellStr);
        } else if (j == 17) {//固定拜访时间
            bean.setFixVisitTime(cellStr);
        } else if (j == 18) {//区域经理
            bean.setBak1(cellStr);
        } else if (j == 19) {//区域经理电话
            bean.setBak2(cellStr);
        } else if (j == 20) {//区域推广经理
            bean.setBak11(cellStr);
        } else if (j == 21) {//区域推广经理电话
            bean.setBak12(cellStr);
        } else if (j == 22) {//动销专员
            bean.setBak5(cellStr);
        } else if (j == 23) {//动销专员电话
            bean.setBak6(cellStr);
        } else if (j == 24) {//业务专员
            bean.setBak9(cellStr);
        } else if (j == 25) {//业务专员电话
            bean.setBak10(cellStr);
        } else if (j == 26) {//推广专员
            bean.setBak7(cellStr);
        } else if (j == 27) {//推广专员电话
            bean.setBak8(cellStr);
        } else if (j == 28) {//营养代表
            bean.setBak13(cellStr);
        } else if (j == 29) {//营养代表电话
            bean.setBak14(cellStr);
        } else{

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