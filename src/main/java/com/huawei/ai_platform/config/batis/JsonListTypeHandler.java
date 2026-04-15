package com.huawei.ai_platform.config.batis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Basic class for the type handling
 *
 * @author Borodulin Artem
 * @since 2026.04.13
 * @param <T> type of the class
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(List.class)
@Slf4j
public class JsonListTypeHandler<T> extends BaseTypeHandler<List<T>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Class<T> tClass;

    public JsonListTypeHandler(Class<T> tClass) {
        this.tClass = tClass;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<T> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            log.error("Error during serialization: {}", e.getMessage());
        }
    }

    @Override
    public List<T> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getString(columnName));
    }

    @Override
    public List<T> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getString(columnIndex));
    }

    @Override
    public List<T> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getString(columnIndex));
    }

    private List<T> toList(String input) {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }

        try {
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, tClass);
            return objectMapper.readValue(input, javaType);
        } catch (IOException exception) {
            log.error("An error has occurred during deserializing data: {}", exception.getMessage());
            return Collections.emptyList();
        }
    }
}
