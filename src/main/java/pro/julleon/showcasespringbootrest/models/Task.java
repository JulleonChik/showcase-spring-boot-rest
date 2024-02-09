package pro.julleon.showcasespringbootrest.models;

import java.util.UUID;

public record Task(UUID id, String description, boolean completed) {

    public Task(String description) {
        this(UUID.randomUUID(), description, false);
    }
}
