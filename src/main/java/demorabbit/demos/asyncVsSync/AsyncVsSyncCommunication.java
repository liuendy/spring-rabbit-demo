package demorabbit.demos.asyncVsSync;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import demorabbit.demos.demoapp.conf.Queues;
import demorabbit.demos.demoapp.conf.RoutingKeys;
import demorabbit.demos.demoapp.order.OrderInfo;

/**
 * Created by mmoraes on 2016-01-07.
 */
@SpringBootApplication(scanBasePackages = "demorabbit.demos.demoapp.conf")
@EnableRabbit
public class AsyncVsSyncCommunication {

    private static final Log LOG = LogFactory.getLog(AsyncVsSyncCommunication.class);
    private final int numRequests = 100;
    private final Random idGenerator = new Random();

    private final static int PERSISTENT = 1;
    private final static int TRANSACTIONAL = 2;
    private final static int DEBUG = 4;

    public static void main(final String[] args) throws InterruptedException {
        System.setProperty("spring.main.web_environment", "false");
        final AsyncVsSyncCommunication demo = SpringApplication.run(AsyncVsSyncCommunication.class, args)
                .getBean(AsyncVsSyncCommunication.class);
        demo.run();
        System.exit(0);
    }

    @Autowired()
    @Qualifier("noTxRabbitTemplate")
    private RabbitTemplate noTxRabbitTemplate;

    @Autowired()
    @Qualifier("txRabbitTemplate")
    private RabbitTemplate txRabbitTemplate;

    @Autowired
    private Queues queues;

    @Autowired
    private RoutingKeys routingKeys;

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    void init() {
        LOG.info("Criando " + numRequests + " Orders para teste...");
        for (int i = 0; i < numRequests; i++) {
            final int checkoutRef = i + 1;
            final OrderInfo orderInfo = restTemplate.postForObject("http://localhost:8080/createTest/" + nextId(), null,
                    OrderInfo.class);
        }
        LOG.info("Orders criadas, iniciando teste.");
    }

    void run() throws InterruptedException {
//        createRabbitCall(0).run(1).printResult();
        init();

        final Call rabbitCallTx = createRabbitCall(TRANSACTIONAL + PERSISTENT);
        final Call rabbitCall = createRabbitCall(0);
        final Call restCall = createRESTCall();

        restCall.run(10);
        rabbitCallTx.run(10);
        rabbitCall.run(10);

        final List<Call> calls = Arrays.asList(restCall, rabbitCall, rabbitCallTx);
        Collections. sort(calls);
        calls.forEach(Call::printResult);
    }

    private Call createRESTCall() {
        return new Call(() -> restTemplate.getForObject("http://localhost:8080/order/" + nextId(),
                OrderInfo.class), numRequests, "getCreateOrder REST  ");
    }

    private Call createRabbitCall(final int options) {
        final String tag = "getCreateOrder RABT:";

        final Call call = new Call(() -> {
            final OrderInfo orderInfo;
            final RabbitTemplate rabbitTemplate;

            rabbitTemplate = getRabbitTemplate(options);
            final int id = nextId();

            final String routingKey = getRoutingKey(options);
            if (isTransient(options)) {
                final Message message = createNonPersistenceMessage(id);
                final Message response = rabbitTemplate.sendAndReceive(queues.exchange(),
                        routingKey, message);
                orderInfo = (OrderInfo) rabbitTemplate.getMessageConverter().fromMessage(response);
            } else {
                orderInfo = (OrderInfo) rabbitTemplate.convertSendAndReceive(queues.exchange(),
                        routingKey, id);
            }

            if ((options & DEBUG) == DEBUG) {
                System.out.println("orderId:" + id + " response:" + orderInfo);
            }

        }, numRequests, tag + options);

        return call;
    }

    private boolean isTransient(final int options) {
        return !((options & PERSISTENT) == PERSISTENT);
    }

    private String getRoutingKey(final int options) {
        if ((options & TRANSACTIONAL) == TRANSACTIONAL)
            return routingKeys.getOrder() + "Tx";
        return routingKeys.getOrder();
    }

    private Message createNonPersistenceMessage(final int id) {
        final MessageProperties props = MessagePropertiesBuilder.newInstance()
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT).build();
        return noTxRabbitTemplate.getMessageConverter().toMessage(id, props);
    }

    private RabbitTemplate getRabbitTemplate(final int options) {
        final RabbitTemplate rabbitTemplate;
        if ((options & TRANSACTIONAL) == TRANSACTIONAL) {
            rabbitTemplate = txRabbitTemplate;
        } else {
            rabbitTemplate = noTxRabbitTemplate;
        }
        return rabbitTemplate;
    }

    private int nextId() {
        int id = idGenerator.nextInt(numRequests);
        id = (id > 0) ? id : 1;
        return id;
    }

    private class Call implements Comparable<Call> {

        final Runnable task;
        private int numRequests;
        private final String tag;
        private String result;
        private int curLine = 1;
        private Long totalTime;
        private ExecutorService executorASync;

        Call(final Runnable task, final int numRequests, final String tag) {
            this.task = task;
            this.numRequests = numRequests;
            this.tag = tag;
        }

        Call setNumRequests(final int numRequests) {
            this.numRequests = numRequests;
            return this;
        }

        public Call run(final int workers) throws InterruptedException {

            final StopWatch stopWatch = new StopWatch(tag);
            stopWatch.start();
            System.out.println("======================== " + tag.trim() + " ========================");

            // startWorkers the task and wait finish
            final CountDownLatch counter = startWorkers(workers);
            counter.await(1, TimeUnit.MINUTES);

            stopWatch.stop();
            totalTime = stopWatch.getTotalTimeMillis();
            final float avg = (float) totalTime / numRequests;
            result = String.format("Total: %s AVG: %.2f (%.2f/s)", totalTime, avg, 60 / avg);

            System.out.println("");
            System.out.println("======================== FINISHED ========================");
            return this;
        }

        private CountDownLatch startWorkers(final int workers) {
            final CountDownLatch counter = new CountDownLatch(numRequests);
            final ExecutorService executor = Executors.newFixedThreadPool(workers);
            for (int i = 0; i < numRequests; i ++) {
                executor.execute(() -> {
                    this.task.run();
                    if (curLine > 80) {
                        curLine = 1;
                        System.out.println('.');
                    } else {
                        curLine++;
                        System.out.print('.');
                    }
                    counter.countDown();
                });
            }
            return counter;
        }

        void printResult() {
            System.out.println(tag + " - " + result);
        }

        @Override
        public int compareTo(final Call other) {
            return totalTime.compareTo(other.totalTime);
        }

    }
}
