package com.github.mattdenny12.pipeline;

import com.github.mattdenny12.pipeline.exception.PipelineException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PipelineTest {
    private static final Function<Exchange, Exchange> successfulPipe = exchange -> {
        Integer value = (Integer) exchange.object();
        exchange.object(value + 1);

        return exchange;
    };

    private static final Function<Exchange, Exchange> failingPipe = exchange -> {
        throw new RuntimeException("I've failed!");
    };

    @Test
    public void testDoesNotThrow() {
        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(successfulPipe, successfulPipe, successfulPipe));

        Assertions.assertDoesNotThrow(() -> pipeline.run(0));
    }

    @Test
    public void testSuccessfulPipeline() {
        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(successfulPipe, successfulPipe, successfulPipe));

        Exchange output = pipeline.run(0);

        Assertions.assertEquals(3, output.object());
        Assertions.assertTrue(output.proceed());
        Assertions.assertTrue(output.exceptions().isEmpty());
    }

    @Test
    public void testStartPipelineWithoutInput() {
        Function<Exchange, Exchange> initialPipe = exchange -> exchange.object(0);

        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(initialPipe, successfulPipe, successfulPipe, successfulPipe));

        Exchange output = pipeline.run();

        Assertions.assertEquals(3, output.object());
        Assertions.assertTrue(output.proceed());
        Assertions.assertTrue(output.exceptions().isEmpty());
    }

    @Test
    public void addPipeReturnsSameObject() {
        Pipeline pipeline = new Pipeline()
                .pipes(new ArrayList<>(Arrays.asList(successfulPipe, successfulPipe, successfulPipe)));
        Pipeline otherPipeline = pipeline.addPipe(successfulPipe);

        Assertions.assertEquals(pipeline, otherPipeline);
    }

    @Test
    public void addPipesReturnsSameObject() {
        Pipeline pipeline = new Pipeline()
                .pipes(new ArrayList<>(Arrays.asList(successfulPipe, successfulPipe, successfulPipe)));
        Pipeline otherPipeline = pipeline.addPipes(Arrays.asList(successfulPipe, successfulPipe));

        Assertions.assertEquals(pipeline, otherPipeline);
    }

    @Test
    public void testStartPipelineWithExchange() {
        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(successfulPipe, successfulPipe, successfulPipe));

        Exchange output = pipeline.run(new Exchange().object(0));

        Assertions.assertEquals(3, output.object());
        Assertions.assertTrue(output.proceed());
        Assertions.assertTrue(output.exceptions().isEmpty());
    }

    @Test
    public void testAddPipe() {
        Pipeline pipeline = new Pipeline()
                .pipes(new ArrayList<>(Collections.singletonList(successfulPipe)));

        pipeline.addPipe(successfulPipe);
        pipeline.addPipe(successfulPipe);

        Exchange output = pipeline.run(0);

        Assertions.assertEquals(3, output.object());
        Assertions.assertTrue(output.proceed());
        Assertions.assertTrue(output.exceptions().isEmpty());
    }

    @Test
    public void testAddPipes() {
        Pipeline pipeline = new Pipeline()
                .pipes(new ArrayList<>(Collections.singletonList(successfulPipe)));

        pipeline.addPipes(Arrays.asList(successfulPipe, successfulPipe));

        Exchange output = pipeline.run(0);

        Assertions.assertEquals(3, output.object());
        Assertions.assertTrue(output.proceed());
        Assertions.assertTrue(output.exceptions().isEmpty());
    }

    @Test
    public void testBubbleExceptionsByDefault() {
        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(successfulPipe, failingPipe, successfulPipe));

        Assertions.assertThrows(PipelineException.class, () -> pipeline.run(0));
    }

    @Test
    public void testBubbleExceptionsTrue() {
        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(true)
                .pipes(Arrays.asList(successfulPipe, failingPipe, successfulPipe));

        Assertions.assertThrows(PipelineException.class, () -> pipeline.run(0));
    }

    @Test
    public void testBubbleExceptionsFalse() {
        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .pipes(Arrays.asList(successfulPipe, failingPipe, successfulPipe));

        Exchange exchange = pipeline.run(0);

        Assertions.assertEquals(1, exchange.exceptions().size());
    }

    @Test
    public void testContinueOnExceptionFalseByDefault() {
        List<Exchange> exchangeList = new ArrayList<>();

        Function<Exchange, Exchange> addExchange = exchange -> {
            exchangeList.add(exchange);
            return exchange;
        };

        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .pipes(Arrays.asList(addExchange, failingPipe, addExchange, addExchange));

        pipeline.run(0);

        Assertions.assertEquals(1, exchangeList.size());
    }

    @Test
    public void testContinueOnExceptionFalse() {
        List<Exchange> exchangeList = new ArrayList<>();

        Function<Exchange, Exchange> addExchange = exchange -> {
            exchangeList.add(exchange);
            return exchange;
        };

        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .continueOnException(false)
                .pipes(Arrays.asList(addExchange, failingPipe, addExchange, addExchange));

        Exchange exchange = pipeline.run(0);

        Assertions.assertEquals(1, exchangeList.size());
        Assertions.assertFalse(exchange.proceed());
    }

    @Test
    public void testContinueOnExceptionTrue() {
        List<Exchange> exchangeList = new ArrayList<>();

        Function<Exchange, Exchange> addExchange = exchange -> {
            exchangeList.add(exchange);
            return exchange;
        };

        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .continueOnException(true)
                .pipes(Arrays.asList(addExchange, failingPipe, addExchange, addExchange));

        Exchange exchange = pipeline.run(0);

        Assertions.assertEquals(3, exchangeList.size());
        Assertions.assertTrue(exchange.proceed());
    }
}
