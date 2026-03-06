package com.huawei.freshrss_news_upload.utils;

import lombok.*;
import lombok.experimental.SuperBuilder;

import static com.huawei.freshrss_news_upload.utils.OperationResultEnum.FAILURE;

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
}
