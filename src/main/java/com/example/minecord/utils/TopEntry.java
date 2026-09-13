package com.example.minecord.utils;

import java.util.UUID;

public class TopEntry {
    private final String name;
    private final UUID uuid;
    private final long value;
    private final String formattedValue;

    public TopEntry(String name, UUID uuid, long value, String formattedValue) {
        this.name = name != null ? name : "Гравець";
        this.uuid = uuid;
        this.value = value;
        this.formattedValue = formattedValue;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getValue() {
        return value;
    }

    public String getFormattedValue() {
        return formattedValue;
    }
}
