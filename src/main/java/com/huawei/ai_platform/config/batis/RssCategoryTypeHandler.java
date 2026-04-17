package com.huawei.ai_platform.config.batis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.ai_platform.rss.infrastructure.persistence.entity.RssCategoryAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * RSS category type handler. Uses to correctly convert attributes field from the category side
 *
 * @author Borodoulin Artem
 * @since 2026.04.17
 */
@Slf4j
public class RssCategoryTypeHandler extends BaseTypeHandler<RssCategoryAttribute> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RssCategoryAttribute parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (JsonProcessingException exception) {
            log.error("Error processing RssCategoryAttribute class: {}", exception.getMessage());
        }
    }

    @Override
    public RssCategoryAttribute getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toValue(rs.getString(columnName));
    }

    @Override
    public RssCategoryAttribute getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toValue(rs.getString(columnIndex));
    }

    @Override
    public RssCategoryAttribute getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toValue(cs.getString(columnIndex));
    }

    /**
     * Performs converting from input side (string) to end value
     *
     * @param input input list item
     * @return Rss attribute value class
     */
    private RssCategoryAttribute toValue(String input) {
        try {
            if (StringUtils.isBlank(input) || input.toLowerCase(Locale.ENGLISH).equals("[]")) {
                return new RssCategoryAttribute(Integer.MAX_VALUE);
            }

            return mapper.readValue(input, RssCategoryAttribute.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting from string to json: {}", e.getMessage());
        }

        return new RssCategoryAttribute(Integer.MAX_VALUE);
    }
}
