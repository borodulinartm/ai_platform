package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Rss category DAO layer
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Mapper
public interface RssCategoryDao extends BaseMapper<RssCategoryEntity> {
}
