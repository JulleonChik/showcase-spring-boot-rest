package pro.julleon.showcasespringbootrest.controllers;

import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import pro.julleon.showcasespringbootrest.http.dto.NewTaskPayload;
import pro.julleon.showcasespringbootrest.http.error.ErrorsPresentation;
import pro.julleon.showcasespringbootrest.models.Task;
import pro.julleon.showcasespringbootrest.repositories.TaskRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/tasks")
public class TaskRestController {

    private final TaskRepository taskRepository;

    private final MessageSource messageSource;

    public TaskRestController(TaskRepository taskRepository,
                              MessageSource messageSource) {
        this.taskRepository = taskRepository;
        this.messageSource = messageSource;
    }

    @GetMapping
    public ResponseEntity<List<Task>> handelGetAllTasks() {
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(this.taskRepository.findAll());
    }


    @PostMapping
    public ResponseEntity<?> handelCreateNewTask(
            @RequestBody NewTaskPayload taskPayload,
            UriComponentsBuilder uriComponentsBuilder,
            Locale locale
    ) {

        if (taskPayload.description() == null || taskPayload.description().isBlank()) {
            final String errorMessage = this.messageSource
                    .getMessage("tasks.create.description.errors.not_set",
                            new Object[0], locale);
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorsPresentation(
                            List.of(errorMessage)));
        } else {
            Task task = new Task(taskPayload.description());
            this.taskRepository.save(task);
            return ResponseEntity
                    .created(uriComponentsBuilder
                            .path("/api/tasks/{taskId}")
                            .build(Map.of("taskId", task.id())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(task);
        }
    }


    @GetMapping("{id}")
    public ResponseEntity<Task> handelFindTask(
            @PathVariable("id") UUID taskId
    ) {
        return ResponseEntity
                .of(this.taskRepository.findById(taskId));
    }


}
