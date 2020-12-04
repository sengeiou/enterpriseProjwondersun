package com.proj.appservice;

import com.arlen.eaf.core.dto.APIResult;
import com.arlen.ebp.dto.SysUserDTO;
import com.arlen.ebt.dto.BillDetailDTO;

import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2017/5/16.
 * 其他出库单导入
 */
public interface ProjRdcToDealerOutAppService {
    /**
     * 验证导入的单据
     *
     * @param dataList dataList
     * @return APIResult
     */
    APIResult<Map<String, Object>> checkDataIsNormal(List<Map<String, String>> dataList, String importType, String orgId);

    /**
     * 根据出库单号分组并校验同一出库单号 的 发货方 和 收货方 是否一致
     *
     * @param dataList
     * @param importType
     * @param orgId
     * @return
     */
    APIResult<Map<String, Object>> groupByRefCodeDataAndCheck(List<Map<String, String>> dataList, String importType, String orgId);

    /**
     * 保存导入结果
     *
     * @param dataList 数据
     * @param userDTO  当前用户
     * @return APIResult
     * @throws Exception Exception
     */
    APIResult<String> saveData(List<Map<String, String>> dataList, SysUserDTO userDTO) throws Exception;

    /**
     * 保存分组导入结果
     *
     * @param groupMap
     * @param userDTO
     * @return
     * @throws Exception
     */
    APIResult<String> saveGroupData(Map<String, List<Map<String, String>>> groupMap, SysUserDTO userDTO) throws Exception;


    /**
     * 生成明细
     *
     * @param orgId 经销商
     * @return APIResult
     */
    APIResult<List<BillDetailDTO>> getExpectQtyPcsDetailList(String orgId);

    /**
     * 是否有在处理的单子
     *
     * @param orgId 经销商
     * @return boolean
     */
    boolean haveProcessingBill(String orgId);

    /**
     * 迁移单确认
     *
     * @param id       单据Id
     * @param userName 确认人
     * @return APIResult
     */
    APIResult<String> transferBillConfirm(String id, String userName);
}
