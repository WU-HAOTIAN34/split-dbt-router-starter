package com.wht.sdt.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL表名替换工具类
 * 使用JSqlParser进行SQL解析，支持复杂SQL（JOIN、子查询等）
 * 当JSqlParser解析失败时，降级使用正则表达式
 *
 * @author wht
 */
@Slf4j
public class SqlTableReplacer {

    // 降级使用的正则表达式
    private static final Pattern REGEX_PATTERN = Pattern.compile(
            "(from|into|update|join)\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE);

    /**
     * 替换SQL中的表名
     *
     * @param originalSql  原始SQL
     * @param tableConfigs 表配置（表名 -> 分表数量）
     * @param tableSuffix  表后缀（如：001, 002）
     * @return 替换后的SQL
     */
    public static String replaceTableName(String originalSql,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        if (originalSql == null || originalSql.isEmpty()) {
            return originalSql;
        }

        try {
            // 尝试使用JSqlParser解析
            return replaceTableNameWithParser(originalSql, tableConfigs, tableSuffix);
        } catch (Exception e) {
            log.warn("JSqlParser failed to parse SQL, fallback to regex. Error: {}", e.getMessage());
            // 降级使用正则表达式
            return replaceTableNameWithRegex(originalSql, tableConfigs, tableSuffix);
        }
    }

    /**
     * 使用JSqlParser替换表名（推荐方式）
     */
    private static String replaceTableNameWithParser(String sql,
            Map<String, Integer> tableConfigs,
            String tableSuffix) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);

        if (statement instanceof Select) {
            handleSelect((Select) statement, tableConfigs, tableSuffix);
        } else if (statement instanceof Insert) {
            handleInsert((Insert) statement, tableConfigs, tableSuffix);
        } else if (statement instanceof Update) {
            handleUpdate((Update) statement, tableConfigs, tableSuffix);
        } else if (statement instanceof Delete) {
            handleDelete((Delete) statement, tableConfigs, tableSuffix);
        }

        String modifiedSql = statement.toString();
        log.debug("SQL replaced by JSqlParser: {} -> {}", sql, modifiedSql);

        return modifiedSql;
    }

    /**
     * 处理SELECT语句
     */
    private static void handleSelect(Select select,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {

        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            handlePlainSelect((PlainSelect) selectBody, tableConfigs, tableSuffix);
        } else if (selectBody instanceof SetOperationList) {
            // 处理UNION等集合操作
            SetOperationList setOpList = (SetOperationList) selectBody;
            for (SelectBody body : setOpList.getSelects()) {
                if (body instanceof PlainSelect) {
                    handlePlainSelect((PlainSelect) body, tableConfigs, tableSuffix);
                }
            }
        }
    }

    /**
     * 处理普通SELECT语句
     */
    private static void handlePlainSelect(PlainSelect plainSelect,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        // 处理FROM子句
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table) {
            replaceTable((Table) fromItem, tableConfigs, tableSuffix);
        }

        // 处理JOIN子句
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table) {
                    replaceTable((Table) rightItem, tableConfigs, tableSuffix);
                }
            }
        }

        // 处理子查询
        if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            SelectBody subSelectBody = subSelect.getSelectBody();
            if (subSelectBody instanceof PlainSelect) {
                handlePlainSelect((PlainSelect) subSelectBody, tableConfigs, tableSuffix);
            }
        }
    }

    /**
     * 处理INSERT语句
     */
    private static void handleInsert(Insert insert,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        Table table = insert.getTable();
        replaceTable(table, tableConfigs, tableSuffix);
    }

    /**
     * 处理UPDATE语句
     */
    private static void handleUpdate(Update update,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        Table table = update.getTable();
        replaceTable(table, tableConfigs, tableSuffix);

        // 处理UPDATE的JOIN子句（如果有）
        List<Join> joins = update.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table) {
                    replaceTable((Table) rightItem, tableConfigs, tableSuffix);
                }
            }
        }
    }

    /**
     * 处理DELETE语句
     */
    private static void handleDelete(Delete delete,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        Table table = delete.getTable();
        replaceTable(table, tableConfigs, tableSuffix);

        // 处理DELETE的JOIN子句（如果有）
        List<Join> joins = delete.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table) {
                    replaceTable((Table) rightItem, tableConfigs, tableSuffix);
                }
            }
        }
    }

    /**
     * 替换单个表名
     */
    private static void replaceTable(Table table,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        String tableName = table.getName();

        // 检查该表是否需要分表
        if (tableConfigs != null && tableConfigs.containsKey(tableName)) {
            int tbCount = tableConfigs.get(tableName);

            // 如果配置的分表数量大于1，才进行替换
            if (tbCount > 1) {
                String newTableName = tableName + "_" + tableSuffix;
                table.setName(newTableName);
                log.debug("Table name replaced: {} -> {}", tableName, newTableName);
            }
        }
    }

    /**
     * 使用正则表达式替换表名（降级方案）
     */
    private static String replaceTableNameWithRegex(String sql,
            Map<String, Integer> tableConfigs,
            String tableSuffix) {
        Matcher matcher = REGEX_PATTERN.matcher(sql);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String keyword = matcher.group(1); // from/into/update/join
            String tableName = matcher.group(2); // 表名

            // 检查该表是否需要分表
            if (tableConfigs != null && tableConfigs.containsKey(tableName)) {
                int tbCount = tableConfigs.get(tableName);

                if (tbCount > 1) {
                    String newTableName = tableName + "_" + tableSuffix;
                    matcher.appendReplacement(result, keyword + " " + newTableName);
                    log.debug("Table name replaced by regex: {} -> {}", tableName, newTableName);
                    continue;
                }
            }

            // 不需要替换，保持原样
            matcher.appendReplacement(result, matcher.group(0));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 检查SQL语句类型
     */
    public static SqlType getSqlType(String sql) {
        if (sql == null || sql.isEmpty()) {
            return SqlType.UNKNOWN;
        }

        String trimmedSql = sql.trim().toUpperCase();
        if (trimmedSql.startsWith("SELECT")) {
            return SqlType.SELECT;
        } else if (trimmedSql.startsWith("INSERT")) {
            return SqlType.INSERT;
        } else if (trimmedSql.startsWith("UPDATE")) {
            return SqlType.UPDATE;
        } else if (trimmedSql.startsWith("DELETE")) {
            return SqlType.DELETE;
        }

        return SqlType.UNKNOWN;
    }

    /**
     * SQL类型枚举
     */
    public enum SqlType {
        SELECT, INSERT, UPDATE, DELETE, UNKNOWN
    }
}
