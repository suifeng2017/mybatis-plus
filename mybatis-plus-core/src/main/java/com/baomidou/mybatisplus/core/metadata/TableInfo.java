/*
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.core.metadata;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.apache.ibatis.session.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlUtils;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据库表反射信息
 * </p>
 *
 * @author hubin
 * @since 2016-01-23
 */
@Data
@Accessors(chain = true)
public class TableInfo {

    /**
     * 表主键ID 类型
     */
    private IdType idType = IdType.NONE;
    /**
     * 数据库类型
     */
    private DbType dbType;
    /**
     * 表名称
     */
    private String tableName;
    /**
     * 表映射结果集
     */
    private String resultMap;
    /**
     * 主键是否有存在字段名与属性名关联
     * true: 表示要进行 as
     */
    private boolean keyRelated = false;
    /**
     * 表主键ID 属性名
     */
    private String keyProperty;
    /**
     * 表主键ID 字段名
     */
    private String keyColumn;
    /**
     * 表主键ID Sequence
     */
    private KeySequence keySequence;
    /**
     * 表字段信息列表
     */
    @Setter(AccessLevel.NONE)
    private List<TableFieldInfo> fieldList;
    /**
     * 命名空间
     */
    private String currentNamespace;
    /**
     * MybatisConfiguration 标记 (Configuration内存地址值)
     */
    private String configMark;
    /**
     * 是否开启逻辑删除
     */
    private boolean logicDelete = false;
    /**
     * 是否开启下划线转驼峰
     */
    private boolean underCamel;
    /**
     * 标记该字段属于哪个类
     */
    private Class<?> clazz;
    /**
     * 缓存包含主键及字段的 sql select
     */
    @Setter(AccessLevel.NONE)
    private String allSqlSelect;
    /**
     * 缓存主键字段的 sql select
     */
    @Setter(AccessLevel.NONE)
    private String sqlSelect;

    /**
     * <p>
     * 获得注入的 SQL Statement
     * </p>
     *
     * @param sqlMethod MybatisPlus 支持 SQL 方法
     * @return SQL Statement
     */
    public String getSqlStatement(String sqlMethod) {
        return currentNamespace + StringPool.DOT + sqlMethod;
    }

    public void setFieldList(GlobalConfig globalConfig, List<TableFieldInfo> fieldList) {
        this.fieldList = fieldList;
        /*
         * 启动逻辑删除注入、判断该表是否启动
         */
        if (null != globalConfig.getDbConfig().getLogicDeleteValue()) {
            for (TableFieldInfo tfi : fieldList) {
                if (tfi.isLogicDelete()) {
                    this.setLogicDelete(true);
                    break;
                }
            }
        }
    }

    public void setConfigMark(Configuration configuration) {
        Assert.notNull(configuration, "Error: You need Initialize MybatisConfiguration !");
        this.configMark = configuration.toString();
    }

    public boolean isLogicDelete() {
        return logicDelete;
    }

    /**
     * 获取主键的 select sql 片段
     *
     * @return sql 片段
     */
    public String getKeySqlSelect() {
        if (sqlSelect != null) {
            return sqlSelect;
        }
        if (StringUtils.isNotEmpty(keyProperty)) {
            if (keyRelated) {
                sqlSelect = SqlUtils.sqlWordConvert(dbType, keyColumn, true) + " AS " +
                    SqlUtils.sqlWordConvert(dbType, keyProperty, false);
            } else {
                sqlSelect = SqlUtils.sqlWordConvert(dbType, keyColumn, true);
            }
        } else {
            sqlSelect = StringPool.EMPTY;
        }
        return sqlSelect;
    }

    /**
     * 获取包含主键及字段的 select sql 片段
     *
     * @return sql 片段
     */
    public String getAllSqlSelect() {
        if (allSqlSelect != null) {
            return allSqlSelect;
        }
        String sqlSelect = getKeySqlSelect();
        String fieldsSqlSelect = fieldList.stream().filter(TableFieldInfo::isSelect)
            .map(i -> i.getSqlSelect(dbType)).collect(joining(StringPool.COMMA));
        if (StringUtils.isNotEmpty(sqlSelect) && StringUtils.isNotEmpty(fieldsSqlSelect)) {
            allSqlSelect = sqlSelect + StringPool.COMMA + fieldsSqlSelect;
        } else if (StringUtils.isNotEmpty(fieldsSqlSelect)) {
            allSqlSelect = fieldsSqlSelect;
        } else {
            allSqlSelect = sqlSelect;
        }
        return allSqlSelect;
    }

    /**
     * 获取 inset 时候主键 sql 脚本片段
     * insert into table (字段) values (值)
     * 位于 "值" 部位
     *
     * @return sql 脚本片段
     */
    public String getKeyInsertSqlProperty() {
        if (StringUtils.isNotEmpty(keyProperty)) {
            if (idType == IdType.AUTO) {
                return "";
            }
            return SqlScriptUtils.HASH_LEFT_BRACE + keyProperty + StringPool.RIGHT_BRACE + StringPool.COMMA;
        }
        return "";
    }

    /**
     * 获取 inset 时候主键 sql 脚本片段
     * insert into table (字段) values (值)
     * 位于 "字段" 部位
     *
     * @return sql 脚本片段
     */
    public String getKeyInsertSqlColumn() {
        if (StringUtils.isNotEmpty(keyColumn)) {
            if (idType == IdType.AUTO) {
                return "";
            }
            return keyColumn + StringPool.COMMA;
        }
        return "";
    }


    /**
     * 获取所有 inset 时候插入值 sql 脚本片段
     * insert into table (字段) values (值)
     * 位于 "值" 部位
     *
     * @return sql 脚本片段
     */
    public String getAllInsertSqlProperty() {
        return getKeyInsertSqlProperty() + fieldList.stream().map(TableFieldInfo::getInsertSqlProperty)
            .collect(joining(StringPool.EMPTY));
    }

    /**
     * 获取 inset 时候字段 sql 脚本片段
     * insert into table (字段) values (值)
     * 位于 "字段" 部位
     *
     * @return sql 脚本片段
     */
    public String getAllInsertSqlColumn() {
        return getKeyInsertSqlColumn() + fieldList.stream().map(TableFieldInfo::getInsertSqlColumn)
            .collect(joining(StringPool.EMPTY));
    }

    /**
     * 获取所有的 sql set 片段
     *
     * @param prefix 前缀
     * @return sql 脚本片段
     */
    public String getAllSqlSet(String prefix) {
        String newPrefix = StringUtils.isEmpty(prefix) ? StringPool.EMPTY : prefix;
        return fieldList.stream().map(i -> i.getSqlSet(newPrefix)).collect(joining(StringPool.NEWLINE));
    }
}
