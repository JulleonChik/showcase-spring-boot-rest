package pro.julleon.showcasespringbootrest.repositories;

import pro.julleon.showcasespringbootrest.models.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {

    List<Task> findAll();

    void save(Task task);

    Optional<Task> findById(UUID taskId);
}
