package ru.spring.demo.reactive.pechkin.producer;

import com.github.javafaker.RickAndMorty;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import ru.spring.demo.reactive.starter.speed.AdjustmentProperties;
import ru.spring.demo.reactive.starter.speed.model.Letter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Evgeny Borisov
 */
@Slf4j
@Service
@Setter
@RequiredArgsConstructor
public class LetterProducer {
    private final RickAndMorty                           faker;
    private final ThreadPoolExecutor                     letterProcessorExecutor;
    private final ObjectProvider<EmitterProcessor<Long>> emitterProcessorProvider;
    private final AdjustmentProperties                   adjustmentProperties;

    @SneakyThrows
    public Letter getLetter() {
        return randomLetter();
    }

    LinkedBlockingQueue letterQueue = new LinkedBlockingQueue();

    public Flux<Letter> letterFlux() {
        return Flux.<Letter>generate(letterSynchronousSink -> letterSynchronousSink.next(randomLetter()));
    }

    private Letter randomLetter() {
        String character = faker.character();
        return Letter.builder()
                .content(faker.quote())
                .location(faker.location())
                .signature(character)
                ._original(character)
                .build();
    }


}
