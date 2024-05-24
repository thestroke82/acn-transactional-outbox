package it.gov.acn.etc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import javax.sql.DataSource;

public class Utils {
    /**
     * Check if the specified DataSource is a Postgres datasource.
     * @param dataSource The DataSource to check
     * @return True if the specified DataSource is a Postgres datasource, false otherwise
     */
    public static boolean isPostgresDatasource(DataSource dataSource){
        if(dataSource==null){
            return false;
        }
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            return url != null && url.contains("jdbc:postgresql:");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the specified bean type is present in the context, without initializing it in case it's not.
     * @param beanFactory The bean factory of the context
     * @param beanType The bean type to check
     * @return True if the specified bean type is present in the context, false otherwise
     */
    public static boolean isBeanPresentInContext(ConfigurableListableBeanFactory beanFactory, Class<?> beanType){
        return beanFactory.getBeanNamesForType(beanType, true, false).length>0;
    }



    public static boolean doesTableExist(DataSource dataSource, String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            if(metaData==null){
                return false;
            }
            try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
                return tables!=null && tables.next();
            }
        } catch (Exception e) {
            return false;
        }
    }


}
