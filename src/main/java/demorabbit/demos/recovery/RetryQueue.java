package demorabbit.demos.recovery;

import java.util.Map;

/**
 * Created by mmoraes on 06/04/16.
 */
public interface RetryQueue {
    String getName();
    boolean accept(final RetryAttempt retry);
    Map<String,Object> getArgs();
}
