package org.misha.authservice.mapper;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.ClientDto;
import org.misha.authservice.dto.ClientImageDto;
import org.misha.authservice.dto.DocumentDetailDto;
import org.misha.authservice.dto.DocumentDto;
import org.misha.authservice.dto.PassportDto;
import org.misha.authservice.dto.ToolDto;
import org.misha.authservice.entity.Address;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientImage;
import org.misha.authservice.entity.ClientPassport;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final ToolRepository toolRepository;

    public ClientDto toDto(Client c) {
        return ClientDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .whatsappPhone(c.getWhatsappPhone())
                .registrationAddress(toAddressDto(c.getRegistrationAddress()))
                .livingAddress(toAddressDto(c.getLivingAddress()))
                .objectAddress(c.getObjectAddress())
                .email(c.getEmail())
                .birthDate(c.getBirthDate())
                .comment(c.getComment())
                .passport(toPassportDto(c.getPassport()))
                .tag(c.getTag() != null ? c.getTag().name() : null)
                .documents(
                        c.getDocuments() == null || c.getDocuments().isEmpty()
                                ? new ArrayList<>()
                                : c.getDocuments().stream()
                                        .map(this::toDocDto)
                                        .toList())
                .build();
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null)
            return null;
        return new AddressDto(address.getRegion(), address.getStreet());
    }

    public DocumentDto toDocDto(RentalDocument d) {
        // Получаем инструмент из загруженной коллекции
        Tool tool = d.getTools() != null && !d.getTools().isEmpty()
                ? d.getTools().get(0)
                : null;

        // Если инструмент не найден в коллекции (например, после закрытия контракта),
        // но toolId сохранен в документе, пытаемся загрузить инструмент по ID
        Long toolId = d.getToolId();
        if (tool == null && toolId != null) {
            tool = toolRepository.findByIdWithTemplateAndContract(toolId).orElse(null);
        }

        // Если все еще не найден, пытаемся найти через репозиторий по contractId
        if (tool == null) {
            List<Tool> tools = toolRepository.findByContractIdWithTemplate(d.getId());
            if (!tools.isEmpty()) {
                tool = tools.get(0);
                toolId = tool.getId();
            }
        } else {
            // Используем toolId из инструмента, если он найден
            toolId = tool.getId();
        }

        String categoryName = null;
        String toolName = null;
        String serialNumber = null;

        if (tool != null) {
            if (tool.getTemplate() != null) {
                toolName = tool.getTemplate().getName();
                serialNumber = tool.getSerialNumber();
                if (tool.getTemplate().getCategory() != null) {
                    categoryName = tool.getTemplate().getCategory().getName();
                }
            }
        }

        return DocumentDto.builder()
                .id(d.getId())
                .contractNumber(d.getContractNumber())
                .category(categoryName)
                .toolName(toolName)
                .serialNumber(serialNumber)
                .startDateTime(d.getStartDateTime())
                .amount(d.getAmount())
                .toolId(toolId) // Используем сохраненный toolId или ID найденного инструмента
                .returnDate(d.getReturnDate())
                .terminatedAt(d.getTerminatedAt())
                .terminationReason(d.getTerminationReason())
                .status(d.getStatus())
                .build();
    }

    public DocumentDetailDto toDetailDto(RentalDocument d) {
        // Получаем инструмент
        Tool tool = d.getTools() != null && !d.getTools().isEmpty()
                ? d.getTools().get(0)
                : null;

        Long toolId = d.getToolId();
        if (tool == null && toolId != null) {
            tool = toolRepository.findByIdWithTemplateAndContract(toolId).orElse(null);
        }

        if (tool == null) {
            List<Tool> tools = toolRepository.findByContractIdWithTemplate(d.getId());
            if (!tools.isEmpty()) {
                tool = tools.get(0);
            }
        }

        ToolDto toolDto = tool != null ? ToolDto.fromEntity(tool) : null;

        // Для обратной совместимости или если в tool нет инфы, но она есть в d
        if (toolDto != null && toolDto.getCategoryName() == null && tool != null && tool.getTemplate() != null
                && tool.getTemplate().getCategory() != null) {
            toolDto.setCategoryName(tool.getTemplate().getCategory().getName());
        }

        return DocumentDetailDto.builder()
                .id(d.getId())
                .contractNumber(d.getContractNumber())
                .amount(d.getAmount())
                .dailyPrice(d.getDailyPrice())
                .startDateTime(d.getStartDateTime())
                .createdAt(d.getCreatedAt())
                .returnDate(d.getReturnDate())
                .terminatedAt(d.getTerminatedAt())
                .terminationReason(d.getTerminationReason())
                .status(d.getStatus())
                .comment(d.getComment())
                .clientId(d.getClient() != null ? d.getClient().getId() : null)
                .client(d.getClient() != null ? toDtoForDetail(d.getClient()) : null)
                .toolId(toolId != null ? toolId : (tool != null ? tool.getId() : null))
                .tool(toolDto)
                .build();
    }

    private ClientDto toDtoForDetail(Client c) {
        // Маппим клиента БЕЗ документов, чтобы избежать бесконечной рекурсии и лишних
        // данных
        return ClientDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .whatsappPhone(c.getWhatsappPhone())
                .registrationAddress(toAddressDto(c.getRegistrationAddress()))
                .livingAddress(toAddressDto(c.getLivingAddress()))
                .objectAddress(c.getObjectAddress())
                .email(c.getEmail())
                .birthDate(c.getBirthDate())
                .comment(c.getComment())
                .passport(toPassportDto(c.getPassport()))
                .tag(c.getTag() != null ? c.getTag().name() : null)
                .build();
    }

    private PassportDto toPassportDto(ClientPassport passport) {
        if (passport == null) {
            return null;
        }
        return new PassportDto(
                passport.getSeries(),
                passport.getNumber(),
                passport.getIssuedBy(),
                passport.getSubdivisionCode(),
                passport.getIssueDate(),
                passport.getInn());
    }

    public List<ClientImageDto> toImageDtos(List<ClientImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .map(img -> new ClientImageDto(
                        img.getId(),
                        img.getFileName(),
                        img.getFileType()))
                .toList();
    }
}
