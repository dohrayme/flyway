/*
 * Copyright 2010-2017 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.database.redshift;

import org.flywaydb.core.internal.database.Delimiter;
import org.flywaydb.core.internal.sqlscript.SqlStatement;
import org.flywaydb.core.internal.database.SqlStatementBuilder;
import org.flywaydb.core.internal.sqlscript.StandardSqlStatement;
import org.flywaydb.core.internal.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SqlStatementBuilder supporting PostgreSQL specific syntax.
 */
public class RedshiftSqlStatementBuilder extends SqlStatementBuilder {
    /**
     * Matches $$, $BODY$, $xyz123$, ...
     */
    /*private -> for testing*/
    static final String DOLLAR_QUOTE_REGEX = "(\\$[A-Za-z0-9_]*\\$).*";

    /**
     * Holds the beginning of the statement.
     */
    private String statementStart = "";

    RedshiftSqlStatementBuilder(Delimiter defaultDelimiter) {
        super(defaultDelimiter);
    }

    /**
     * @return The assembled statement, with the delimiter stripped off.
     */
    @Override
    public SqlStatement getSqlStatement() {
        return new StandardSqlStatement(lineNumber, statement.toString());
    }

    @Override
    protected void applyStateChanges(String line) {
        super.applyStateChanges(line);

        if (!executeInTransaction) {
            return;
        }

        if (StringUtils.countOccurrencesOf(statementStart, " ") < 8) {
            statementStart += line;
            statementStart += " ";
            statementStart = statementStart.replaceAll("\\s+", " ");
        }

        if (statementStart.matches("^(CREATE|DROP) LIBRARY .*")
                || statementStart.matches("^CREATE EXTERNAL TABLE .*")
                || statementStart.matches("^ALTER TABLE .* APPEND FROM .*")
                || statementStart.matches("^VACUUM .*")
                ) {
            executeInTransaction = false;
        }
    }

    @Override
    protected String[] tokenizeLine(String line) {
        return StringUtils.tokenizeToStringArray(line, " @<>;:=|(),+{}\\[\\]");
    }

    @Override
    protected String extractAlternateOpenQuote(String token) {
        Matcher matcher = Pattern.compile(DOLLAR_QUOTE_REGEX).matcher(token);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    protected String cleanToken(String token) {
        if (token.startsWith("E'")) {
            return token.substring(token.indexOf("'"));
        }

        return token;
    }
}