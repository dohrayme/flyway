/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * INTERNAL RELEASE. ALL RIGHTS RESERVED.
 *
 * Must
 * be
 * exactly
 * 13 lines
 * to match
 * community
 * edition
 * license
 * length.
 */
package org.flywaydb.core.internal.database.oracle.pro;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.database.AbstractSqlStatement;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.jdbc.ContextImpl;
import org.flywaydb.core.internal.util.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.util.jdbc.Result;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A SQL*Plus SHOW statement.
 */
public class SQLPlusShowSqlStatement extends AbstractSqlStatement {
    private static final Log LOG = LogFactory.getLog(SQLPlusShowSqlStatement.class);

    public SQLPlusShowSqlStatement(int lineNumber, String sql) {
        super(lineNumber, sql);
    }

    @Override
    public List<Result> execute(ContextImpl errorContext, JdbcTemplate jdbcTemplate) throws SQLException {
        String option = sql.substring(sql.indexOf(" ") + 1).toUpperCase(Locale.ENGLISH);
        if ("CON_ID".equals(option)) {
            conId(jdbcTemplate);
        } else if ("EDITION".equals(option)) {
            edition(jdbcTemplate);
        } else if (option.startsWith("ERR")) {
            err(jdbcTemplate, option);
        } else if (option.startsWith("REL")) {
            rel(jdbcTemplate);
        } else if (option.startsWith("USER")) {
            user(jdbcTemplate);
        } else {
            LOG.warn("Unknown option for SHOW: " + option);
        }
        return new ArrayList<Result>();
    }

    private void conId(JdbcTemplate jdbcTemplate) throws SQLException {
        LOG.info("CON_ID\n------------------------------\n"
                + jdbcTemplate.queryForString("SELECT CON_ID FROM V$VERSION WHERE BANNER LIKE 'Oracle Database%'"));
    }

    private void edition(JdbcTemplate jdbcTemplate) throws SQLException {
        if (jdbcTemplate.getConnection().getMetaData().getDatabaseMajorVersion() < 11) {
            LOG.warn("SP2-0614: Server version too low for this feature\n" +
                    "SP2-1539: Edition requires Oracle Database 11g or later.");
        } else {
            LOG.info("EDITION\n------------------------------\n"
                    + jdbcTemplate.queryForString(
                    "SELECT property_value FROM database_properties\n" +
                            "WHERE  property_name = 'DEFAULT_EDITION'"));
        }
    }

    private void err(JdbcTemplate jdbcTemplate, String option) throws SQLException {
        String query = "SELECT TYPE,NAME,LINE,POSITION,TEXT FROM USER_ERRORS";
        String orderBy = " ORDER BY SEQUENCE";

        List<Map<String, String>> result;
        if (option.matches("(ERR|ERRORS)")) {
            result = jdbcTemplate.queryForList(query + orderBy);
        } else {
            String[] typeName = StringUtils.tokenizeToStringArray(option.substring(option.indexOf(" ") + 1), " ");
            result = jdbcTemplate.queryForList(query + " WHERE TYPE=? AND NAME=?" + orderBy, typeName);
        }
        if (result.isEmpty()) {
            LOG.info("No errors.");
        } else {
            StringBuilder output = new StringBuilder(
                    "Errors for " + result.get(0).get("TYPE") + " " + result.get(0).get("NAME") + ":\n\n" +
                            "LINE/COL ERROR\n" +
                            "-------- -----------------------------------------------------------------\n");
            for (Map<String, String> row : result) {
                output.append(StringUtils.trimOrPad(row.get("LINE") + "/" + row.get("POSITION"), 8))
                        .append(" ").append(row.get("TEXT").replace("\n", "\n         ")).append("\n");
            }
            LOG.info(output.toString().substring(0, output.length() - 1));
        }
    }

    private void rel(JdbcTemplate jdbcTemplate) throws SQLException {
        LOG.info("release "
                + jdbcTemplate.queryForString(
                "SELECT VERSION FROM PRODUCT_COMPONENT_VERSION WHERE PRODUCT LIKE 'Oracle Database%'")
                .replace(".", "0"));
    }

    private void user(JdbcTemplate jdbcTemplate) throws SQLException {
        LOG.info("USER is \"" + jdbcTemplate.getConnection().getMetaData().getUserName() + "\"");
    }
}
