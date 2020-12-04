package com.proj.repository;

import com.proj.dto.ys.DeliveryCodeLibDTO;
import com.proj.dto.ys.DeliveryDTO;
import com.proj.dto.ys.DeliveryDetailDTO;
import com.proj.dto.ys.PushDeliveryDTO;

import java.util.List;

/**
 * 单据推送服务类
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */
public interface DeliveryRepository {

    void inserMcTable(PushDeliveryDTO pushDeliveryDTO);

    /**
     * 查询单据
     *
     * @param id
     * @return
     */
    int countById(String id);

    /**
     * 插入单据
     *
     * @param dto
     */
    void insertDelivery(DeliveryDTO dto);

    /**
     * 插入明细
     *
     * @param dto
     */
    void insertDeliveryDetail(DeliveryDetailDTO dto);

    /**
     * 插入编码
     *
     * @param dto
     */
    void insertDeliveryCodeLib(DeliveryCodeLibDTO dto);

    /**
     * 批量插入单据
     *
     * @param list
     */
    void batchInsertDelivery(List<DeliveryDTO> list);

    /**
     * 批量插入明细
     *
     * @param list
     */
    void batchInsertDeliveryDetail(List<DeliveryDetailDTO> list);

    /**
     * 批量插入编码
     *
     * @param list
     */
    void batchInsertDeliveryLib(List<DeliveryCodeLibDTO> list);


}
