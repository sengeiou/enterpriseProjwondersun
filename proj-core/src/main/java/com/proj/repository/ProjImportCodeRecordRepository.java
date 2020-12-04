package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.arlen.ebc.entity.ImportCodeRecord;

import java.util.List;

/**
 * Created by ehsure on 2018/4/20.
 * 自定义生产数据导入repo
 */
public interface ProjImportCodeRecordRepository extends BaseCrudRepository<ImportCodeRecord> {
    /**
     * 获取同批次，同产品的生产记录 -力博数据
     *
     * @param batchCode  批次
     * @param materialId 产品
     * @return return
     */
    List<ImportCodeRecord> getImportCodeRecordList(String batchCode, String materialId);
}
