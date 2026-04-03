package com.huawei.ai_platform.lock.infrastructure.persistence.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.ai_platform.lock.infrastructure.persistence.model.LockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * DAO layer for the Lock entity
 *
 * @author Borodulin Artem
 * @since 2026.03.30
 */
@Mapper
public interface LockDao extends BaseMapper<LockEntity> {
    /**
     * Extracts status for the provided category
     *
     * @param category category
     * @return true/false
     */
    @Select("SELECT db.locked from db_lock db where db.lock_type = #{lockType}")
    Boolean isLockedFor(@Param("lockType") String category);

    /**
     * Extracts data by lock type
     * @param lockType lock type
     * @return Lock entity
     */
    @Select("SELECT db.* from db_lock db where db.lock_type = #{lockType}")
    LockEntity getAllByLockTypeEntity(@Param("lockType") String lockType);

    /**
     * Performs releasing locks
     */
    @Update("UPDATE db_lock SET locked = false")
    void releaseLocks();
}
