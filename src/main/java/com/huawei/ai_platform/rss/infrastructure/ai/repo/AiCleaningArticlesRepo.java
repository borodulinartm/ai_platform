package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineRequest;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.RssAttributeTypeEnum.IMAGE_JPEG;
import static com.huawei.ai_platform.rss.infrastructure.persistence.enums.RssAttributeTypeEnum.IMAGE_PNG;

/**
 * Cleaner section for the AI
 *
 * @author Borodulin Artem
 * @since 2026.04.04
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiCleaningArticlesRepo {
    public static final String PIPELINE_NAME = "CLEANING";

    private final IAiStageExecutor defaultAiExecutor;
    private final IAiStageExecutor relevanceStageExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;

    @Value("${ai.cleaning.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.cleaning.temperature}")
    private Double temperature;

    /**
     * Performs cleaning data for the input side
     *
     * @param cleaningRequest cleaning request
     * @return cleaning response
     */
    public @Nonnull AiCleaningResponse processCleaning(@Nonnull AiCleaningRequest cleaningRequest) {
        StringBuilder inputContent = new StringBuilder(cleaningRequest.getArticleContent());

        if (cleaningRequest.getAttributes() != null && !CollectionUtils.isEmpty(cleaningRequest.getAttributes().getEnclosures())) {
            for (RssAttributeValue.Enclosure enclosure : cleaningRequest.getAttributes().getEnclosures()) {
                if (enclosure.getType() != null && (enclosure.getType() == IMAGE_JPEG || enclosure.getType() == IMAGE_PNG)) {
                    inputContent.append(String.format("<img src=\"%s\"/>", enclosure.getUrl()));
                }
            }
        }

        AIPipelineResponse pipelineResponse = exec(cleaningRequest, inputContent.toString());

        if (!pipelineResponse.isSuccess()) {
            return AiCleaningResponse.failure(cleaningRequest.getId(), pipelineResponse.getFailureReason());
        }

        return AiCleaningResponse.success(cleaningRequest.getId(), cleaningRequest.getArticleTitle(),
                pipelineResponse.getPayload(), cleaningRequest.getArticleLink()
        );
    }

    /**
     * Performs executing data in the pipeline side
     *
     * @param request cleaning request
     * @param payload payload
     * @return response from the pipeline
     */
    private AIPipelineResponse exec(AiCleaningRequest request, String payload) {
        String relevancePrompt = "prompt/cleaning/relevance-check-prompt.txt";
        String cleaningHtmlPrompt = "prompt/cleaning/cleaning-prompt.txt";
        String noisePrompt = "prompt/cleaning/noise-removing-prompt.txt";
        String normalizingPrompt = "prompt/cleaning/normalization-prompt.txt";

        String userPrompt = "prompt/user-prompt.txt";

        AiPipelineRequest pipelineRequest = AiPipelineBuilder.createBuilder(PIPELINE_NAME)
                .addStage(
                        request.getId(), "RELEVANCE CHECK", relevanceStageExecutor,
                        relevancePrompt, userPrompt, payload,
                        "", temperature, maxCountAttempts
                )
                .addStage(
                        request.getId(), "CLEANING STAGE", defaultAiExecutor, cleaningHtmlPrompt, userPrompt, payload,
                        "", temperature, maxCountAttempts
                )
                .addStage(
                        request.getId(), "REMOVING NOISE STAGE", defaultAiExecutor, noisePrompt, userPrompt,
                        "", temperature, maxCountAttempts
                )
                .addStage(
                        request.getId(), "NORMALIZATION STAGE", defaultAiExecutor, normalizingPrompt, userPrompt,
                        "", temperature, maxCountAttempts
                ).build();

        AIPipelineResponse pipelineResponse = aiPipelineExecutor.executePipeline(pipelineRequest);

        if (!pipelineResponse.isSuccess()) {
            return AIPipelineResponse.failure(PIPELINE_NAME, pipelineResponse.getFailureReason());
        }

        return AIPipelineResponse.success(PIPELINE_NAME, pipelineResponse.getPayload());
    }
}
