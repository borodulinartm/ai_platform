package com.huawei.ai_platform.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

/**
 * Constant utils class
 *
 * @author Borodulin Artem
 * @since 2026.03.07
 */
@NoArgsConstructor(access = AccessLevel.NONE)
@AllArgsConstructor(access = AccessLevel.NONE)
public class Constant {
    public static final ZoneId ZONE = ZoneId.of("GMT");
}
