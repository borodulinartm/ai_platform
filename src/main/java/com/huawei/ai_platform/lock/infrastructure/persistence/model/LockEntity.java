package com.huawei.ai_platform.lock.infrastructure.persistence.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "db_lock")
public class LockEntity {
    @TableId
    private Long lockId;
    private String lockType;
    private boolean locked;
}
