package pro.julleon.showcasespringbootrest.controllers;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;
import pro.julleon.showcasespringbootrest.http.dto.NewTaskPayload;
import pro.julleon.showcasespringbootrest.http.error.ErrorsPresentation;
import pro.julleon.showcasespringbootrest.models.Task;
import pro.julleon.showcasespringbootrest.repositories.TaskRepository;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TaskRestControllerTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    MessageSource messageSource;

    @InjectMocks
    TaskRestController taskRestController;


    @Test
    @DisplayName("GET /api/tasks returns http-response with status 200 ok and list of tasks")
    void handelGetAllTasks_ReturnsValidResponseEntity() {
        // Заданные данные
        List<Task> taskList = List.of(
                new Task(UUID.randomUUID(), "First task", false),
                new Task(UUID.randomUUID(), "Second task", true));

        // Настройка поведения макета taskRepository для возврата заданных данных
        Mockito.doReturn(taskList).when(this.taskRepository).findAll();

        // Вызов метода контроллера
        ResponseEntity<List<Task>> responseEntity = this.taskRestController.handelGetAllTasks();

        // Проверки на валидность возвращенного ResponseEntity
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        // Проверка, что тело ответа содержит те же задачи, которые были заданы в списке
        Assertions.assertEquals(taskList, responseEntity.getBody());
    }

    @Test
    @DisplayName("POST /api/tasks creates a new task when payload is valid " +
                 "and returns an response with status 200 ok the newly created task and its location")
    void handelCreateNewTask_ifPayloadIsValid_ReturnsValidResponseEntity() {
        // Заданные данные
        String description = "Third task";

        // Вызов метода контроллера
        ResponseEntity<?> responseEntity = this.taskRestController
                .handelCreateNewTask(new NewTaskPayload(description),
                        UriComponentsBuilder.fromUriString("http://localhost:8080"), Locale.ENGLISH);

        // Проверки на валидность возвращенного ResponseEntity
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        // Проверка, является ли тело ResponseEntity экземпляром Task
        if (responseEntity.getBody() instanceof Task task) {
            // Если является, то проверяем поля Task
            Assertions.assertNotNull(task.id());
            Assertions.assertEquals(description, task.description());
            Assertions.assertFalse(task.completed());

            // Проверяем, что URI в заголовках соответствует ожидаемому
            Assertions.assertEquals(URI.create("http://localhost:8080/api/tasks/" + task.id()),
                    responseEntity.getHeaders().getLocation());

            // Проверяем, что метод save был вызван у mock объекта taskRepository с аргументом task
            Mockito.verify(this.taskRepository).save(task);
        } else {
            // Если тело не является экземпляром Task, что-то неожиданное произошло
            Assertions.assertInstanceOf(Task.class, responseEntity.getBody());
        }

        // Проверяем, что методы taskRepository больше не были вызваны
        Mockito.verifyNoMoreInteractions(this.taskRepository);
    }

    @Test
    @DisplayName("POST /api/tasks when payload is invalid " +
                 "returns an response with status 400 bad request with error message")
    void handelCreateNewTask_ifPayloadIsInvalid_ReturnsValidResponseEntity() {
        // Заданные данные
        String description = "  "; // Невалидное описание, например, состоящее только из пробелов
        Locale locale = Locale.US;
        String errorMessage = "Description is empty"; // Ожидаемое сообщение об ошибке
        // Настройка поведения макета messageSource для возврата заданного сообщения об ошибке
        Mockito.doReturn(errorMessage).when(this.messageSource)
                .getMessage("tasks.create.description.errors.not_set", new Object[0], locale);

        // Вызов метода контроллера с невалидным payload
        ResponseEntity<?> responseEntity = this.taskRestController
                .handelCreateNewTask(new NewTaskPayload(description),
                        UriComponentsBuilder.fromUriString("http://localhost:8080"), locale);

        // Проверки на валидность возвращенного ResponseEntity
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());
        // Проверка, что тело ответа содержит ожидаемое сообщение об ошибке
        Assertions.assertEquals(new ErrorsPresentation(List.of(errorMessage)), responseEntity.getBody());

        // Проверяем, что методы taskRepository не были вызваны
        Mockito.verifyNoInteractions(this.taskRepository);
    }

    @Test
    @DisplayName("GET /api/tasks/{id} returns http-response with status 200 ok and task details when task exists")
    void handelFindTask_ReturnsValidResponseEntity() {
        // Заданные данные
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Test Task", false);

        // Настройка поведения макета taskRepository
        Mockito.doReturn(Optional.of(task)).when(this.taskRepository).findById(taskId);

        // Вызов метода контроллера
        ResponseEntity<Task> responseEntity = this.taskRestController.handelFindTask(taskId);

        // Проверки на валидность возвращенного ResponseEntity
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_JSON, responseEntity.getHeaders().getContentType());

        // Проверка, что тело ответа содержит ожидаемые детали задачи
        Assertions.assertEquals(task, responseEntity.getBody());

        // Проверяем, что метод findById был вызван с ожидаемым аргументом
        Mockito.verify(taskRepository).findById(taskId);
    }

    @Test
    @DisplayName("GET /api/tasks/{id} returns http-response with status 404 not found when task does not exist")
    void handelFindTask_ReturnsNotFoundForNonexistentTask() {
        // Заданные данные
        UUID nonExistentTaskId = UUID.randomUUID();

        // Настройка поведения макета taskRepository для возврата пустого Optional
        Mockito.when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

        // Вызов метода контроллера
        ResponseEntity<Task> responseEntity = this.taskRestController.handelFindTask(nonExistentTaskId);

        // Проверки на валидность возвращенного ResponseEntity
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        Assertions.assertNull(responseEntity.getBody()); // Для 404 Not Found тело обычно пустое

        // Проверяем, что метод findById был вызван с ожидаемым аргументом
        Mockito.verify(taskRepository).findById(nonExistentTaskId);
    }

}