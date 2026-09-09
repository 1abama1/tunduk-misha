package org.misha.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.client.ClientCreateRequest;
import org.misha.authservice.dto.client.ClientResponseDto;
import org.misha.authservice.entity.Address;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientTag;
import org.misha.authservice.repository.ClientRepository;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Изолированные модульные тесты сервисного слоя ClientDirectoryService.
 * Используется JUnit 5, Mockito и паттерн Arrange-Act-Assert (AAA).
 */
@ExtendWith(MockitoExtension.class)
class ClientDirectoryServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientDirectoryService clientService;

    private Client sampleClient;

    @BeforeEach
    void setUp() {
        sampleClient = Client.builder()
                .id(1L)
                .fullName("Алексей Смирнов")
                .whatsappPhone("+996555123456")
                .additionalPhone("+996777123456")
                .registrationAddress(new Address("Бишкек", "ул. Киевская 10"))
                .livingAddress(new Address("Бишкек", "ул. Киевская 10"))
                .passportNumber("ID123456")
                .passportIssuedAt(LocalDate.of(2020, 1, 15))
                .birthDate(LocalDate.of(1995, 5, 20))
                .pin("12345678901234")
                .birthYear(1995)
                .comment("Постоянный клиент")
                .tags(EnumSet.of(ClientTag.CLIENT))
                .build();
    }

    @Nested
    @DisplayName("Тестирование создания клиента (create)")
    class CreateTests {

        @Test
        @DisplayName("Успешное создание клиента с корректными данными")
        void create_WhenValidRequest_ShouldSaveAndReturnClientDto() {
            // [Arrange] Подготовка входных данных и моков
            AddressDto regAddressDto = new AddressDto("Бишкек", "ул. Киевская 10");
            AddressDto livAddressDto = new AddressDto("Бишкек", "ул. Киевская 10");
            ClientCreateRequest request = new ClientCreateRequest(
                    "Алексей Смирнов",
                    "+996555123456",
                    "+996777123456",
                    regAddressDto,
                    livAddressDto,
                    "ID123456",
                    LocalDate.of(2020, 1, 15),
                    LocalDate.of(1995, 5, 20),
                    "12345678901234",
                    1995,
                    "Постоянный клиент"
            );

            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
                Client entity = invocation.getArgument(0);
                entity.setId(100L);
                return entity;
            });

            // [Act] Вызов тестируемого метода
            ClientResponseDto result = clientService.create(request);

            // [Assert] Проверка вызовов репозитория и полученного результата
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.fullName()).isEqualTo("Алексей Смирнов");
            assertThat(result.whatsappPhone()).isEqualTo("+996555123456");
            assertThat(result.tags()).containsExactly(ClientTag.CLIENT);

            ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
            verify(clientRepository, times(1)).save(clientCaptor.capture());

            Client savedClient = clientCaptor.getValue();
            assertThat(savedClient.getFullName()).isEqualTo("Алексей Смирнов");
            assertThat(savedClient.getRegistrationAddress().getStreet()).isEqualTo("ул. Киевская 10");
            assertThat(savedClient.getTags()).contains(ClientTag.CLIENT);
        }
    }

    @Nested
    @DisplayName("Тестирование поиска и получения (getById / search)")
    class ReadTests {

        @Test
        @DisplayName("getById: Успешное получение по ID, если клиент существует")
        void getById_WhenClientExists_ShouldReturnDto() {
            // [Arrange]
            Long clientId = 1L;
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(sampleClient));

            // [Act]
            ClientResponseDto response = clientService.getById(clientId);

            // [Assert]
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.fullName()).isEqualTo("Алексей Смирнов");
            verify(clientRepository, times(1)).findById(clientId);
        }

        @Test
        @DisplayName("getById: Выброс IllegalArgumentException, если клиент не найден")
        void getById_WhenClientNotFound_ShouldThrowException() {
            // [Arrange]
            Long nonExistentId = 999L;
            when(clientRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // [Act & Assert]
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> clientService.getById(nonExistentId)
            );

            assertThat(ex.getMessage()).isEqualTo("Client not found");
            verify(clientRepository, times(1)).findById(nonExistentId);
        }

        @Test
        @DisplayName("search: Поиск по строке фильтра должен вызывать findByFullNameContainingIgnoreCase")
        void search_WhenNonBlankQuery_ShouldCallCustomQuery() {
            // [Arrange]
            String query = "Алексей";
            when(clientRepository.findByFullNameContainingIgnoreCase("Алексей"))
                    .thenReturn(List.of(sampleClient));

            // [Act]
            List<ClientResponseDto> results = clientService.search(query);

            // [Assert]
            assertThat(results).hasSize(1);
            assertThat(results.get(0).fullName()).isEqualTo("Алексей Смирнов");
            verify(clientRepository, times(1)).findByFullNameContainingIgnoreCase("Алексей");
            verify(clientRepository, never()).findAll();
        }

        @Test
        @DisplayName("search: Пустой запрос должен возвращать всех клиентов (findAll)")
        void search_WhenEmptyQuery_ShouldCallFindAll() {
            // [Arrange]
            when(clientRepository.findAll()).thenReturn(List.of(sampleClient));

            // [Act]
            List<ClientResponseDto> results = clientService.search("   ");

            // [Assert]
            assertThat(results).hasSize(1);
            verify(clientRepository, times(1)).findAll();
            verify(clientRepository, never()).findByFullNameContainingIgnoreCase(any());
        }
    }

    @Nested
    @DisplayName("Тестирование управления тегами (addTag / removeTag)")
    class TagTests {

        @Test
        @DisplayName("addTag: Успешное добавление тега к клиенту")
        void addTag_WhenValidTag_ShouldAppendTag() {
            // [Arrange]
            Long clientId = 1L;
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(sampleClient));

            // [Act]
            clientService.addTag(clientId, "REGULAR");

            // [Assert]
            assertThat(sampleClient.getTags()).contains(ClientTag.REGULAR, ClientTag.CLIENT);
            verify(clientRepository, times(1)).findById(clientId);
        }

        @Test
        @DisplayName("removeTag: Успешное удаление тега у клиента")
        void removeTag_WhenTagPresent_ShouldRemoveTag() {
            // [Arrange]
            Long clientId = 1L;
            sampleClient.getTags().add(ClientTag.REGULAR);
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(sampleClient));

            // [Act]
            clientService.removeTag(clientId, "REGULAR");

            // [Assert]
            assertThat(sampleClient.getTags()).doesNotContain(ClientTag.REGULAR);
            assertThat(sampleClient.getTags()).contains(ClientTag.CLIENT);
            verify(clientRepository, times(1)).findById(clientId);
        }

        @Test
        @DisplayName("addTag: Выброс IllegalArgumentException при невалидном имени тега")
        void addTag_WhenInvalidTag_ShouldThrowException() {
            // [Arrange]
            Long clientId = 1L;
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(sampleClient));

            // [Act & Assert]
            assertThrows(IllegalArgumentException.class, () -> clientService.addTag(clientId, "UNKNOWN_TAG"));
        }
    }
}
