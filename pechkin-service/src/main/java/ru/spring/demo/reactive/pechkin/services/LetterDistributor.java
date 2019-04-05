package ru.spring.demo.reactive.pechkin.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.spring.demo.reactive.pechkin.producer.LetterProducer;
import ru.spring.demo.reactive.starter.speed.AdjustmentProperties;
import ru.spring.demo.reactive.starter.speed.model.Letter;

import java.time.Duration;
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
        webClientBuilder.baseUrl("http://localhost:8081/").build()
                .post().uri("/analyse/letter")
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .body(
                        producer.letterFlux()
                                .doOnNext(letter -> counter.increment())
                                .log(),
                        Letter.class
                )
                .exchange()
                .doOnError(throwable -> log.error("Sth went wrong {}", throwable.getMessage()))
                .retryBackoff(Long.MAX_VALUE, Duration.ofMillis(500))
                .log()
                .subscribe(clientResponse -> log.info("clientResponse = " + clientResponse));
    }

}
