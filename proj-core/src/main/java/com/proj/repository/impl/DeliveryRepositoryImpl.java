package com.proj.repository.impl;

import com.arlen.ebms.utils.DateTimeTool;
import com.proj.dto.ys.DeliveryCodeLibDTO;
import com.proj.dto.ys.DeliveryDTO;
import com.proj.dto.ys.DeliveryDetailDTO;
import com.proj.dto.ys.PushDeliveryDTO;
import com.proj.repository.DeliveryRepository;
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
 * 单据推送服务类
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */

@Repository
public class DeliveryRepositoryImpl extends JdbcDaoSupport implements DeliveryRepository {

    private static Logger logger = LoggerFactory.getLogger(DeliveryRepositoryImpl.class);

    @Override
    public void inserMcTable(PushDeliveryDTO pushDeliveryDTO) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        PlatformTransactionManager ptm = new DataSourceTransactionManager(this.getDataSource());
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = ptm.getTransaction(def);
        try {
            this.insertDelivery(pushDeliveryDTO.getDelivery());
            this.batchInsertDeliveryDetail(pushDeliveryDTO.getDeliveryDetailList());
            this.batchInsertDeliveryLib(pushDeliveryDTO.getDeliveryCodeLibList());
            ptm.commit(status);
        } catch (Exception e) {
            ptm.rollback(status);
            throw e;
        } finally {
            Date logEndTime = DateTimeTool.getCurDatetime();
            long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
            logger.info("     插入inserMcTable:id=" + pushDeliveryDTO.getDelivery().getId() + ",耗时：" + interval + "秒。");
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
        String sql = "SELECT COUNT(*) FROM YS_Delivery WHERE Id = ?";
        Object args[] = new Object[]{id};
        int count = super.getJdbcTemplate().queryForObject(sql, args, Integer.class);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     查询YS_Delivery:id=" + id + ",count=" + count + ",耗时：" + interval + "秒。");
        return count;
    }

    /**
     * 插入单据
     *
     * @param dto
     */
    @Override
    public void insertDelivery(DeliveryDTO dto) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_Delivery (id, AddTime, EditTime, AddFrom, BillStatus, BillTypeId, code, DestId, DestWareId, ScanEndTime, SourceBillId, SrcId, SrcWareId, AuditTime)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] objects = buildDeliveryObjects(dto);
        int update = super.getJdbcTemplate().update(sql, objects);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     插入YS_Delivery:" + update + ",耗时：" + interval + "秒。");
    }

    /**
     * 批量插入单据
     *
     * @param list
     */
    @Override
    public void batchInsertDelivery(List<DeliveryDTO> list) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_Delivery (id, AddTime, EditTime, AddFrom, BillStatus, BillTypeId, code, DestId, DestWareId, ScanEndTime, SourceBillId, SrcId, SrcWareId, AuditTime)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        for (DeliveryDTO dto : list) {
            Object[] objects = buildDeliveryObjects(dto);
            batchArgs.add(objects);
        }
        int[] ints = super.getJdbcTemplate().batchUpdate(sql, batchArgs);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     插入YS_Delivery:" + ints.length + ",耗时：" + interval + "秒。");
    }

    /**
     * 构造单据Objs
     *
     * @param dto
     * @return
     */
    private Object[] buildDeliveryObjects(DeliveryDTO dto) {
        return new Object[]{dto.getId(), dto.getAddTime(), dto.getEditTime(), dto.getAddFrom(), dto.getBillStatus(), dto.getBillTypeId(), dto.getCode(), dto.getDestId(), dto.getDestWareId()
                , dto.getScanEndTime(), dto.getSourceBillId(), dto.getSrcId(), dto.getSrcWareId(), dto.getAuditTime()};
    }

    /**
     * 插入明细
     *
     * @param dto
     */
    @Override
    public void insertDeliveryDetail(DeliveryDetailDTO dto) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_DeliveryDetail (id, AddTime, EditTime, ExpectQtyPcs, RealQtyPcs, HeaderId, MaterialId, SaleQtyPcs, IsPolicy)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] objects = buildDeliveryDetailObjects(dto);
        int update = super.getJdbcTemplate().update(sql, objects);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     插入YS_DeliveryDetail:" + update + ",耗时：" + interval + "秒。");
    }

    /**
     * 批量插入明细
     *
     * @param list
     */
    @Override
    public void batchInsertDeliveryDetail(List<DeliveryDetailDTO> list) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_DeliveryDetail (id, AddTime, EditTime, ExpectQtyPcs, RealQtyPcs, HeaderId, MaterialId, SaleQtyPcs, IsPolicy)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        for (DeliveryDetailDTO dto : list) {
            Object[] objects = buildDeliveryDetailObjects(dto);
            batchArgs.add(objects);
        }
        int[] ints = super.getJdbcTemplate().batchUpdate(sql, batchArgs);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     插入YS_DeliveryDetail:" + ints.length + ",耗时：" + interval + "秒。");
    }

    /**
     * 构造明细Objs
     *
     * @param dto
     * @return
     */
    private Object[] buildDeliveryDetailObjects(DeliveryDetailDTO dto) {
        return new Object[]{dto.getId(), dto.getAddTime(), dto.getEditTime(), dto.getExpectQtyPcs(), dto.getRealQtyPcs(), dto.getHeaderId(), dto.getMaterialId(), dto.getSaleQtyPcs(), dto.getIsPolicy()};
    }

    /**
     * 插入单据编码
     *
     * @param dto
     */
    @Override
    public void insertDeliveryCodeLib(DeliveryCodeLibDTO dto) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_DeliveryCodeLib (id, AddTime, EditTime, BatchCode, CodePath, DetailId, HeaderId, PackDate, ProductionCodeId, ScanCode, ScanTime, MaterialId)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] objects = buildDeliveryCodeLibObjects(dto);
        int update = super.getJdbcTemplate().update(sql, objects);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     插入YS_DeliveryCodeLib:" + update + ",耗时：" + interval + "秒。");
    }

    /**
     * 批量插入单据编码
     *
     * @param list
     */
    @Override
    public void batchInsertDeliveryLib(List<DeliveryCodeLibDTO> list) {
        Date logStartTime = DateTimeTool.getCurDatetime();
        String sql = "INSERT INTO YS_DeliveryCodeLib (id, AddTime, EditTime, BatchCode, CodePath, DetailId, HeaderId, PackDate, ProductionCodeId, ScanCode, ScanTime, MaterialId)\n" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<Object[]>();
        for (DeliveryCodeLibDTO dto : list) {
            Object[] objects = buildDeliveryCodeLibObjects(dto);
            batchArgs.add(objects);
        }
        int[] ints = super.getJdbcTemplate().batchUpdate(sql, batchArgs);
        Date logEndTime = DateTimeTool.getCurDatetime();
        long interval = (logEndTime.getTime() - logStartTime.getTime()) / 1000;
        logger.info("     插入YS_DeliveryCodeLib:" + ints.length + ",耗时：" + interval + "秒。");
    }

    /**
     * 构造单据编码Objs
     *
     * @param dto
     * @return
     */
    private Object[] buildDeliveryCodeLibObjects(DeliveryCodeLibDTO dto) {
        return new Object[]{dto.getId(), dto.getAddTime(), dto.getEditTime(), dto.getBatchCode(), dto.getCodePath(), dto.getDetailId(), dto.getHeaderId(), dto.getPackDate(), dto.getProductionCodeId(),
                dto.getScanCode(), dto.getScanTime(), dto.getMaterialId()};
    }


}
