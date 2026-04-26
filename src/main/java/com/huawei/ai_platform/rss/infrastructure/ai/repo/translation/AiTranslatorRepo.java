package com.huawei.ai_platform.rss.infrastructure.ai.repo.translation;

import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipeline;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
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
    private static final String PIPELINE_NAME = "TRANSLATING";
    private static final String USER_PROMPT = "prompt/user-prompt.txt";

    private static final AiTypedKey<String> TRANSLATING_STAGE_INPUT = AiTypedKey.of(String.class, "TRANSLATING_STAGE_INPUT");
    private static final AiTypedKey<String> TRANSLATING_STAGE_OUTPUT = AiTypedKey.of(String.class, "TRANSLATING_STAGE_OUTPUT");
    private static final AiTypedKey<String> NORMALIZATION_STAGE_OUTPUT = AiTypedKey.of(String.class, "NORMALIZATION_STAGE_OUTPUT");


    private final AiFunction1Executor<String, String> defaultAiExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;

    @Value("${ai.translating.translatingStage.countAttempts}")
    private int maxCountAttemptsTranslatingStage;
    @Value("${ai.translating.translatingStage.temperature}")
    private Double temperatureTranslatingStage;
    @Value("${ai.translating.translatingStage.model}")
    private String modelTranslatingStage;

    @Value("${ai.translating.normalizationStage.countAttempts}")
    private int maxCountAttemptsNormalizationStage;
    @Value("${ai.translating.normalizationStage.temperature}")
    private Double temperatureNormalizationStage;
    @Value("${ai.translating.normalizationStage.model}")
    private String modelNormalizationStage;

    /**
     * Performs translation
     *
     * @param request article that need to request
     * @return response data
     */
    public AiTranslationResponse translate(@Nonnull AiTranslationRequest request) {
        AIPipelineResponse<String> titleEn = exec(request, request.getArticleTitle(), Locale.ENGLISH);
        AIPipelineResponse<String> titleZh = exec(request, request.getArticleTitle(), Locale.SIMPLIFIED_CHINESE);
        AIPipelineResponse<String> contentEn = exec(request, request.getArticleContent(), Locale.ENGLISH);
        AIPipelineResponse<String> contentZh = exec(request, request.getArticleContent(), Locale.SIMPLIFIED_CHINESE);

        if (!(titleEn.isSuccess() && titleZh.isSuccess() && contentEn.isSuccess() && contentZh.isSuccess())) {
            return AiTranslationResponse.failureResponse(request.getArticleId(), "Not translated :(");
        }

        return AiTranslationResponse.successResponse(request.getArticleId(),
                titleEn.getPayload(), titleZh.getPayload(), contentEn.getPayload(), contentZh.getPayload()
        );
    }

    private AiStage addTranslatingStage(AiTranslationRequest cleaningRequest, Locale locale) {
        String translationPrompt = locale == Locale.ENGLISH ? "prompt/translations/translation-prompt-en.txt" :
                "prompt/translations/translation-prompt-zh.txt";
        String stageName = "NORMALIZATION_STAGE";

        AiStageParameters stageParameters = new AiStageParameters(stageName,
                cleaningRequest.getArticleId(), translationPrompt, USER_PROMPT, modelTranslatingStage,
                temperatureTranslatingStage, maxCountAttemptsTranslatingStage
        );

        return AiStageBuilder.with1Parameter(stageName, TRANSLATING_STAGE_INPUT, TRANSLATING_STAGE_OUTPUT, stageParameters,
                defaultAiExecutor
        );
    }

    private AiStage addNormalizationStage(AiTranslationRequest cleaningRequest, Locale locale) {
        String normalizingPrompt = locale == Locale.ENGLISH ? "prompt/translations/normalization-prompt-en.txt" :
                "prompt/translations/normalization-prompt-zh.txt";
        String stageName = "NORMALIZATION_STAGE";

        AiStageParameters stageParameters = new AiStageParameters(stageName,
                cleaningRequest.getArticleId(), normalizingPrompt, USER_PROMPT, modelNormalizationStage,
                temperatureNormalizationStage, maxCountAttemptsNormalizationStage
        );

        return AiStageBuilder.with1Parameter(stageName, TRANSLATING_STAGE_OUTPUT, NORMALIZATION_STAGE_OUTPUT, stageParameters,
                defaultAiExecutor
        );
    }

    /**
     * Performs executing data in the pipeline side
     *
     * @param request cleaning request
     * @param payload payload
     * @return response from the pipeline
     */
    private AIPipelineResponse<String> exec(AiTranslationRequest request, String payload, Locale locale) {
        AiPipeline<String, String> pipeline = AiPipelineBuilder.withName(PIPELINE_NAME,
                TRANSLATING_STAGE_INPUT, NORMALIZATION_STAGE_OUTPUT
        ).addStage(addTranslatingStage(request, locale)).addStage(addNormalizationStage(request, locale)).build();

        return aiPipelineExecutor.executePipeline(pipeline, payload);
    }
}
