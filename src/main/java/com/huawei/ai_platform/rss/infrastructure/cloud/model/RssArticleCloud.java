package com.huawei.ai_platform.rss.infrastructure.cloud.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Article structure that will use to upload data into cloud
 *
 * @author Borodulin Arterm
 * @since 2026.03.10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssArticleCloud {
    private long articleId;

    private String articleTitleEn;
    private String articleTitleZh;

    private List<String> articleAuthors;

    private String articleContent;
    private String articleContentZh;

    private String articleLink;
    private List<String> articleTags;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createDate;

    private int categoryId;
    private int feedId;
}
