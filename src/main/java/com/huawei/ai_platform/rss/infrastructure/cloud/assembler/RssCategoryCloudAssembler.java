package com.huawei.ai_platform.rss.infrastructure.cloud.assembler;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssCategoryCloud;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryEntity;
import jakarta.annotation.Nonnull;
import org.mapstruct.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * Assembler to cloud structure
 *
 * @author Borodulin Artem
 * @since 2026.03.10
 */
@Mapper(componentModel = "spring")
public abstract class RssCategoryCloudAssembler {
    /**
     * Converts from one structure to another
     *
     * @param input input list. Should be not null
     * @return List of converted structures
     */
    public abstract List<RssCategoryCloud> convert(Collection<RssCategoryEntity> input);
}
