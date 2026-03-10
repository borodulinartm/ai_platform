package com.huawei.ai_platform.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;

/**
 * OperationResult Enumeration
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@Getter
@RequiredArgsConstructor
public enum OperationResultEnum {
    SUCCESS(Level.INFO),
    FAILURE(Level.ERROR);

    private final Level logLevel;
}
