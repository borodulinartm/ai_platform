package com.huawei.ai_platform.rss.infrastructure.persistence.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Attribute type enumeration for the RSS
 *
 * @author Borodulin Artem
 * @since 2026.04.13
 */
@RequiredArgsConstructor
@Getter
public enum RssAttributeTypeEnum {
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png");

    @JsonValue
    private final String textName;

    /**
     * Static factory
     *
     * @param value for which value do you want
     * @return Enumerayion attribute
     */
    @JsonCreator
    public static RssAttributeTypeEnum of(String value) {
        Map<String, RssAttributeTypeEnum> mapData = Stream.of(RssAttributeTypeEnum.values())
                .collect(Collectors.toMap(RssAttributeTypeEnum::getTextName, Function.identity(), (a, b) -> a));

        return mapData.get(value);
    }
}
