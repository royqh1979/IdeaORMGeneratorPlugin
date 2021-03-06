<#compress>
public List<${mapEntity.classInfo.name}> find${mapEntity.classInfo.name}ForAdd(
    <#list indexProperties as indexProperty>
        <#if templateUtils.isRangeTypeProperty(indexProperty)>
    ${templateUtils.getObjectType(indexProperty.type)} min${indexProperty.name?cap_first},
    ${templateUtils.getObjectType(indexProperty.type)} max${indexProperty.name?cap_first},
        <#else>
    ${templateUtils.getObjectType(indexProperty.type)} ${indexProperty.name},
        </#if>
    </#list>
    int ${entityId}) {
    List<String> params=new ArrayList<>();
        <#list indexProperties as indexProperty>
            <#if templateUtils.isRangeTypeProperty(indexProperty)>
    if (min${indexProperty.name?cap_first}!=null && max${indexProperty.name?cap_first}!=null) {
        params.add("(${quote}${indexProperty.columnName}${quote} between ? and ? )");
    } else if (min${indexProperty.name?cap_first} != null) {
        params.add("(${quote}${indexProperty.columnName}${quote} > ? )");
    } else if (max${indexProperty.name?cap_first} != null) {
        params.add("(${quote}${indexProperty.columnName}${quote} < ? )");
    }
            <#elseif templateUtils.isStringType(indexProperty.type)>
    if (${indexProperty.name}!=null) {
        params.add("${quote}${indexProperty.columnName}${quote} like ?");
    }
            <#else>
    if (${indexProperty.name}!=null) {
        params.add("${quote}${indexProperty.columnName}${quote} = ?");
    }
            </#if>
        </#list>
    String sql = "select * from ${quote}${mapEntity.tableName}${quote} where ${quote}${mapEntity.idProperty.columnName}${quote} not in (select ${quote}${mapRelationInfo.mappingEntityIdColumn}${quote} from  ${quote}${mapRelationInfo.mapTable}${quote}  where ${quote}${mapRelationInfo.idColumn}${quote} = ?) ";
    if (params.size()!=0) {
        sql=sql+ " and "+String.join(" and ",params);
    }
    logger.debug(sql);
    Connection con = DataSourceUtils.getConnection(getDataSource());
    PreparedStatement stmt = null;
    try {
        stmt = con.prepareStatement(sql);
        int i=1;
        stmt.setInt(i,${entityId});
        i++;
<#list indexProperties as indexProperty>
    <#if templateUtils.isRangeTypeProperty(indexProperty)>
        if (min${indexProperty.name?cap_first} != null) {
            ${templateUtils.generateStatementParameterSetter("i", indexProperty, "min" + indexProperty.name?cap_first)}
            i++;
        }
        if (max${indexProperty.name?cap_first} != null) {
            ${templateUtils.generateStatementParameterSetter("i", indexProperty, "max" + indexProperty.name?cap_first)}
            i++;
        }
    <#elseif templateUtils.isStringType(indexProperty.type)>
        if (${indexProperty.name}!=null) {
            ${templateUtils.generateStatementParameterSetter("i", indexProperty, indexProperty.name+"+\"%\"")}
            i++;
        }
    <#else>
        if (${indexProperty.name}!=null) {
            ${templateUtils.generateStatementParameterSetter("i", indexProperty, indexProperty.name)}
            i++;
        }
    </#if>
</#list>
        ResultSet resultSet = stmt.executeQuery();
        List<${mapEntity.classInfo.name}> results = new ArrayList<>();
        i=0;
        while (resultSet.next()) {
            results.add(__${mapEntity.classInfo.name}Persistor.SIMPLE_ROW_MAPPER.mapRow(resultSet, i++));
        }
        return results;
    } catch (SQLException ex) {
        JdbcUtils.closeStatement(stmt);
        stmt = null;
        DataSourceUtils.releaseConnection(con, getDataSource());
        con = null;
        throw new RuntimeException(ex);
    } finally {
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(con, getDataSource());
    }
}
</#compress>
