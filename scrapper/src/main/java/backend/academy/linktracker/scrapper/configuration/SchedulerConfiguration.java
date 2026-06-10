package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService linkCheckExecutor(SchedulerProperties properties) {
        return Executors.newFixedThreadPool(properties.getThreadCount());
    }
}
