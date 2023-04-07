package com.github.mattdenny12.pipeline;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;

@Accessors(fluent = true)
@Getter
@Setter
public class Exchange {
    protected boolean proceed = true;
    protected Object object = null;
    protected Collection<Exception> exceptions = new ArrayList<>();
}
