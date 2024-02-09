package pro.julleon.showcasespringbootrest.repositories.impl;

import org.springframework.stereotype.Repository;
import pro.julleon.showcasespringbootrest.models.Task;
import pro.julleon.showcasespringbootrest.repositories.TaskRepository;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InMemoryTaskRepositoryImpl implements TaskRepository {

    private final List<Task> tasks = new LinkedList<>() {{
        this.add(new Task("First task"));
        this.add(new Task("Second task"));
    }};


    @Override
    public List<Task> findAll() {
        return this.tasks;
    }

    @Override
    public void save(Task task) {
        this.tasks.add(task);
    }

    @Override
    public Optional<Task> findById(UUID taskId) {
        return this.tasks
                .stream()
                .filter(task -> taskId.equals(task.id()))
                .findFirst();
    }
}
