package ru.spring.demo.reactive.pechkin.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.spring.demo.reactive.pechkin.producer.LetterProducer;
import ru.spring.demo.reactive.starter.speed.AdjustmentProperties;
import ru.spring.demo.reactive.starter.speed.model.Letter;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Evgeny Borisov
 */
@Slf4j
@Getter
@Service
public class LetterDistributor {
    private final LetterSender         sender;
    private final AdjustmentProperties adjustmentProperties;
    private final LetterProducer       producer;
    private final Counter              counter;
    private final ThreadPoolExecutor   letterProcessorExecutor;
    private final ObjectMapper         objectMapper;

    public LetterDistributor(
            LetterSender sender,
            AdjustmentProperties adjustmentProperties,
            LetterProducer producer,
            MeterRegistry meterRegistry,
            ThreadPoolExecutor letterProcessorExecutor,
            ObjectMapper objectMapper) {
        this.sender = sender;
        this.adjustmentProperties = adjustmentProperties;
        this.producer = producer;
        this.counter = meterRegistry.counter("letter.rps");
        this.letterProcessorExecutor = letterProcessorExecutor;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        while (true) {
            if(adjustmentProperties.getRequest().get() > 0) {
                distribute();
                counter.increment();
            } else {
                TimeUnit.MILLISECONDS.sleep(200);
            }

        }
    }

    @SneakyThrows
    public void distribute() {
        Letter letter = producer.getLetter();
        log.info("letter = " + letter);
        sender.send(letter);
        adjustmentProperties.getRequest().getAndDecrement();
    }
}
