package shu.xai.literature_backened;

import java.sql.Types;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

public class SQLiteDialect extends Dialect {
    
    public SQLiteDialect() {
        super();
        
        registerColumnType(Types.BIT, "integer");
        registerColumnType(Types.BOOLEAN, "integer");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.NUMERIC, "numeric");
        registerColumnType(Types.VARCHAR, "varchar");
        registerColumnType(Types.LONGVARCHAR, "text");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "datetime");
        registerColumnType(Types.BINARY, "blob");
        registerColumnType(Types.VARBINARY, "blob");
        registerColumnType(Types.LONGVARBINARY, "blob");
        registerColumnType(Types.BLOB, "blob");
        registerColumnType(Types.CLOB, "text");
        
        registerFunction("concat", new StandardSQLFunction("concat", StandardBasicTypes.STRING));
    }

    public boolean supportsLimit() {
        return true;
    }

    public boolean supportsLimitOffset() {
        return true;
    }

    public String getLimitString(String query, boolean hasOffset) {
        return new StringBuffer(query.length() + 20)
                .append(query)
                .append(hasOffset ? " limit ? offset ?" : " limit ?")
                .toString();
    }

    public boolean bindLimitParametersInReverseOrder() {
        return true;
    }
}