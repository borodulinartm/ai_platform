package com.huawei.ai_platform.rss;

import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.repo.RssRepository;
import com.huawei.ai_platform.rss.application.service.impl.RssServiceImpl;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssFeedEntity;
import com.huawei.ai_platform.rss.model.RssCategory;
import com.huawei.ai_platform.rss.model.RssData;
import com.huawei.ai_platform.rss.model.RssFeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.huawei.ai_platform.common.OperationResultEnum.FAILURE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit testing for the uploading reports
 * That class demonstrates correct situation
 */
@ExtendWith(MockitoExtension.class)
class RssSynchronizationUnitTest {
    @Mock
    private RssRepository rssRepository;

    @InjectMocks
    private RssServiceImpl rssSyncService;

    /**
     * Basic unit testing. Check that we upload exactly once categories,
     * feeds, articles, not more
     */
    @Test
    public void testUploadReport_shouldCompleteSuccessfully() {
        // Prepare DB mocking
        List<RssData> rssDataList = Arrays.asList(
                RssData.builder().articleId(1L).articleTitle("Title").articleContent("Content").articleAuthors(null).build(),
                RssData.builder().articleId(2L).articleTitle("Title #2").articleContent("Content #2").articleAuthors(null).build()
        );
        when(rssRepository.getArticlesBy(any())).thenReturn(rssDataList);
        when(rssRepository.getListCategories()).thenReturn(
                List.of(RssCategory.builder().categoryNameEn("Name1").build())
        );
        when(rssRepository.getListFeeds()).thenReturn(
                List.of(RssFeed.builder().feedNameEn("Feed").feedId(1).build())
        );

        // Prepare FS mocking
        OperationResult mockResult = OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Reason").build();
        when(rssRepository.uploadCategories(anyList())).thenReturn(mockResult);
        when(rssRepository.uploadFeeds(anyList())).thenReturn(mockResult);
        when(rssRepository.uploadArticles(anyList(), any())).thenReturn(mockResult);

        OperationResult result = rssSyncService.uploadNewArticles(LocalDateTime.now().minusDays(1L));

        assertThat(result.isSuccess()).isTrue();

        // Verification blocks (that we call uploading mechanisms exactly once)
        // For uploading into cloud
        verify(rssRepository, times(1)).uploadCategories(anyList());
        verify(rssRepository, times(1)).uploadFeeds(anyList());
        verify(rssRepository, times(1)).uploadArticles(anyList(), any());

        // For DB calling
        verify(rssRepository, times(1)).getListCategories();
        verify(rssRepository, times(1)).getListFeeds();
        verify(rssRepository, times(1)).getArticlesBy(any());
    }

    @Test
    public void testUploadReport_shouldCompleteWithFailingBecauseUploadFails() {
        when(rssRepository.getListCategories()).thenReturn(
                List.of(RssCategory.builder().categoryNameEn("Name1").build())
        );
        // Prepare FS mocking
        OperationResult failResult = OperationResult.builder().state(FAILURE).reason("Failure for some reason").build();

        when(rssRepository.uploadCategories(anyList())).thenReturn(failResult);
        OperationResult result = rssSyncService.uploadNewArticles(LocalDateTime.now().minusDays(1L));

        assertThat(result.isFailed()).isTrue();
    }
}
