SELECT  record.Id AS ID ,
        record.BatchCode AS BATCHCODE ,
        record.attr1 AS ATTR1 ,
        record.addTime AS  ADDTIME,
        record.addBy AS  ADDBY,
        record.addBy AS  SUBMITBY,
        record.attr2 AS ATTR2 ,
        record.attr3 AS ATTR3 ,
        record.attr4 AS ATTR4 ,
        record.attr5 AS ATTR5 ,
        record.attr6 AS ATTR6 ,
        record.MaterialId AS MATERIALID ,
        material.ShortCode AS MATERIALCODE ,
        material.Sku AS MATERIALSKU ,
        material.ShortName AS MATERIALNAME ,
        record.FileUploadStatus AS FILEUPLOADSTATUS ,
        CASE record.FileUploadStatus
          WHEN 0 THEN '待上传'
          WHEN 1 THEN '上传中'
          WHEN 2 THEN '上传成功'
          WHEN 3 THEN '上传失败'
        END AS FILEUPLOADSTATUSNAME ,
        CASE record.FileUploadStatus
          WHEN 0 THEN '未质检'
          WHEN 1 THEN '正在质检'
          WHEN 2 THEN '质检通过'
          WHEN 3 THEN '质检失败'
        END AS QCSTATUS ,
        record.ConformityCertificate AS CONFORMITYCERTIFICATE ,
        record.ProdInspectionReport AS PRODINSPECTIONREPORT ,
        record.UploadLocalTime AS UPLOADLOCALTIME ,
        record.MiiUploadTime AS MIIUPLOADTIME ,
        record.ConCertUploadLocalTime AS CONCERTUPLOADLOCALTIME ,
        record.InStockQtyMax AS INSTOCKQTYMAX ,
        record.InStockQtyPcs AS INSTOCKQTYPCS ,
        record.InStockQtyPcs4sp AS INSTOCKQTYPCS4SP ,
        record.FileUploadStatus4one AS FILEUPLOADSTATUS4ONE ,
        record.MiiUploadTime4one AS MIIUPLOADTIME4ONE ,
        record.InStockQtyPcs4one AS INSTOCKQTYPCS4ONE ,
        CASE record.FileUploadStatus4one
          WHEN 0 THEN '待上传'
          WHEN 1 THEN '上传中'
          WHEN 2 THEN '上传成功'
          WHEN 3 THEN '上传失败'
        END AS FILEUPLOADSTATUS4ONENAME,
        record.ProductStandard AS PRODUCTSTANDARD,
        record.ProductStandardPdf AS PRODUCTSTANDARDPDF
FROM    ebcProductQcRecord record
        LEFT JOIN ebdMaterial material ON material.Id = record.MaterialId
WHERE   material.ext13='1' and (material.ext4 is null or material.ext4='0')
ORDER BY record.AddTime DESC