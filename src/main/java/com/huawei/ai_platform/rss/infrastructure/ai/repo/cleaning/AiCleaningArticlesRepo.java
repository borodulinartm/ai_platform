package com.huawei.ai_platform.rss.infrastructure.ai.repo.cleaning;

import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningRequest;
import com.huawei.ai_platform.rss.infrastructure.ai.model.cleaning.AiCleaningResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.driver.AiPipelineExecutor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.AiFunction1Executor;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AIPipelineResponse;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipeline;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiPipelineBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiTypedKey;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStage;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageBuilder;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
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
    public static final String USER_PROMPT = "prompt/user-prompt.txt";

    // AiTypedKeys
    private static final AiTypedKey<String> CLEANING_STAGE_INPUT = AiTypedKey.of(String.class, "CLEANING_STAGE_INPUT");

    private static final AiTypedKey<String> CLEANING_STAGE_OUTPUT = AiTypedKey.of(String.class, "CLEANING_STAGE_OUTPUT");
    private static final AiTypedKey<String> NOISE_REMOVING_STAGE_OUTPUT = AiTypedKey.of(String.class, "NOISE_REMOVING_STAGE_OUTPUT");
    private static final AiTypedKey<String> NORMALIZATION_STAGE_OUTPUT = AiTypedKey.of(String.class, "NORMALIZATION_STAGE_OUTPUT");

    private final AiFunction1Executor<String, String> defaultAiExecutor;
    private final AiPipelineExecutor aiPipelineExecutor;

    // AI cleaning parameters
    @Value("${ai.cleaning.removingHtml.countAttempts}")
    private int maxCountAttemptsCleaning;
    @Value("${ai.cleaning.removingHtml.temperature}")
    private Double temperatureCleaning;
    @Value("${ai.cleaning.removingHtml.model}")
    private String modelCleaning;

    // AI cleaning parameters
    @Value("${ai.cleaning.removingNoise.countAttempts}")
    private int maxCountAttemptsNoiseRemoving;
    @Value("${ai.cleaning.removingNoise.temperature}")
    private Double temperatureNoiseRemoving;
    @Value("${ai.cleaning.removingNoise.model}")
    private String modelNoiseRemoving;

    // AI cleaning parameters
    @Value("${ai.cleaning.normalization.countAttempts}")
    private int maxCountAttemptsNormalization;
    @Value("${ai.cleaning.normalization.temperature}")
    private Double temperatureNormalization;
    @Value("${ai.cleaning.normalization.model}")
    private String modelNormalization;

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

    private AiStage addCleaningStage(AiCleaningRequest cleaningRequest) {
        String cleaningHtmlPrompt = "prompt/cleaning/cleaning-prompt.txt";
        String cleaningStage = "CLEANING_STAGE";
        AiStageParameters stageParameters = new AiStageParameters(cleaningStage,
                cleaningRequest.getId(), cleaningHtmlPrompt, USER_PROMPT, modelCleaning, temperatureCleaning, maxCountAttemptsCleaning
        );

        return AiStageBuilder.with1Parameter(cleaningStage, CLEANING_STAGE_INPUT, CLEANING_STAGE_OUTPUT, stageParameters,
                defaultAiExecutor
        );
    }

    private AiStage addNoiseRemovingStage(AiCleaningRequest cleaningRequest) {
        String noisePrompt = "prompt/cleaning/noise-removing-prompt.txt";
        String stageName = "NOISE_REMOVING_STAGE";

        AiStageParameters stageParameters = new AiStageParameters(stageName,
                cleaningRequest.getId(), noisePrompt, USER_PROMPT, modelNoiseRemoving, temperatureNoiseRemoving, maxCountAttemptsNoiseRemoving
        );

        return AiStageBuilder.with1Parameter(stageName, CLEANING_STAGE_OUTPUT, NOISE_REMOVING_STAGE_OUTPUT, stageParameters,
                defaultAiExecutor
        );
    }

    private AiStage addNormalizationStage(AiCleaningRequest cleaningRequest) {
        String normalizingPrompt = "prompt/cleaning/normalization-prompt.txt";
        String stageName = "NORMALIZATION_STAGE";

        AiStageParameters stageParameters = new AiStageParameters(stageName,
                cleaningRequest.getId(), normalizingPrompt, USER_PROMPT, modelNormalization, temperatureNormalization, maxCountAttemptsNormalization
        );

        return AiStageBuilder.with1Parameter(stageName, NOISE_REMOVING_STAGE_OUTPUT, NORMALIZATION_STAGE_OUTPUT, stageParameters,
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
    private AIPipelineResponse<String> exec(AiCleaningRequest request, String payload) {
        AiPipeline<String, String> pipeline = AiPipelineBuilder.withName(PIPELINE_NAME, CLEANING_STAGE_INPUT,
                NORMALIZATION_STAGE_OUTPUT
        ).addStage(addCleaningStage(request)).addStage(addNoiseRemovingStage(request)).addStage(addNormalizationStage(request))
                .build();

        return aiPipelineExecutor.executePipeline(pipeline, payload);
    }
}
