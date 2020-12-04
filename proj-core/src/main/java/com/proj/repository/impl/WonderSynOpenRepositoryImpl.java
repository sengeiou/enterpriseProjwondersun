package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebd.dto.MaterialDTO;
import com.arlen.ebp.dto.SysGeoCityDTO;
import com.arlen.ebp.dto.SysStructureDTO;
import com.arlen.ebt.entity.BillData;
import com.arlen.ebt.entity.STBillData;
import com.proj.repository.WonderSynOpenRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by arlenChen on 2019/12/18.
 * 对外数据
 *
 * @author arlenChen
 */
@Repository
public class WonderSynOpenRepositoryImpl extends BaseHibernateRepository implements WonderSynOpenRepository {
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 获取需要同步中间表的产品数据
     *
     * @param lastSyncTime 最后同步时间
     * @return 数据
     */
    @Override
    public List<MaterialDTO> getSyncMaterialList(String lastSyncTime) {
        String sql = " " +
                "SELECT material.Id as Id " +
                "      ,material.ForeignId as ForeignId " +
                "      ,material.ShortCode as ShortCode " +
                "      ,material.Sku as Sku " +
                "      ,material.ShortName as ShortName " +
                "      ,material.FullName as FullName  " +
                "      ,material.EnglishName  as EnglishName " +
                "      ,material.ExworkPrice as ExworkPrice " +
                "      ,material.RetailPrice as RetailPrice " +
                "      ,material.Spec as Spec " +
                "      ,material.ShelfLife as ShelfLife " +
                "      ,material.ShelfLifeUnit as ShelfLifeUnit " +
                "      ,material.ForwardDays as ForwardDays " +
                "      ,material.Description as Description " +
                "      ,material.ComQty as ComQty  " +
                "      ,material.PcsQty as  PcsQty " +
                "      ,material.BoxQty as BoxQty  " +
                "      ,material.PcsGtin as PcsGtin " +
                "      ,material.CollecQty as CollecQty  " +
                "      ,material.ComGtin as ComGtin " +
                "      ,material.BoxGtin as BoxGtin " +
                "      ,material.PcsVolume as PcsVolume " +
                "      ,material.VolumeUnit as VolumeUnit " +
                "      ,material.PcsWeight as PcsWeight " +
                "      ,material.WeightUnit as WeightUnit " +
                "      ,material.Origin as Origin " +
                "      ,Spec.name as  specId " +
                "      ,Stage.name as StageId " +
                "      ,Series.name as SeriesId " +
                "      ,material.InUse as InUse " +
                "      ,material.OnSale as OnSale  " +
                "      ,material.SyncTime as SyncTime " +
                "      ,Package.name as PackageId " +
                "      ,material.GrossWeight as  GrossWeight " +
                "      ,material.ErrorRange as ErrorRange " +
                "      ,material.NoCode as NoCode " +
                "      ,material.UnitId as UnitId " +
                "      ,material.CategoryId as CategoryId " +
                "      ,material.MaterialType as  MaterialType " +
                "      ,material.NewMaterial as NewMaterial " +
                "      ,material.AddTime as AddTime " +
                "      ,material.EditTime as EditTime " +
                "  FROM ebdMaterial material with(noLock) " +
                "LEFT  JOIN  ebdSimpleType  Spec  with(noLock) ON  material.SpecId =Spec.ID " +
                "LEFT  JOIN  ebdSimpleType  Stage  with(noLock) ON  material.StageId =Stage.ID " +
                "LEFT  JOIN  ebdSimpleType  Series  with(noLock) ON  material.SeriesId =Series.ID " +
                "LEFT  JOIN  ebdSimpleType  Package  with(noLock) ON  material.PackageId =Package.ID " +
                " where  1=1 ";
        Map<String, Object> map = new HashMap<>(0);
        if (StringUtils.isNotEmpty(lastSyncTime)) {
            sql += " and material.EditTime > '" + lastSyncTime + "' ";
        }
        sql += "order by material.EditTime desc";
        return findAllResutlBeansBySql(sql, map, MaterialDTO.class);
    }

    @Override
    public List<SysGeoCityDTO> getSyncGeoCityList(String lastSyncTime) {
        String sql = "select * from sysGeoCity with(noLock) where 1=1 ";
        Map<String, Object> map = new HashMap<>(0);
        if (StringUtils.isNotEmpty(lastSyncTime)) {
            sql += " and EditTime > '" + lastSyncTime + "' ";
        }
        sql += "order by EditTime desc";
        return findAllResutlBeansBySql(sql, map, SysGeoCityDTO.class);
    }

    /**
     * 获取需要同步中间表的考核城市
     *
     * @param lastSyncTime 最后同步时间
     * @return 数据
     */
    @Override
    public List<SysStructureDTO> getSyncCheckCityList(String lastSyncTime) {
        String sql = "select Id,AddTime,EditTime,Deleted as IsDeleted,Name,Ordinal,AreaId,ParentId,RegionId,LevelNum from sysStructure with(noLock) where 1=1 ";
        Map<String, Object> map = new HashMap<>(0);
        if (StringUtils.isNotEmpty(lastSyncTime)) {
            sql += " and EditTime > '" + lastSyncTime + "' ";
        }
        sql += "order by EditTime desc";
        return findAllResutlBeansBySql(sql, map, SysStructureDTO.class);
    }

    /**
     * 获取单据编码
     *
     * @param headerId
     * @return
     */
    @Override
    public List<BillData> getBillData(String headerId) {
        String hql = "select * from ebtBillData with(noLock) where headerId=:headerId";
        HashMap<String, Object> kvList = new HashMap();
        kvList.put("headerId", headerId);
        return super.findAllResutlBeansBySql(hql, kvList, BillData.class);
    }

    /**
     * 获取盘点单编码
     *
     * @param headerId
     * @return
     */
    @Override
    public List<STBillData> getSTBillData(String headerId) {
        String hql = "select * from ebtSTBillData with(noLock) where headerId=:headerId";
        HashMap<String, Object> kvList = new HashMap();
        kvList.put("headerId", headerId);
        return super.findAllResutlBeansBySql(hql, kvList, STBillData.class);
    }
}
