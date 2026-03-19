package com.huawei.ai_platform.rss;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.common.OperationResult;
import com.huawei.ai_platform.common.OperationResultEnum;
import com.huawei.ai_platform.rss.application.service.RssConfigService;
import com.huawei.ai_platform.rss.application.service.RssSyncService;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssCategoryDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssDao;
import com.huawei.ai_platform.rss.infrastructure.persistence.dao.RssFeedDao;
import com.huawei.ai_platform.rss.infrastructure.web.RssController;
import com.huawei.ai_platform.rss.infrastructure.web.assembler.RssNewsAssembler;
import com.huawei.ai_platform.rss.infrastructure.web.model.RssReportDto;
import com.huawei.ai_platform.rss.model.RssCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {RssController.class},
        excludeAutoConfiguration = MybatisPlusAutoConfiguration.class
)
public class UploadingReportsUnitTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    public RssSyncService rssSyncService;

    @MockitoBean
    private RssConfigService rssConfigService;

    @MockitoBean
    private RssNewsAssembler rssNewsAssembler;

    // Code smell !!!
    @MockitoBean
    private RssDao rssDao;

    @MockitoBean
    private RssCategoryDao rssCategoryDao;

    @MockitoBean
    private RssFeedDao rssFeedDao;

    @Test
    public void performUploadDocument_ShouldReturnOk() throws Exception {
        when(rssSyncService.uploadReport(anyList(), any())).thenReturn(
                OperationResult.builder().state(OperationResultEnum.SUCCESS).reason("Reason").build()
        );
        when(rssConfigService.listCategories()).thenReturn(
                List.of(RssCategory.builder().categoryNameEn("Name").categoryId(1).build())
        );

//        List<RssReportDto> newsReportDtoList = List.of(
//                RssReportDto.builder()
//                        .articleTitle("title").authors(Collections.emptyList())
//                        .articleLink("foo.ru").background("back and ground").categoryId(1)
//                        .effects("Effects").eventSummary("Event summary").technologyAndInnovation("innovation")
//                        .valueAndImpact("Value and impact").build()
//        );
//
//        mockMvc.perform(
//                MockMvcRequestBuilders.post("/v1/upload-report")
//                        .param("report_date", "2026-03-11")
//                        .contentType(MediaType.APPLICATION_JSON_VALUE)
//                        .content(objectMapper.writeValueAsString(newsReportDtoList))
//        ).andExpect(status().is(HttpStatus.OK.value()));
    }
}
