package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.proj.entity.BillPushToYsRecord;
import com.proj.repository.BillPushToYsRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * 单据推送到美驰系统记录实现类
 * Created by johnny on 2019/12/23.
 *
 * @author johnny
 */
@Repository
public class BillPushToYsRecordRepositoryImpl extends BaseHibernateRepository<BillPushToYsRecord> implements BillPushToYsRecordRepository {
    
    /**
     * 获取待推送的记录
     *
     * @param size
     * @return
     */
    @Override
    public List<BillPushToYsRecord> getWaitPushRecordList(int size) {
        String hql = "from BillPushToYsRecord where pushStatus = 0    order by addTime asc";
        HashMap<String, Object> kvList = new HashMap();
        return super.findTopNEntityObjects(hql, kvList, size);
    }
}
