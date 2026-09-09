package org.misha.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.client.ClientCreateRequest;
import org.misha.authservice.dto.client.ClientResponseDto;
import org.misha.authservice.entity.ClientTag;
import org.misha.authservice.exception.GlobalExceptionHandler;
import org.misha.authservice.security.EmailPhoneAuthenticationProvider;
import org.misha.authservice.security.JwtFilter;
import org.misha.authservice.security.JwtUtil;
import org.misha.authservice.security.SecurityConfig;
import org.misha.authservice.service.ClientDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Изолированные тесты REST-контроллера ClientController с использованием MockMvc.
 * Полностью изолирован от компонентов Spring Security (SecurityConfig, JwtFilter).
 */
@WebMvcTest(
        controllers = ClientController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class, EmailPhoneAuthenticationProvider.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientDirectoryService clientService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/clients - 200 OK: Успешное создание клиента при валидном DTO")
    void create_WhenValidPayload_ShouldReturn200AndClientJson() throws Exception {
        // [Arrange] Подготовка данных запроса и ожидаемого ответа сервиса
        ClientCreateRequest request = new ClientCreateRequest(
                "Иван Петров",
                "+996700112233",
                null,
                new AddressDto("Чуй", "ул. Ленина 5"),
                null,
                "ID987654",
                LocalDate.of(2021, 5, 10),
                LocalDate.of(1990, 8, 20),
                "22008199000111",
                1990,
                "Новый клиент"
        );

        ClientResponseDto responseDto = ClientResponseDto.builder()
                .id(15L)
                .fullName("Иван Петров")
                .whatsappPhone("+996700112233")
                .registrationAddress(new AddressDto("Чуй", "ул. Ленина 5"))
                .tags(Set.of(ClientTag.CLIENT))
                .build();

        when(clientService.create(any(ClientCreateRequest.class))).thenReturn(responseDto);

        // [Act & Assert] Вызов эндпоинта и проверка JSON ответа
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(15)))
                .andExpect(jsonPath("$.fullName", is("Иван Петров")))
                .andExpect(jsonPath("$.whatsappPhone", is("+996700112233")))
                .andExpect(jsonPath("$.tags", hasItem("CLIENT")));

        verify(clientService, times(1)).create(any(ClientCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/clients - 400 Bad Request: Ошибка валидации при пустых обязательных полях")
    void create_WhenRequiredFieldsBlank_ShouldReturn400AndValidationError() throws Exception {
        // [Arrange] DTO с нарушением аннотаций @NotBlank
        ClientCreateRequest invalidRequest = new ClientCreateRequest(
                "", // Blank fullName
                "", // Blank whatsappPhone
                null, null, null, null, null, null, null, null, null
        );

        // [Act & Assert]
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("fullName is required")))
                .andExpect(jsonPath("$.message", containsString("whatsappPhone is required")));

        verify(clientService, never()).create(any());
    }

    @Test
    @DisplayName("GET /api/clients/{id} - 200 OK: Клиент найден, возвращается объект")
    void getById_WhenFound_ShouldReturn200AndJson() throws Exception {
        // [Arrange]
        Long clientId = 10L;
        ClientResponseDto responseDto = ClientResponseDto.builder()
                .id(clientId)
                .fullName("Мария Сидорова")
                .whatsappPhone("+996500998877")
                .tags(Set.of(ClientTag.CLIENT))
                .build();

        when(clientService.getById(clientId)).thenReturn(responseDto);

        // [Act & Assert]
        mockMvc.perform(get("/api/clients/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.fullName", is("Мария Сидорова")));

        verify(clientService, times(1)).getById(clientId);
    }

    @Test
    @DisplayName("GET /api/clients/{id} - 400 Bad Request: Клиент не найден -> ответ из GlobalExceptionHandler")
    void getById_WhenNotFound_ShouldReturn400FromExceptionHandler() throws Exception {
        // [Arrange]
        Long nonExistentId = 999L;
        when(clientService.getById(nonExistentId)).thenThrow(new IllegalArgumentException("Client not found"));

        // [Act & Assert]
        mockMvc.perform(get("/api/clients/{id}", nonExistentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", is("Client not found")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(clientService, times(1)).getById(nonExistentId);
    }

    @Test
    @DisplayName("GET /api/clients/search - 200 OK: Успешный возврат списка найденных клиентов")
    void search_WhenQueryPassed_ShouldReturnJsonArray() throws Exception {
        // [Arrange]
        String query = "Иван";
        ClientResponseDto client = ClientResponseDto.builder()
                .id(1L)
                .fullName("Иван Иванов")
                .whatsappPhone("+996555111222")
                .build();

        when(clientService.search(query)).thenReturn(List.of(client));

        // [Act & Assert]
        mockMvc.perform(get("/api/clients/search")
                        .param("q", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName", is("Иван Иванов")));

        verify(clientService, times(1)).search(query);
    }

    @Test
    @DisplayName("POST /api/clients/{id}/tags/{tag} - 200 OK: Успешный вызов добавления тега")
    void addTag_ShouldCallServiceAndReturn200() throws Exception {
        // [Arrange]
        Long clientId = 5L;
        String tag = "REGULAR";
        doNothing().when(clientService).addTag(clientId, tag);

        // [Act & Assert]
        mockMvc.perform(post("/api/clients/{id}/tags/{tag}", clientId, tag))
                .andExpect(status().isOk());

        verify(clientService, times(1)).addTag(eq(clientId), eq(tag));
    }
}
