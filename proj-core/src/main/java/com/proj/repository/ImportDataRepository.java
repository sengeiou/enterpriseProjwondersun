package com.proj.repository;

import com.ebp.entity.SysGeoCity;
import com.ebp.entity.SysStructure;

/**
 * Created by ehsure on 2016/8/18.
 */
public interface ImportDataRepository {
    SysStructure getSysStructureByName(String name);

    SysGeoCity getSysGeoCityByName(String name);
}
