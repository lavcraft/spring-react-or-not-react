package ru.spring.demo.reactive.pechkin.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.spring.demo.reactive.pechkin.producer.LetterProducer;
import ru.spring.demo.reactive.starter.speed.AdjustmentProperties;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Evgeny Borisov
 */
@Slf4j
@Getter
@Service
public class LetterDistributor {
    private final LetterProducer       producer;
    private final Counter              counter;
    private final WebClient.Builder    webClientBuilder;
    private final ThreadPoolExecutor   letterProcessorExecutor;
    private final ObjectMapper         objectMapper;
    private final AdjustmentProperties adjustmentProperties;

    public LetterDistributor(LetterProducer producer,
                             MeterRegistry meterRegistry,
                             WebClient.Builder webClientBuilder,
                             ThreadPoolExecutor letterProcessorExecutor,
                             ObjectMapper objectMapper,
                             AdjustmentProperties adjustmentProperties,
                             AdjustmentProperties adjustmentProperties1) {
        this.producer = producer;
        this.counter = meterRegistry.counter("letter.rps");
        this.webClientBuilder = webClientBuilder;
        this.letterProcessorExecutor = letterProcessorExecutor;
        this.objectMapper = objectMapper;
        this.adjustmentProperties = adjustmentProperties1;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        try {
            if(adjustmentProperties.getRequest().get() > 0) {
//                distribute();
                counter.increment();
            } else {
                TimeUnit.MILLISECONDS.sleep(50);
            }
        } catch (Exception e) {
            log.error("Cannot send letter");
            try {
                TimeUnit.MILLISECONDS.sleep(adjustmentProperties.getProcessingTime());
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}
