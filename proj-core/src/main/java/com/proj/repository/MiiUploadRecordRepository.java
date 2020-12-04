package com.proj.repository;

import com.arlen.eaf.core.repository.BaseCrudRepository;
import com.proj.entity.MiiUploadRecord;

import java.util.List;

/**
 * 上传到工信部文件记录repo
 */
public interface MiiUploadRecordRepository extends BaseCrudRepository<MiiUploadRecord> {
    /**
     * 根据批次和materialId查询记录
     * @param batchCode
     * @param prodId
     * @return
     */
     MiiUploadRecord getOne(String batchCode,String prodId);

    /**
     * 根据批次和materialId、status查询记录
     * @param batchCode
     * @param prodId
     * @param status
     * @return
     */
     List<MiiUploadRecord> findList(String batchCode,String prodId,int status);

    /**
     * 根据fileUploadStatus查询记录
     * @param fileUploadStatus
     * @return
     */
     List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus);
    List<MiiUploadRecord> findListByFileUploadStatus(int fileUploadStatus,int size);
}
