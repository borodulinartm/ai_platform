package com.huawei.ai_platform.rss.infrastructure.ai.repo;

import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.IAiStageExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineRequest;
import jakarta.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public static final String PIPELINE_NAME = "TRANSLATION";
    private static final String TRANSLATING_PROMPT_EN = "prompt/translations/translation-prompt-en.txt";
    private static final String TRANSLATING_PROMPT_ZH = "prompt/translations/translation-prompt-zh.txt";
    private static final String NORMALIZATION_PROMPT_EN = "prompt/translations/normalization-prompt-en.txt";
    private static final String NORMALIZATION_PROMPT_ZH = "prompt/translations/normalization-prompt-zh.txt";
    private static final String FORMATTING_PROMPT = "prompt/translations/formatting-prompt.txt";
    private static final String USER_PROMPT = "prompt/user-prompt.txt";

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
        TranslationConfig titleConfig = TranslationConfig.builder()
            .request(request).payload(request.getArticleTitle()).locale(Locale.ENGLISH)
            .stage(Stage.TRANSLATING).stage(Stage.NORMALIZATION)
            .build();

        TranslationConfig titleZhConfig = TranslationConfig.builder()
            .request(request).payload(request.getArticleTitle()).locale(Locale.SIMPLIFIED_CHINESE)
            .stage(Stage.TRANSLATING).stage(Stage.NORMALIZATION)
            .build();

        TranslationConfig contentConfig = TranslationConfig.builder()
            .request(request).payload(request.getArticleContent()).locale(Locale.ENGLISH)
            .stage(Stage.TRANSLATING).stage(Stage.NORMALIZATION).stage(Stage.FORMATTING)
            .build();

        TranslationConfig contentZhConfig = TranslationConfig.builder()
            .request(request).payload(request.getArticleContent()).locale(Locale.SIMPLIFIED_CHINESE)
            .stage(Stage.TRANSLATING).stage(Stage.NORMALIZATION).stage(Stage.FORMATTING)
            .build();

        AIPipelineResponse titleEn = exec(titleConfig);
        AIPipelineResponse titleZh = exec(titleZhConfig);
        AIPipelineResponse contentEn = exec(contentConfig);
        AIPipelineResponse contentZh = exec(contentZhConfig);

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
     * @param config translation config with all parameters
     * @return response from the pipeline
     */
    private AIPipelineResponse exec(TranslationConfig config) {
        String translationPrompt = config.locale() == Locale.ENGLISH ? TRANSLATING_PROMPT_EN : TRANSLATING_PROMPT_ZH;
        String normalizingPrompt = config.locale() == Locale.ENGLISH ? NORMALIZATION_PROMPT_EN : NORMALIZATION_PROMPT_ZH;

        AiPipelineBuilder builder = AiPipelineBuilder.createBuilder(PIPELINE_NAME);

        for (Stage stage : config.stages()) {
            switch (stage) {
                case TRANSLATING:
                    builder.addStage(
                        config.request().getArticleId(), "TRANSLATING", defaultAiExecutor,
                        translationPrompt, USER_PROMPT, config.payload(), "", temperature, maxCountAttempts
                    );
                    break;
                case NORMALIZATION:
                    builder.addStage(
                        config.request().getArticleId(), "NORMALIZATION STAGE", defaultAiExecutor,
                        normalizingPrompt, USER_PROMPT, "", temperature, maxCountAttempts
                    );
                    break;
                case FORMATTING:
                    builder.addStage(
                        config.request().getArticleId(), "FORMATTING STAGE", defaultAiExecutor,
                        FORMATTING_PROMPT, USER_PROMPT, "", temperature, maxCountAttempts
                    );
                    break;
            }
        }

        AiPipelineRequest pipelineRequest = builder.build();

        AIPipelineResponse pipelineResponse = aiPipelineExecutor.executePipeline(pipelineRequest);

        if (!pipelineResponse.isSuccess()) {
            return AIPipelineResponse.failure(PIPELINE_NAME, pipelineResponse.getFailureReason());
        }

        return AIPipelineResponse.success(PIPELINE_NAME, pipelineResponse.getPayload());
    }

    public enum Stage {
        TRANSLATING, NORMALIZATION, FORMATTING
    }

    @Builder
    private record TranslationConfig(AiTranslationRequest request, String payload, Locale locale, @Singular List<Stage> stages) {
    }
}
