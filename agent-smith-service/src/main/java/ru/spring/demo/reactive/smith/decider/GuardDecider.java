package ru.spring.demo.reactive.smith.decider;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.spring.demo.reactive.smith.notifier.Notifier;
import ru.spring.demo.reactive.starter.speed.AdjustmentProperties;
import ru.spring.demo.reactive.starter.speed.model.DecodedLetter;
import ru.spring.demo.reactive.starter.speed.model.Notification;
import ru.spring.demo.reactive.starter.speed.services.LetterRequesterService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GuardDecider {
    private final AdjustmentProperties    adjustmentProperties;
    private final Notifier                notifier;
    private final Counter                 counter;
    private final ThreadPoolExecutor      letterProcessorExecutor;
    private final LetterRequesterService  letterRequesterService;
    private final BlockingQueue<Runnable> workQueue;

    public GuardDecider(
            AdjustmentProperties adjustmentProperties,
            Notifier notifier,
            MeterRegistry meterRegistry,
            ThreadPoolExecutor letterProcessorExecutor,
            LetterRequesterService letterRequesterService) {
        this.adjustmentProperties = adjustmentProperties;
        this.notifier = notifier;
        this.letterProcessorExecutor = letterProcessorExecutor;
        this.letterRequesterService = letterRequesterService;

        counter = meterRegistry.counter("letter.rps");
        workQueue = letterProcessorExecutor.getQueue();
    }

    public void decide(DecodedLetter notification) {
        makeDecisionAndNotify(notification);
    }


    @SneakyThrows
    private String getDecision() {
        TimeUnit.MILLISECONDS.sleep(adjustmentProperties.getProcessingTime());
        int decision = (int) ((Math.random() * (2)) + 1);
        if(decision == 1) {
            return "Nothing";
        } else {
            return "Block";
        }
    }

    private void makeDecisionAndNotify(DecodedLetter decodedLetter) {
        String decision = getDecision();

        Notification notification = Notification.builder()
                .author(decodedLetter.getAuthor())
                .action(decision)
                .build();

        notifier.sendNotification(notification);
        counter.increment();

    }

}
