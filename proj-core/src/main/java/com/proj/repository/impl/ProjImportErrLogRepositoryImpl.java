package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.proj.entity.ProjImportErrLog;
import com.proj.repository.ProjImportErrLogRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ProjImportErrLogRepositoryImpl extends BaseHibernateRepository<ProjImportErrLog> implements ProjImportErrLogRepository {
}
