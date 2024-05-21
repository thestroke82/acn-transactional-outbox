package it.gov.acn.context;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import javax.sql.DataSource;
import java.util.Map;

public class ContextUtils {

    /**
     * Check if a property with the specified prefix is present in the environment.
     * @param prefix The prefix to check
     * @param environment The environment
     * @return True if a property with the specified prefix is present in the environment, false otherwise
     */
    public static boolean isPrefixPresentInProperties(String prefix, Environment environment){
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;
            for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
                if (propertySource.getSource() instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) propertySource.getSource();
                    for (String key : map.keySet()) {
                        if (key.startsWith(prefix)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }



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


}
