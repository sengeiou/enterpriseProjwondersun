package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebc.entity.ImportCodeRecord;
import com.proj.repository.ProjImportCodeRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ehsure on 2018/4/20.
 * 自定义生产数据导入repo
 */
@Repository
public class ProjImportCodeRecordRepositoryImpl extends BaseHibernateRepository<ImportCodeRecord> implements ProjImportCodeRecordRepository {

    @Override
    public List<ImportCodeRecord> getImportCodeRecordList(String batchCode, String materialId) {
        String sql = " from ImportCodeRecord  where batchCode=:batchCode and materialId=:materialId";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("materialId", materialId);
        return findEntityObjects(sql, kvList);
    }
}
