package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.proj.entity.MiiUploadRecord;
import com.proj.repository.MiiUploadRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * 上传到工信部文件记录repo实现
 */
@Repository
public class MiiUploadRecordRepositoryImpl extends BaseHibernateRepository<MiiUploadRecord> implements MiiUploadRecordRepository {

    /**
     * 根据批次和materialId查询记录
     * @param batchCode
     * @param prodId
     * @return
     */
    public MiiUploadRecord getOne(String batchCode,String prodId){
        String hql = "from MiiUploadRecord where batchCode=:batchCode " +
                " and prodId=:prodId ";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("prodId", prodId);

        return findOneEntityObject(hql, kvList);
    }

    /**
     * 根据批次和materialId、status查询记录
     * @param batchCode
     * @param prodId
     * @param status
     * @return
     */
    public List<MiiUploadRecord> findList(String batchCode,String prodId,int status){
        String hql = "from MiiUploadRecord where batchCode=:batchCode " +
                " and prodId=:prodId and fileUploadStatus=:status";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("batchCode", batchCode);
        kvList.put("prodId", prodId);
        kvList.put("status", status);

        return findEntityObjects(hql, kvList);
    }

    /**
     * 根据fileUploadStatus查询记录
     * @param fileUploadStatus
     * @return
     */
    public List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus){
        String hql = "from MiiUploadRecord where fileUploadStatus=:status order by uploadLocalTime asc";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("status", fileUploadStatus);
        return findEntityObjects(hql, kvList);
    }

    public List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus,int size){
        String hql = "from MiiUploadRecord where fileUploadStatus=:status order by uploadLocalTime asc";
        HashMap<String, Object> kvList = new HashMap<>();
        kvList.put("status", fileUploadStatus);
        return findTopNEntityObjects(hql, kvList,size);
    }
}
