package com.proj.repository;

import com.arlen.ebd.dto.MaterialDTO;
import com.arlen.ebp.dto.SysGeoCityDTO;
import com.arlen.ebp.dto.SysStructureDTO;
import com.arlen.ebt.entity.BillData;
import com.arlen.ebt.entity.STBillData;

import java.util.Date;
import java.util.List;

/**
 * Created by arlenChen on 2019/12/18.
 * 对外数据
 *
 * @author arlenChen
 */
public interface WonderSynOpenRepository {

    /**
     * 获取需要同步中间表的产品数据
     *
     * @param lastSyncTime 最后同步时间
     * @return 数据
     */
    List<MaterialDTO> getSyncMaterialList(String lastSyncTime);

    /**
     * 获取需要同步中间表的地理城市
     *
     * @param lastSyncTime 最后同步时间
     * @return 数据
     */
    List<SysGeoCityDTO> getSyncGeoCityList(String lastSyncTime);

    /**
     * 获取需要同步中间表的考核城市
     *
     * @param lastSyncTime 最后同步时间
     * @return 数据
     */
    List<SysStructureDTO> getSyncCheckCityList(String lastSyncTime);

    /**
     * 获取单据编码
     *
     * @param headerId
     * @return
     */
    List<BillData> getBillData(String headerId);

    /**
     * 获取盘点单编码
     *
     * @param headerId
     * @return
     */
    List<STBillData> getSTBillData(String headerId);
}
