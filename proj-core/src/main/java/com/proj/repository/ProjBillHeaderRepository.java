package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.arlen.ebt.dto.BillDetailDTO;
import com.arlen.ebt.dto.BillHeaderDTO;
import com.arlen.ebt.entity.BillHeader;

import java.util.List;

/**
 * Created by arlenChen on 2019/07/19
 * 自定义单据
 *
 * @author arlenChen
 */
public interface ProjBillHeaderRepository extends BaseCrudRepository<BillHeader> {
    /**
     * 获取编码不存在异常的单据
     *
     * @return 单据
     */
    List<BillHeader> noCodeBillList();

    /**
     * 根据双方组织和单号获取数据
     *
     * @param srcCode  发货方
     * @param destCode 收货方
     * @param code     单号
     * @return 单据
     */
    List<BillHeader> getListByOrgAndCode(String srcCode, String destCode, String code);

    /**
     * 根据双方组织和扫码获取数据
     *
     * @param srcId    发货方
     * @param destId   收货方
     * @param scanCode 扫码
     * @return 单据
     */
    List<BillHeader> getListByOrgAndScanCode(String srcId, String destId, String scanCode);

    /**
     * 生成明细
     *
     * @param orgId 经销商
     * @return BillDetail
     */
    List<BillDetailDTO> generateBillDetailList(String orgId);

    /**
     * 根据主体Id获取对应的出库单
     *
     * @param orgId 主体Id
     * @return List
     */
    List<BillHeaderDTO> listByOrgId(String orgId);

    /**
     * 根据双方组织和单据类型获取数据
     *
     * @param srcId      发货方
     * @param destId     收货方
     * @param billTypeId 单据类型
     * @return List
     */
    List<BillHeader> getListByOrg(String srcId, String destId, String billTypeId);
}
