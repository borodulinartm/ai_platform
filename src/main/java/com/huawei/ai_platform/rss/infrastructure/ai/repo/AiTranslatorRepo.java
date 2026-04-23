package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineRequest;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * AI content generator
 *
 * @author Borodulin Artem
 * @since 2026.03.23
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiTranslatorRepo {
    public static final String PIPELINE_NAME = "TRANSLATING";

    private final IAiStageExecutor defaultAiExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;

    @Value("${ai.translating.countAttempts}")
    private int maxCountAttempts;

    @Value("${ai.translating.temperature}")
    private Double temperature;

    /**
     * Performs translation
     *
     * @param request article that need to request
     * @return response data
     */
    public AiTranslationResponse translate(@Nonnull AiTranslationRequest request) {
        AIPipelineResponse titleEn = exec(request, request.getArticleTitle(), Locale.ENGLISH);
        AIPipelineResponse titleZh = exec(request, request.getArticleTitle(), Locale.SIMPLIFIED_CHINESE);
        AIPipelineResponse contentEn = exec(request, request.getArticleContent(), Locale.ENGLISH);
        AIPipelineResponse contentZh = exec(request, request.getArticleContent(), Locale.SIMPLIFIED_CHINESE);

        if (!(titleEn.isSuccess() && titleZh.isSuccess() && contentEn.isSuccess() && contentZh.isSuccess())) {
            return AiTranslationResponse.failureResponse(request.getArticleId(), "Not translated :(");
        }

        return AiTranslationResponse.successResponse(request.getArticleId(),
                titleEn.getPayload(), titleZh.getPayload(), contentEn.getPayload(), contentZh.getPayload()
        );
    }

    /**
     * Performs executing data in the pipeline side
     *
     * @param request cleaning request
     * @param payload payload
     * @return response from the pipeline
     */
    private AIPipelineResponse exec(AiTranslationRequest request, String payload, Locale locale) {
        String translationPrompt = locale == Locale.ENGLISH ? "prompt/translations/translation-prompt-en.txt" : "prompt/translations/translation-prompt-zh.txt";
        String normalizingPrompt = locale == Locale.ENGLISH ? "prompt/translations/normalization-prompt-en.txt" : "prompt/translations/normalization-prompt-zh.txt";

        String userPrompt = "prompt/user-prompt.txt";

        AiPipelineRequest pipelineRequest = AiPipelineBuilder.createBuilder(PIPELINE_NAME, payload)
                .addStage(
                        request.getArticleId(), "TRANSLATING", defaultAiExecutor, translationPrompt, userPrompt,
                        "", null, temperature, maxCountAttempts
                )
                .addStage(
                        request.getArticleId(), "NORMALIZATION STAGE", defaultAiExecutor, normalizingPrompt, userPrompt,
                        "", null, temperature, maxCountAttempts
                ).build();

        AIPipelineResponse pipelineResponse = aiPipelineExecutor.executePipeline(pipelineRequest);

        if (!pipelineResponse.isSuccess()) {
            return AIPipelineResponse.failure(PIPELINE_NAME, pipelineResponse.getFailureReason());
        }

        return AIPipelineResponse.success(PIPELINE_NAME, pipelineResponse.getPayload());
    }
}
