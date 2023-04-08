# pipeline

pipeline is a lightweight and functional approach to implementing pipelines into a project.

## Usage

Most classes in this library make use of [fluent accessors](https://www.baeldung.com/lombok-accessors#fluent).

At the core of the library, we have a class called `Pipeline`. This is the class that will be used to build and
configure our pipelines to our likings.

### Adding Pipes to the Pipeline

In its simplest form, we can set the pipes in our pipeline using the `.pipes(...)` method:

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Pipeline pipeline = new Pipeline();
        pipeline.pipes(Arrays.asList(pipe, pipe, pipe));
    }
}
```

The `.pipes(...)` method will overwrite any pipes that are currently in our pipeline. If we want to add new pipes to the
end of out pipeline, we can simply use the `.addPipe(...)` and `.addPipes(...)` methods:

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Pipeline pipeline = new Pipeline();
        pipeline.addPipe(pipe);
        pipeline.addPipes(Arrays.asList(pipe, pipe));
    }
}
```

### Disable Bubbling Exceptions

We can use the `.bubbleExceptions(...)` method to set whether exceptions should bubble up or not. If we decided to 
capture the exceptions (`.bubbleExceptions(false)`), then the `Exchange` class will contain any exceptions that occurred
during the execution of the pipeline.

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .pipes(Arrays.asList(pipe, pipe, pipe));
    }
}
```

### Continue on Exception

If we have chosen to not bubble our exceptions (`.bubbleExceptions(false)`), we can then define how we want the pipeline
to behave when an exception does occur. If we want our pipeline to continue when an exception does occur, we can simply
call `.continueOnException(true)`:

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .continueOnException(true)
                .pipes(Arrays.asList(pipe, pipe, pipe));
    }
}
```

### Exception Callback

If you want to handle exceptions as they arise in your pipeline, you can define an exception callback using 
`.excpetionCallback(...)`. This method will be called regardless of the settings for `bubblesExceptions` and 
`continueOnException`.

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Pipeline pipeline = new Pipeline()
                .bubbleExceptions(false)
                .continueOnException(true)
                .exceptionCallback(exchange -> {
                    // Do something...
                    return exchange;
                })
                .pipes(Arrays.asList(pipe, pipe, pipe));
    }
}
```

### Starting the Pipeline

Let's run our pipeline now that it's been configured. All we need to do is call `.run(...)` and the pipeline will take
the input and begin execution. The `.run(...)` method can be called without an `Object`, and `Exchange`, or you can
abstain from providing an input all together if you want the pipeline to handle generating the object for you.

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Object someObject = new Object();
        
        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(pipe, pipe, pipe))
                .run(someObject);
    }
}
```

```java
import java.util.Arrays;
import java.util.function.Function;

public class Example {
    final Function<Exchange, Exchange> initialPipe = exchange -> exchange.object(new Object());
    final Function<Exchange, Exchange> pipe = exchange -> exchange;

    public static void example() {
        Object someObject = new Object();
        
        Pipeline pipeline = new Pipeline()
                .pipes(Arrays.asList(initialPipe, pipe, pipe))
                .run();
    }
}
```

## Reasoning

While Java's built-in functional interfaces and streams provide a significant amount of functionality and can be used
to create relatively straight forward pipeline's, there are a few scenarios where the vanilla java implementation just
doesn't cut it.

### Quiet Exits

Say we have a scenario where we need to exit the series of functions early. Whether it be the result of some bad state,
a failed validation, etc. With standard functions, with or without streams, we have to rely on exceptions to stop the
chain of functions for executing the whole way through.

```java
public class Main {
    static final Function<Double, Double> validationFunction = value -> {
        if (value < 0) {
            throw new RuntimeException("Exceeded maximum value.");
        }
        return value;
    };

    public static void main(String[] args) {
        Double[] someValues = new Double[]{4.0, 9.0, 16.0, -4.0};

        for (Double value: someValues) {
            try {
                Double squareRoot = validationFunction.andThen(Math::sqrt).apply(value);
                System.out.printf("The square root of %s is %s%n", value, squareRoot);
            } catch (Exception e) {
                System.out.printf("%s is not a positive number.%n", value);
            }
        }
    }
}
```

With pipeline, we can simply indicate that we no longer want to proceed, and the pipeline will exit early:

```java
public class Main {
    static final Function<Exchange, Exchange> validationFunction =
            exchange -> exchange.proceed((Double) exchange.object() >= 0);

    static final Function<Exchange, Exchange> squareRootFunction =
            exchange -> exchange.object(Math.sqrt((Double) exchange.object()));

    public static void main(String[] args) {
        Double[] someValues = new Double[]{4.0, 9.0, 16.0, -4.0};

        // Create our pipeline
        Pipeline pipeline = new Pipeline().pipes(Arrays.asList(validationFunction, squareRootFunction));

        for (Double value: someValues) {
            Exchange exchange = pipeline.run(value);

            // exchange.proceed() will be false if we had to exit early
            String message = exchange.proceed()
                    ? String.format("The square root of %s is %s", value, exchange.object())
                    : String.format("%s is not a positive number.", value);

            System.out.println(message);
        }
    }
}
```

### Continuation

Let's say you have a series of actions that need to be performed after a person has registered for your site. Maybe
you want to email them? Maybe you want to send analytics to your analytic engine? Maybe they are the 1000th
registration, and you want to send them some cool merch!? In any case, pipeline will allow you to take those discrete
units of work, and execute each of them without any exceptions blocking later parts of the pipeline. The `Exchange`
object will track any exceptions that do arise, and allow you to action them later on.

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Person {
    String name;
    String email;
    // Some more data here
}

public class SomeBrokenPipe implements Function<Exchange, Exchange> {
    public Exchange apply(Exchange exchange) {
        throw new RuntimeException("I've failed!");
    }
}

public class SomeWorkingPipe implements Function<Exchange, Exchange> {
    public Exchange apply(Exchange exchange) {
        // Do some stuff
        return exchange;
    }
}

public class PostRegistrationUseCase() {

    private static final Logger logger = Logger.getLogger(PostRegistrationUseCase.class);
    private final Pipeline pipeline;

    public PostRegistrationUseCase() {
        pipeline = new Pipeline()
                .bubblesExcpetions(false)
                .continueOnException(true)
                .pipes(Arrays.asList(new SomeWorkingPipe(), new SomeBrokenPipe(), new SomeWorkingPipe()));
    }

    public void execute(Person person) {
        Exchange exchange = pipeline.apply(person);
        
        for (Exception exception: exchange.exceptions()) {
            // Handle exceptions however you want...
        }
    }
}
```

## Disclaimer

This repo is mainly used as a testing grounds for CI/CD stuff. With that in mind, feel free use and make changes to the
package as you see fit.