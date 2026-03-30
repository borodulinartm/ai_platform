package com.huawei.ai_platform.lock.infrastructure.persistence.repo;

import com.huawei.ai_platform.lock.infrastructure.persistence.dao.LockDao;
import com.huawei.ai_platform.lock.infrastructure.persistence.model.LockEntity;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repo layer for the lock side
 *
 * @author Borodulin Artem
 * @since 2026.03.30
 */
@Repository
@RequiredArgsConstructor
public class LockRepo {
    private final LockDao lockDao;

    /**
     * Checks data for locking status
     *
     * @param category category. Must be not null
     * @return true if locked, false otherwise
     */
    public boolean isLockedFor(@Nonnull String category) {
        Boolean result = lockDao.isLockedFor(category);
        return result != null && result;
    }

    /**
     * Lock record
     * @param category category
     */
    public void lock(@Nonnull String category) {
        LockEntity entity = lockDao.getAllByLockTypeEntity(category);

        // If we call at first time
        if (entity == null) {
            LockEntity lockEntity = new LockEntity(null, category, true);
            lockDao.insert(lockEntity);
        } else {
            entity.setLocked(true);
            lockDao.updateById(entity);
        }
    }

    /**
     * Unlocks data
     * @param category category
     */
    public void unlock(@Nonnull String category) {
        LockEntity entity = lockDao.getAllByLockTypeEntity(category);

        if (entity == null) {
            throw new IllegalStateException("Entity is null. For unlocking record should be in DB");
        } else {
            entity.setLocked(false);
            lockDao.updateById(entity);
        }
    }
}
