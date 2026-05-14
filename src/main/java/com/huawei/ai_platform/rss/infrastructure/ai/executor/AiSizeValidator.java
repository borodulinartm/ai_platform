package com.huawei.ai_platform.rss.infrastructure.ai.executor;

import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.enums.AiResultEnum;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.exec.IAiStageValidation;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.AiResultText;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageParameters;
import com.huawei.ai_platform.rss.infrastructure.ai.pipeline.model.stage.AiStageValidationResult;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI validator by length of content
 *
 * @author Borodulin Artem
 * @since 2026.05.01
 */
@Component("aiSizeValidator")
@RequiredArgsConstructor
public class AiSizeValidator implements IAiStageValidation<String, String> {
    private static final String ERROR_TEXT = "Text too short. Not valid";

    @Override
    public @Nonnull AiStageValidationResult validateStage(@Nonnull AiResultText<String> inputData, @Nonnull AiResultText<String> resultData,
                                                          AiStageParameters parameters) {
        int lengthInput = inputData.getText().length();
        int lengthOutput = resultData.getText().length();
        double ratio = (double) lengthOutput / lengthInput;

        if (inputData.getResultEnum() == AiResultEnum.FAILURE || resultData.getResultEnum() == AiResultEnum.FAILURE) {
            return AiStageValidationResult.failure("Not all parameters are successfully");
        }

        // No content - no need check
        if (resultData.getResultEnum() == AiResultEnum.NO_CONTENT) {
            return AiStageValidationResult.success();
        }

        // Idea: if the text is too short, then we consider as a failure (because of the not good situation)
        if (lengthInput < 25) {
            if (ratio < 0.25) {
                return AiStageValidationResult.failure(ERROR_TEXT);
            }
        } else if (lengthInput < 100) {
            if (ratio < 0.1) {
                return AiStageValidationResult.failure(ERROR_TEXT);
            }
        } else if (lengthInput < 200) {
            if (ratio < 0.06) {
                return AiStageValidationResult.failure(ERROR_TEXT);
            }
        } else {
            if (ratio < 0.03) {
                return AiStageValidationResult.failure(ERROR_TEXT);
            }
        }

        return AiStageValidationResult.success();
    }
}
