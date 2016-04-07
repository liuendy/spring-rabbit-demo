package demorabbit.demos.demoapp;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by mmoraes on 2016-01-11.
 */
@SpringBootApplication
@EnableJpaRepositories("demorabbit.demos.demoapp")
@EnableRabbit
@EnableTransactionManagement
public class DemoAppApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DemoAppApplication.class, args).getBean(DemoAppApplication.class);
    }
}
