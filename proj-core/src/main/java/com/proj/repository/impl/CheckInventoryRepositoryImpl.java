package com.proj.repository.impl;

import com.arlen.ebms.utils.DateTimeTool;
import com.proj.dto.ys.CheckInventoryCodeLibDTO;
import com.proj.dto.ys.CheckInventoryDTO;
import com.proj.dto.ys.CheckInventoryDetailDTO;
import com.proj.dto.ys.PushCheckInventoryDTO;
import com.proj.repository.CheckInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by johnny on 2019/12/24.
 *
 * @author johnny
 */
@Repository
public class CheckInventoryRepositoryImpl extends JdbcDaoSupport implements CheckInventoryRepository {

    private static Logger logger = LoggerFactory.getLogger(CheckInventoryRepositoryImpl.class);

    @Override
    public void inserMcTable(PushCheckInventoryDTO pushCheckInventoryDTO) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        PlatformTransactionManager ptm = new DataSourceTransactionManager(this.getDataSource());
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = ptm.getTransaction(def);
        try {
            this.insertCheckInventory(pushCheckInventoryDTO.getCheckInventory());
            this.batchInsertCheckInventoryDetail(pushCheckInventoryDTO.getCheckInventoryDetailList());
            this.batchInsertCheckInventoryCodeLib(pushCheckInventoryDTO.getCheckInventoryCodeLibList());
            ptm.commit(status);
        } catch (Exception e) {
            ptm.rollback(status);
            throw e;
        } finally {
            Date logEndTime = DateTimeTool.getCurDatetime();
            long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
            logger.info("  插入inserMcTable:id=" + pushCheckInventoryDTO.getCheckInventory().getId() + ",耗时：" + interval + "秒。");
        }
    }

    /**
     * 查询单据
     *
     * @param id
     * @return
     */
    @Override
    public int countById(String id) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "SELECT COUNT(*) FROM YS_CheckInventory WHERE Id = ?";
        Object args[] = new Object[]{id};
        int count = super.getJdbcTemplate().queryForObject(sql, args, Integer.class);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  查询YS_CheckInventory:id=" + id + ",count=" + count + ",耗时：" + interval + "秒。");
        return count;
    }

    /**
     * 插入盘点单据
     *
     * @param dto
     */
    @Override
    public void insertCheckInventory(CheckInventoryDTO dto) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_CheckInventory(id, addtime, edittime, audittime, billstatus, code, orgid, orgwareid, processstatus) \n" +
                "values (?, ?, ?, ?, ?, ?, ?, ?,?)";
        Object[] objects = buildCheckInventoryObjects(dto);
        int update = super.getJdbcTemplate().update(sql, objects);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  插入YS_CheckInventory:" + update + ",耗时：" + interval + "秒。");
    }

    /**
     * 批量插入盘点单据
     *
     * @param list
     */
    @Override
    public void batchInsertCheckInventory(List<CheckInventoryDTO> list) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_CheckInventory(id, addtime, edittime, audittime, billstatus, code, orgid, orgwareid, processstatus)\n" +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        for (CheckInventoryDTO dto : list) {
            Object[] objects = buildCheckInventoryObjects(dto);
            batchArgs.add(objects);
        }
        int[] ints = super.getJdbcTemplate().batchUpdate(sql, batchArgs);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  插入YS_CheckInventory:" + ints.length + ",耗时：" + interval + "秒。");
    }

    /**
     * 构造单据Objs
     *
     * @param dto
     * @return
     */
    private Object[] buildCheckInventoryObjects(CheckInventoryDTO dto) {
        return new Object[]{dto.getId(), dto.getAddTime(), dto.getEditTime(), dto.getAuditTime(), dto.getBillStatus(), dto.getCode(), dto.getOrgId(), dto.getOrgWareId(), dto.getProcessStatus()};
    }

    /**
     * 插入盘点明细
     *
     * @param dto
     */
    @Override
    public void insertCheckInventoryDetail(CheckInventoryDetailDTO dto) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "insert into YS_CheckInventoryDetail(id, addtime, edittime, expectqtypcs, materialid, realqtypcs2, headerid)\n" +
                "values (?, ?, ?, ?, ?, ?, ?)";
        Object[] objects = buildCheckInventoryDetailObjects(dto);
        int update = super.getJdbcTemplate().update(sql, objects);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  插入YS_CheckInventoryDetail:" + update + ",耗时：" + interval + "秒。");
    }

    /**
     * 批量插入盘点单明细
     *
     * @param list
     */
    @Override
    public void batchInsertCheckInventoryDetail(List<CheckInventoryDetailDTO> list) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "insert into YS_CheckInventoryDetail(id, addtime, edittime, expectqtypcs, materialid, realqtypcs2, headerid)\n" +
                "values (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        for (CheckInventoryDetailDTO dto : list) {
            Object[] objects = buildCheckInventoryDetailObjects(dto);
            batchArgs.add(objects);
        }
        int[] ints = super.getJdbcTemplate().batchUpdate(sql, batchArgs);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  插入YS_CheckInventoryDetail:" + ints.length + ",耗时：" + interval + "秒。");
    }

    /**
     * 构造明细Objs
     *
     * @param dto
     * @return
     */
    private Object[] buildCheckInventoryDetailObjects(CheckInventoryDetailDTO dto) {
        return new Object[]{dto.getId(), dto.getAddTime(), dto.getEditTime(), dto.getExpectQtyPcs(), dto.getMaterialId(), dto.getRealQtyPcs2(), dto.getHeaderId()};
    }


    /**
     * 插入盘点数据
     *
     * @param dto
     */
    @Override
    public void insertCheckInventoryCodeLib(CheckInventoryCodeLibDTO dto) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "insert into YS_CheckInventoryCodeLib(id, addtime, edittime, batchcode, currentorgid, currentorgtype, detailid, headerid,\n" +
                "                                     materialid, packdate, productioncodeid, scancode)\n" +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] objects = buildCheckInventoryCodeLibObjects(dto);
        int update = super.getJdbcTemplate().update(sql, objects);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  插入YS_CheckInventoryCodeLib:" + update + ",耗时：" + interval + "秒。");
    }

    /**
     * 批量插入盘点单数据
     *
     * @param list
     */
    @Override
    public void batchInsertCheckInventoryCodeLib(List<CheckInventoryCodeLibDTO> list) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "insert into YS_CheckInventoryCodeLib(id, addtime, edittime, batchcode, currentorgid, currentorgtype, detailid, headerid,\n" +
                "                                     materialid, packdate, productioncodeid, scancode)\n" +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        for (CheckInventoryCodeLibDTO dto : list) {
            Object[] objects = buildCheckInventoryCodeLibObjects(dto);
            batchArgs.add(objects);
        }
        int[] ints = super.getJdbcTemplate().batchUpdate(sql, batchArgs);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("  插入YS_CheckInventoryCodeLib:" + ints.length + ",耗时：" + interval + "秒。");
    }

    /**
     * 构造数据Objs
     *
     * @param dto
     * @return
     */
    private Object[] buildCheckInventoryCodeLibObjects(CheckInventoryCodeLibDTO dto) {
        return new Object[]{dto.getId(), dto.getAddTime(), dto.getEditTime(), dto.getBatchCode(), dto.getCurrentOrgId(), dto.getCurrentOrgType(), dto.getDetailId(), dto.getHeaderId()
                , dto.getMaterialId(), dto.getPackDate(), dto.getProductionCodeId(), dto.getScanCode()};
    }
}
