package com.huawei.ai_platform.rss.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "article_report")
public class ArticleReportEntity {
    @TableId
    private Long id;

    private int categoryId;
    private LocalDate reportDate;
    private Long articleId;
    private Integer position;
    private Double relevanceScore;
    private String articleTitleEn;
    private String articleTitleZh;
    private String articleAbstractEn;
    private String articleAbstractZh;
    private String articleLink;
    private String backgroundEn;
    private String backgroundZh;
    private String effectsEn;
    private String effectsZh;
    private String eventSummaryEn;
    private String eventSummaryZh;
    private String technologyAndInnovationEn;
    private String technologyAndInnovationZh;
    private String valueAndImpactEn;
    private String valueAndImpactZh;
    private String categorySummaryEn;
    private String categorySummaryZh;
}
