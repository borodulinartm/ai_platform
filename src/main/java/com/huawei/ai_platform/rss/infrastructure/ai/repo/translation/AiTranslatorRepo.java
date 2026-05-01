package com.huawei.ai_platform.rss.infrastructure.ai.repo.translation;

import com.huawei.ai_platform.rss.enums.AiTranslatingStagesEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslatingPipelineProperties;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.translation.AiTranslationResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipeline;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.factory.AiUnaryStageFactory;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.huawei.ai_platform.rss.enums.AiTranslatingStagesEnum.FORMATTING_STAGE;

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

    private static final AiTypedKey<String> TRANSLATING_STAGE_INPUT = AiTypedKey.of(String.class, "TRANSLATING_STAGE_INPUT");
    private static final AiTypedKey<String> TRANSLATING_STAGE_OUTPUT = AiTypedKey.of(String.class, "TRANSLATING_STAGE_OUTPUT");
    private static final AiTypedKey<String> NORMALIZATION_STAGE_OUTPUT = AiTypedKey.of(String.class, "NORMALIZATION_STAGE_OUTPUT");
    private static final AiTypedKey<String> FORMATTING_STAGE_OUTPUT = AiTypedKey.of(String.class, "FORMATTING_STAGE_OUTPUT");

    private final AiFunction1Executor<String, String> defaultAiExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;
    private final IAiStageValidation<String, String> aiDefaultValidator;
    private final IAiStageValidation<String, String> aiSizeValidator;
    private final AiTranslatingPipelineProperties aiTranslatingPipelineProperties;

    /**
     * Performs translation
     *
     * @param request article that need to request
     * @return response data
     */
    public AiTranslationResponse translate(@Nonnull AiTranslationRequest request) {
        AIPipelineResponse<String> titleEn = exec(request, request.getArticleTitle(), Locale.ENGLISH, false);
        AIPipelineResponse<String> titleZh = exec(request, request.getArticleTitle(), Locale.SIMPLIFIED_CHINESE, false);
        AIPipelineResponse<String> contentEn = exec(request, request.getArticleContent(), Locale.ENGLISH, true);
        AIPipelineResponse<String> contentZh = exec(request, request.getArticleContent(), Locale.SIMPLIFIED_CHINESE, true);

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
     * @param request           cleaning request
     * @param payload           payload
     * @param includeFormatting include formatting or not?
     * @return response from the pipeline
     */
    private AIPipelineResponse<String> exec(AiTranslationRequest request, String payload, Locale locale,
                                            boolean includeFormatting) {
        AiPipelineBuilder<String, String> aiPipelineBuilder = AiPipelineBuilder.withName(PIPELINE_NAME, TRANSLATING_STAGE_INPUT, FORMATTING_STAGE_OUTPUT);
        List<AiTranslatingPipelineProperties.StageParams> params = aiTranslatingPipelineProperties.getStages();

        for (AiTranslatingPipelineProperties.StageParams param : params) {
            // Skip formatting if not required
            if (param.getStageName() == FORMATTING_STAGE) {
                if (!includeFormatting) {
                    continue;
                }
            }

            AiStageParameters aiStageParameters = new AiStageParameters(param.getStageName().name(), request.getArticleId(),
                    param.getSystemPrompt(locale), param.getUserPromptPath(), param.getModel(), param.getTemperature(),
                    param.getCountAttempts()
            );

            if (param.getValidation() != null) {
                AiStageParameters aiValidationData = new AiStageParameters(param.getValidation().getStageName().name(), request.getArticleId(),
                        param.getValidation().getSystemPrompt(locale), param.getValidation().getUserPromptPath(), param.getValidation().getModel(),
                        param.getValidation().getTemperature(),
                        param.getValidation().getCountAttempts()
                );

                aiPipelineBuilder.addStage(
                        new AiUnaryStageFactory().createStage(
                                param.getStageName().name(), getInputTypeKeyByStageName(param.getStageName()), getOutputTypeKeyByStageName(param.getStageName()),
                                aiStageParameters, defaultAiExecutor, aiDefaultValidator, aiValidationData
                        )
                );
            } else {
                aiPipelineBuilder.addStage(
                        new AiUnaryStageFactory().createStage(
                                param.getStageName().name(), getInputTypeKeyByStageName(param.getStageName()),
                                getOutputTypeKeyByStageName(param.getStageName()),
                                aiStageParameters, defaultAiExecutor, aiSizeValidator, null
                        )
                );
            }
        }
        AiPipeline<String, String> aiPipeline = aiPipelineBuilder.build();

        return aiPipelineExecutor.executePipeline(aiPipeline, payload);
    }

    private AiTypedKey<String> getInputTypeKeyByStageName(@Nonnull AiTranslatingStagesEnum stagesEnum) {
        switch (stagesEnum) {
            case TRANSLATION_STAGE -> {
                return TRANSLATING_STAGE_INPUT;
            }

            case NORMALIZATION_STAGE -> {
                return TRANSLATING_STAGE_OUTPUT;
            }

            case FORMATTING_STAGE -> {
                return NORMALIZATION_STAGE_OUTPUT;
            }
        }

        throw new IllegalArgumentException("Not valid input key");
    }

    private AiTypedKey<String> getOutputTypeKeyByStageName(@Nonnull AiTranslatingStagesEnum stagesEnum) {
        switch (stagesEnum) {
            case TRANSLATION_STAGE -> {
                return TRANSLATING_STAGE_OUTPUT;
            }

            case NORMALIZATION_STAGE -> {
                return NORMALIZATION_STAGE_OUTPUT;
            }

            case FORMATTING_STAGE -> {
                return FORMATTING_STAGE_OUTPUT;
            }
        }

        throw new IllegalArgumentException("Not valid input key");
    }
}
