package com.proj.repository.impl;

import com.arlen.eaf.core.repository.jpa.BaseHibernateRepository;
import com.arlen.ebp.entity.SysGeoCity;
import com.arlen.ebp.entity.SysStructure;
import com.proj.repository.ImportDataRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ehsure on 2016/8/18.
 */
@Repository
public class ImportDataRepositoryImpl  extends BaseHibernateRepository implements ImportDataRepository {

    @Override
    public SysStructure getSysStructureByName(String name) {
        String sql = "select * from sysStructure where name=:name";
        HashMap<String,Object> kvList=new HashMap<>();
        kvList.put("name",name);
        List<SysStructure> sysStructureList = findAllResutlBeansBySql(sql,kvList,SysStructure.class);
        if(sysStructureList==null || sysStructureList.size()==0){
            return null;
        }
        return sysStructureList.get(0);
    }

    @Override
    public SysGeoCity getSysGeoCityByName(String name) {
        String sql = "select * from sysGeoCity where name = :name";
        HashMap<String,Object> kvList=new HashMap<>();
        kvList.put("name",name);
        List<SysGeoCity> sysGeoCities = findAllResutlBeansBySql(sql,kvList,SysGeoCity.class);
        if(sysGeoCities==null || sysGeoCities.size()==0){
            return null;
        }
        return sysGeoCities.get(0);
    }
}
