package com.huawei.ai_platform.rss.infrastructure.ai.model.validation;

import com.huawei.ai_platform.common.OperationResultEnum;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.huawei.ai_platform.common.OperationResultEnum.FAILURE;
import static com.huawei.ai_platform.common.OperationResultEnum.SUCCESS;

/**
 * Validation request wrapper file
 *
 * @author Borodulin Artem
 * @since 2026.04.15
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationResponse {
    @Getter(value = AccessLevel.NONE)
    private OperationResultEnum status;

    private List<String> errors;

    /**
     * Checks status for true/false
     *
     * @return true if failed status, false otherwise
     */
    public boolean isFailed() {
        return status == FAILURE;
    }

    /**
     * Checks status for true/false
     *
     * @return true if success, false otherwise
     */
    public boolean isSuccess() {
        return status == SUCCESS;
    }

    // Static factory block

    /**
     * Failure factory method
     *
     * @param reason reason
     * @return Response
     */
    public static ValidationResponse failure(String reason) {
        List<String> reasonList = new ArrayList<>();
        reasonList.add(reason);

        return new ValidationResponse(FAILURE, reasonList);
    }

    /**
     * Success validation status
     *
     * @return validation ok
     */
    public static ValidationResponse success() {
        return new ValidationResponse(SUCCESS, Collections.emptyList());
    }
}
