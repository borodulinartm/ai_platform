package com.huawei.ai_platform.rss.application.service;

import com.huawei.ai_platform.common.OperationResult;

/**
 * Input port for the translation
 *
 * @author Borodulin Artem
 * @since 2026.03.20
 */
public interface RssTranslationService {
    /**
     * Enables translation
     *
     * @return OperationResult: success/failure
     */
    OperationResult performTranslate();
}
