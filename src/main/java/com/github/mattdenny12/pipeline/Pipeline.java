package com.github.mattdenny12.pipeline;

import com.github.mattdenny12.pipeline.exception.PipelineException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

@Accessors(fluent = true)
@Getter
@Setter
public class Pipeline {

    /**
     * Whether exceptions should bubble up. That is, if an exception occurs, and {@code bubbleExceptions} is
     * {@code true}, then any exceptions that occur during the execution of the pipeline will be thrown. Inversely, if
     * {@code bubbleExceptions} is false, then exceptions will not be thrown and will instead be added to the exchange.
     */
    protected boolean bubbleExceptions = true;

    /**
     * <b>bubbleExceptions must be false for this to take affect.</b><br>
     * Allows the pipeline to continue in the event that an exception occurs. This is best used when pipes further in
     * the pipeline do not rely on the successful completion of the previous pipes. This is meant to be used sparingly.
     */
    protected boolean continueOnException = false;
    protected Collection<Function<Exchange, Exchange>> pipes = new ArrayList<>();

    /**
     * @param newPipe The new pipe to be added to the end of the pipeline.
     * @return The resulting pipeline.
     */
    public Pipeline addPipe(Function<Exchange, Exchange> newPipe) {
        pipes.add(newPipe);
        return this;
    }

    /**
     * @param newPipes The new pipes to be added to the end of the pipeline.
     * @return The resulting pipeline.
     */
    public Pipeline addPipes(Collection<Function<Exchange, Exchange>> newPipes) {
        pipes.addAll(newPipes);
        return this;
    }

    /**
     * Run the pipeline with no input. This is best used when one of the first pipes fetch the required input. <br>
     * <i>Note: An exchange will still be generated for the pipeline, but the object in the exchange will be null.</i>
     * @return The resulting exchange from the pipeline.
     */
    public Exchange run() {
        return run(new Exchange());
    }

    /**
     * Run the pipeline with the provided input. An exchange will be generated for you.
     * @param input The input that will be placed into the exchange before starting the pipeline.
     * @return The resulting exchange from the pipeline.
     */
    public Exchange run(Object input) {
        return run(new Exchange().object(input));
    }

    /**
     * Run the pipeline using the provided exchange.
     * @param exchange The exchange that will be used to start the pipeline.
     * @return The resulting exchange from the pipeline.
     */
    public Exchange run(Exchange exchange) {
        for (Function<Exchange, Exchange> pipe : pipes) {
            try {
                exchange = pipe.apply(exchange);
            } catch (Exception e) {
                PipelineException pipelineException = PipelineException.builder()
                        .message(String.format("An error occurred when processing pipe %s: %s", pipe, e.getMessage()))
                        .cause(e)
                        .exchange(exchange)
                        .brokenPipe(pipe)
                        .build();

                handleException(pipelineException, exchange);
            }

            if (!exchange.proceed()) break;
        }

        return exchange;
    }

    private void handleException(PipelineException pipelineException, Exchange exchange) {
        exchange.exceptions().add(pipelineException);

        if (bubbleExceptions) {
            throw pipelineException;
        }

        exchange.proceed(continueOnException);
    }
}
