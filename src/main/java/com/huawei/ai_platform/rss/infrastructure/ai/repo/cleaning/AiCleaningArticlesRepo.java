package com.huawei.ai_platform.rss.infrastructure.ai.repo.cleaning;

import com.huawei.ai_platform.rss.enums.AiCleaningStagesEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningPipelineProperties;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipeline;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.factory.AiUnaryStageFactory;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

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

    // AiTypedKeys
    private static final AiTypedKey<String> CLEANING_STAGE_INPUT = AiTypedKey.of(String.class, "CLEANING_STAGE_INPUT");
    private static final AiTypedKey<String> CLEANING_STAGE_OUTPUT = AiTypedKey.of(String.class, "CLEANING_STAGE_OUTPUT");
    private static final AiTypedKey<String> NOISE_REMOVING_STAGE_OUTPUT = AiTypedKey.of(String.class, "NOISE_REMOVING_STAGE_OUTPUT");
    private static final AiTypedKey<String> NORMALIZATION_STAGE_OUTPUT = AiTypedKey.of(String.class, "NORMALIZATION_STAGE_OUTPUT");

    private final AiFunction1Executor<String, String> defaultAiExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;
    private final IAiStageValidation<String, String> aiDefaultValidator;
    private final IAiStageValidation<String, String> aiSizeValidator;
    private final AiCleaningPipelineProperties cleaningPipelineProperties;

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

        AIPipelineResponse<String> pipelineResponse = exec(cleaningRequest, inputContent.toString());

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
    private AIPipelineResponse<String> exec(AiCleaningRequest request, String payload) {
        // first, building map for input and output values for the data

        AiPipelineBuilder<String, String> aiPipelineBuilder = AiPipelineBuilder.withName(PIPELINE_NAME, CLEANING_STAGE_INPUT, NORMALIZATION_STAGE_OUTPUT);
        List<AiCleaningPipelineProperties.StageParams> params = cleaningPipelineProperties.getStages();
        for (AiCleaningPipelineProperties.StageParams param : params) {
            AiStageParameters aiStageParameters = new AiStageParameters(param.getStageName().name(), request.getId(),
                    param.getSystemPromptPath(), param.getUserPromptPath(), param.getModel(), param.getTemperature(),
                    param.getCountAttempts()
            );

            if (param.getValidation() != null) {
                AiStageParameters aiValidationData = new AiStageParameters(param.getValidation().getStageName().name(), request.getId(),
                        param.getValidation().getSystemPromptPath(), param.getValidation().getUserPromptPath(), param.getValidation().getModel(),
                        param.getValidation().getTemperature(),
                        param.getValidation().getCountAttempts()
                );

                aiPipelineBuilder.addStage(
                        new AiUnaryStageFactory().createStage(
                                param.getStageName().name(), getInputTypeKeyByStageName(param.getStageName()),
                                getOutputTypeKeyByStageName(param.getStageName()),
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

    private AiTypedKey<String> getInputTypeKeyByStageName(@Nonnull AiCleaningStagesEnum stagesEnum) {
        switch (stagesEnum) {
            case CLEANING_STAGE -> {
                return CLEANING_STAGE_INPUT;
            }

            case NOISE_REMOVING_STAGE -> {
                return CLEANING_STAGE_OUTPUT;
            }

            case NORMALIZATION_STAGE -> {
                return NOISE_REMOVING_STAGE_OUTPUT;
            }
        }

        throw new IllegalArgumentException("Not valid input key");
    }

    private AiTypedKey<String> getOutputTypeKeyByStageName(@Nonnull AiCleaningStagesEnum stagesEnum) {
        switch (stagesEnum) {
            case CLEANING_STAGE -> {
                return CLEANING_STAGE_OUTPUT;
            }

            case NOISE_REMOVING_STAGE -> {
                return NOISE_REMOVING_STAGE_OUTPUT;
            }

            case NORMALIZATION_STAGE -> {
                return NORMALIZATION_STAGE_OUTPUT;
            }
        }

        throw new IllegalArgumentException("Not valid input key");
    }
}
