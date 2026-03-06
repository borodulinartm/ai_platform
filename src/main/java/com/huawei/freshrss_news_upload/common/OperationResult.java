package com.huawei.freshrss_news_upload.common;

import lombok.*;
import lombok.experimental.SuperBuilder;

import static com.huawei.freshrss_news_upload.common.OperationResultEnum.FAILURE;
import static com.huawei.freshrss_news_upload.common.OperationResultEnum.SUCCESS;

/**
 * Wrapper class for the result of an Operation
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class OperationResult {
    private OperationResultEnum state;

    @Getter(value = AccessLevel.PRIVATE)
    private String reason;

    /**
     * Reason with decorator
     *
     * @return info
     */
    public String getInfo() {
        if (state == FAILURE) {
            return "Operation FAILED: " + reason;
        }

        return "Operation SUCCESS: " + reason;
    }

    /**
     * Helpful method to check whether it fail or not
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailed() {
        return state == FAILURE;
    }

    /**
     * Helpful method to check whether it success or not
     *
     * @return true if success, false otherwise
     */
    public boolean isSuccess() {
        return state == SUCCESS;
    }
}
