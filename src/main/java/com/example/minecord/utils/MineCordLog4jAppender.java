package com.example.minecord.utils;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.function.Consumer;

public class MineCordLog4jAppender extends AbstractAppender {
    private final Consumer<LogEvent> eventConsumer;

    public MineCordLog4jAppender(Consumer<LogEvent> eventConsumer) {
        super("MineCordConsoleAppender", null, null, false, Property.EMPTY_ARRAY);
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void append(LogEvent event) {
        if (eventConsumer != null) {
            try {
                eventConsumer.accept(event);
            } catch (Throwable ignored) {
            }
        }
    }
}
