package com.huawei.ai_platform.config.batis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssAttributeValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handling for the attribute
 *
 * @author Borodulin Artem
 * @since 2026.04.13
 */
@Slf4j
public class RssAttributeTypeHandler extends BaseTypeHandler<RssAttributeValue> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RssAttributeValue parameter, JdbcType jdbcType)
                throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            log.error("Error during serialization: {}", e.getMessage());
        }
    }

    @Override
    public RssAttributeValue getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toValue(rs.getString(columnName));
    }

    @Override
    public RssAttributeValue getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toValue(rs.getString(columnIndex));
    }

    @Override
    public RssAttributeValue getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toValue(cs.getString(columnIndex));
    }

    private RssAttributeValue toValue(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }

        try {
            return mapper.readValue(input, RssAttributeValue.class);
        } catch (JsonProcessingException e) {
            log.error("An error has occurred during deserialization: {}", e.getMessage());
        }

        return null;
    }
}
