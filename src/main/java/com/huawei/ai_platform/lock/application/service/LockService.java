package com.huawei.ai_platform.lock.application.service;

import com.huawei.ai_platform.lock.infrastructure.persistence.repo.LockRepo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Service class layer for the locking
 *
 * @author Borodulin Artem b6007802
 * @since 2026.03.30
 */
@Service
@RequiredArgsConstructor
public class LockService {
    private final LockRepo lockRepo;

    /**
     * Predicate for testing whether it's ok or not
     *
     * @param category category
     * @return true/false
     */
    public boolean isLockedFor(String category) {
        if (StringUtils.isBlank(category)) {
            throw new IllegalArgumentException("Category must be not blank");
        }

        return lockRepo.isLockedFor(category);
    }

    /**
     * Performs locking
     *
     * @param category category
     */
    public void lockFor(String category) {
        if (isLockedFor(category)) {
            throw new IllegalStateException("Cannot lock locked record");
        }

        lockRepo.lock(category);
    }

    /**
     * Unlocks data
     *
     * @param category category
     */
    public void unlockFor(String category) {
        if (!isLockedFor(category)) {
            throw new IllegalStateException("Cannot unlock unlocked record");
        }

        lockRepo.unlock(category);
    }
}
