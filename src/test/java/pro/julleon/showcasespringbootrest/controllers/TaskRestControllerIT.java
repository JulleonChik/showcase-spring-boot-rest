package pro.julleon.showcasespringbootrest.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import pro.julleon.showcasespringbootrest.models.Task;
import pro.julleon.showcasespringbootrest.repositories.impl.InMemoryTaskRepositoryImpl;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class TaskRestControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    InMemoryTaskRepositoryImpl taskRepository;

    @AfterEach
    void tearDown() {
        // Очищение репозитория после каждого теста
        this.taskRepository.getTasks().clear();
    }

    @Test
    @DisplayName("GET /api/tasks returns http-response with status 200 ok and list of tasks")
    void handelGetAllTasks_ReturnsValidResponseEntity() throws Exception {
        // Заданные данные
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder =
                MockMvcRequestBuilders.get("/api/tasks");
        this.taskRepository.getTasks().addAll(List.of(
                new Task(UUID.fromString("045ea98e-0320-4489-92d3-1cde446f94b1"), "First task", false),
                new Task(UUID.fromString("5fe9c6b8-af62-460c-9320-a3cdc16ad5be"), "Second task", true)
        ));

        // Выполнение запроса и проверки результата
        this.mockMvc
                .perform(mockHttpServletRequestBuilder)
                .andExpectAll(
                        MockMvcResultMatchers.status().isOk(),
                        MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON),
                        MockMvcResultMatchers.content().json(
                                """
                                        [
                                            {
                                                "id": "045ea98e-0320-4489-92d3-1cde446f94b1",
                                                "description": "First task",
                                                "completed": false
                                            },
                                            {
                                                "id": "5fe9c6b8-af62-460c-9320-a3cdc16ad5be",
                                                "description": "Second task",
                                                "completed": true
                                            }
                                        ]
                                        """
                        )
                );
    }

    @Test
    @DisplayName("POST /api/tasks creates a new task when payload is valid " +
                 "and returns an response with status 200 ok the newly created task and its location")
    void handelCreateNewTask_ifPayloadIsValid_ReturnsValidResponseEntity() throws Exception {
        // Заданные данные
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder =
                MockMvcRequestBuilders.post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                 "description": "Third task"
                                }
                                """
                        );

        // Выполнение запроса и проверки результата
        mockMvc
                .perform(mockHttpServletRequestBuilder)
                .andExpectAll(
                        MockMvcResultMatchers.status().isCreated(),
                        MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON),
                        MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION),
                        MockMvcResultMatchers.content().json("""
                                {
                                "description" : "Third task",
                                "completed": false
                                }
                                """
                        ),
                        MockMvcResultMatchers.jsonPath("$.id").exists()
                );

        // Проверка, что задача была добавлена в репозиторий
        Assertions.assertEquals(1, this.taskRepository.getTasks().size());
        Task task = this.taskRepository.getTasks().get(0);
        Assertions.assertNotNull(task.id());
        Assertions.assertEquals("Third task", task.description());
        Assertions.assertFalse(task.completed());
    }

    @Test
    @DisplayName("POST /api/tasks when payload is invalid " +
                 "returns an response with status 400 bad request with error message")
    void handelCreateNewTask_ifPayloadIsInvalid_ReturnsValidResponseEntity() throws Exception {
        // Заданные данные
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder =
                MockMvcRequestBuilders.post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content("""
                                {
                                    "description": null
                                }
                                """);

        // Выполнение запроса и проверки результата
        mockMvc
                .perform(mockHttpServletRequestBuilder)
                .andExpectAll(
                        MockMvcResultMatchers.status().isBadRequest(),
                        MockMvcResultMatchers.header().doesNotExist(HttpHeaders.LOCATION),
                        MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON),
                        MockMvcResultMatchers.content().json("""
                                {
                                    "errors": ["Task description must be set"]
                                }
                                """, true)
                );

        // Проверка, что задача не была добавлена в репозиторий
        Assertions.assertTrue(this.taskRepository.getTasks().isEmpty());
    }

    @Test
    @DisplayName("GET /api/tasks/{id} returns http-response with status 200 ok and task details when task exists")
    void handelFindTask_ReturnsValidResponseEntityWhenTaskExists() throws Exception {
        // Заданные данные
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Test Task", false);

        // Добавляем задачу в репозиторий
        this.taskRepository.save(task);

        // Выполнение запроса и проверки результата
        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/tasks/{id}", taskId))
                .andExpectAll(
                        MockMvcResultMatchers.status().isOk(),
                        MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON),
                        MockMvcResultMatchers.content().json("""
                                {
                                    "id": "%s",
                                    "description": "Test Task",
                                    "completed": false
                                }
                                """.formatted(taskId.toString())
                        )
                );
    }

    @Test
    @DisplayName("GET /api/tasks/{id} returns http-response with status 404 not found when task does not exist")
    void handelFindTask_ReturnsNotFoundWhenTaskDoesNotExist() throws Exception {
        // Заданные данные
        UUID taskId = UUID.randomUUID();

        // Выполнение запроса и проверки результата
        mockMvc
                .perform(MockMvcRequestBuilders.get("/api/tasks/{id}", taskId))
                .andExpectAll(
                        MockMvcResultMatchers.status().isNotFound(),
                        MockMvcResultMatchers.content().string("") // Проверка, что тело ответа пусто
                );
    }
}
