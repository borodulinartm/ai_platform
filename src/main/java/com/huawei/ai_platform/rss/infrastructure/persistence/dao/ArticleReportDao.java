package com.huawei.ai_platform.rss.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.ArticleReportEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ArticleReportDao extends BaseMapper<ArticleReportEntity> {
    List<ArticleReportEntity> queryByCategoryAndDate(@Param("categoryId") int categoryId,
                                                     @Param("reportDate") LocalDate reportDate);

    void deleteByCategoryAndDate(@Param("categoryId") int categoryId,
                                 @Param("reportDate") LocalDate reportDate);

    void insertReports(@Param("data") List<ArticleReportEntity> data);
}
