package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.arlen.ebt.entity.ExceptionData;

import java.util.List;

/**
 * Created by ehsure on 2018/3/12.
 * 自定义异常编码获取
 */
public interface ProjExceptionDataRepository extends BaseCrudRepository<ExceptionData> {

    List<ExceptionData> listByObjectIdAndSubjectId(String SubjectId, String ObjectId, String ExceptionReason, String orderType);
}
