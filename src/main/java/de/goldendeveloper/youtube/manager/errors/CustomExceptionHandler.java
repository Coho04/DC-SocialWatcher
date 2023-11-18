package de.goldendeveloper.youtube.manager.errors;

import de.goldendeveloper.mysql.errors.ExceptionHandler;
import io.sentry.Sentry;

public class CustomExceptionHandler extends ExceptionHandler {

    @Override
    public void callException(Exception exception) {
        Sentry.captureException(exception);
        throw new RuntimeException(exception);
    }
}