import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executors;

@Order(-1)
public class TestConfiguration {


    @Bean
    public TaskScheduler testTaskScheduler(){
        return new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(10));    }
}
