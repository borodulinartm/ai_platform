package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Rss category DAO layer
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Mapper
public interface RssCategoryDao extends BaseMapper<RssCategoryEntity> {
    /**
     * Performs extracting RSS category entity (only without errors)
     *
     * @return list of an entities
     */
    List<RssCategoryEntity> queryCategoriesList();
}
