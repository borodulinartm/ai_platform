package com.huawei.ai_platform.rss.infrastructure.cloud.assembler;

import com.huawei.ai_platform.rss.infrastructure.cloud.model.RssFeedCloud;
import com.huawei.ai_platform.rss.model.RssFeed;
import org.mapstruct.Mapper;

import java.util.Collection;

/**
 * Assembler for the RSS feed
 *
 * @author Borodulin Artem b60078502
 * @since 2026.03.10
 */
@Mapper(componentModel = "spring")
public abstract class RssFeedCloudAssembler {
    /**
     * Performs converting from the aggregate to the cloud section
     *
     * @param input collection of an input data
     * @return collection of cloud items
     */
    public abstract Collection<RssFeedCloud> convert(Collection<RssFeed> input);
}
