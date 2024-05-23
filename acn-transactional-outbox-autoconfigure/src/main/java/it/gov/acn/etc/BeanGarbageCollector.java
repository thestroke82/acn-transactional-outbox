package it.gov.acn.etc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class BeanGarbageCollector {
  private static final List<Object> temporaryBeans = new ArrayList<>();
  private static final List<Object> coreBeans = new ArrayList<>();

  public static <T> T registerTemporaryBean(T bean) {
    temporaryBeans.add(bean);
    return bean;
  }

  public static <T> T registerCoreBean(T bean) {
    coreBeans.add(bean);
    return bean;
  }

  public static void destroyTemporaryBeans(ApplicationContext applicationContext) {
    ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext)applicationContext;
    temporaryBeans.forEach(r->destroyBean(configurableApplicationContext, r));
  }

  public static void destroyCoreBeans(ApplicationContext applicationContext) {
    ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext)applicationContext;
    coreBeans.forEach(r->destroyBean(configurableApplicationContext, r));
  }

  private static void destroyBean(ConfigurableApplicationContext configurableApplicationContext, Object bean) {
    configurableApplicationContext.getBeanFactory().destroyBean(bean);
  }
}
