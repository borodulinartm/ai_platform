package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Rss Feed mapper base level
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Mapper
public interface RssFeedDao extends BaseMapper<RssFeedEntity> {
}
