package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebt.entity.ExceptionData;
import com.proj.repository.ProjExceptionDataRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ehsure on 2018/3/12.
 * 自定义异常编码获取
 */
@Repository
public class ProjExceptionDataRepositoryImpl extends BaseHibernateRepository<ExceptionData> implements ProjExceptionDataRepository {

    @Override
    public List<ExceptionData> listByObjectIdAndSubjectId(String SubjectId, String ObjectId, String ExceptionReason, String orderType) {
        String sql = "select * from ebtExceptionData with(noLock) where OrdType=:orderType and ObjectId=:ObjectId and SubjectId=:SubjectId  and ExceptionReason=:ExceptionReason  ";
        Map<String, Object> map = new HashMap<>();
        map.put("SubjectId", SubjectId);
        map.put("ObjectId", ObjectId);
        map.put("ExceptionReason", ExceptionReason);
        map.put("orderType", orderType);
        return findEntityObjectsBySql(sql, map);
    }
}
