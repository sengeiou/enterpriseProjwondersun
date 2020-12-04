package com.proj.repository;

import com.proj.dto.ys.CheckInventoryCodeLibDTO;
import com.proj.dto.ys.CheckInventoryDTO;
import com.proj.dto.ys.CheckInventoryDetailDTO;
import com.proj.dto.ys.PushCheckInventoryDTO;

import java.util.List;

/**
 * 盘点单推送服务类
 * Created by johnny on 2019/12/24.
 *
 * @author johnny
 */
public interface CheckInventoryRepository {

    void inserMcTable(PushCheckInventoryDTO pushCheckInventoryDTO);

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
    void insertCheckInventory(CheckInventoryDTO dto);

    /**
     * 批量插入单据
     *
     * @param list
     */
    void batchInsertCheckInventory(List<CheckInventoryDTO> list);

    /**
     * 插入明细
     *
     * @param dto
     */
    void insertCheckInventoryDetail(CheckInventoryDetailDTO dto);

    /**
     * 批量插入明细
     *
     * @param list
     */
    void batchInsertCheckInventoryDetail(List<CheckInventoryDetailDTO> list);

    /**
     * 插入编码
     *
     * @param dto
     */
    void insertCheckInventoryCodeLib(CheckInventoryCodeLibDTO dto);

    /**
     * 批量插入编码
     *
     * @param list
     */
    void batchInsertCheckInventoryCodeLib(List<CheckInventoryCodeLibDTO> list);
}
