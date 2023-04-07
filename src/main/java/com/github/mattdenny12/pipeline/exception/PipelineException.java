package com.github.mattdenny12.pipeline.exception;

import com.github.mattdenny12.pipeline.Exchange;
import lombok.Builder;

import java.util.function.Function;

@Builder
public class PipelineException extends RuntimeException {
    private String message;
    private Throwable cause;
    private Exchange exchange;
    private Function<Exchange, Exchange> brokenPipe;

    public PipelineException(
            String message, Throwable cause, Exchange exchange, Function<Exchange, Exchange> brokenPipe) {
        super(message, cause);
        this.exchange = exchange;
        this.brokenPipe = brokenPipe;
    }
}
