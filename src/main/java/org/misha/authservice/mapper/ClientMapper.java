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
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final ToolInstanceRepository ToolInstanceRepository;

    public ClientDto toDto(Client c) {
        return ClientDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .whatsappPhone(c.getWhatsappPhone())
                .additionalPhone(c.getAdditionalPhone())
                .registrationAddress(toAddressDto(c.getRegistrationAddress()))
                .livingAddress(toAddressDto(c.getLivingAddress()))
                .objectAddress(c.getObjectAddress())
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
        // РџРѕР»СѓС‡Р°РµРј РёРЅСЃС‚СЂСѓРјРµРЅС‚ РёР· Р·Р°РіСЂСѓР¶РµРЅРЅРѕР№ РєРѕР»Р»РµРєС†РёРё
        ToolInstance ToolInstance = d.getTools() != null && !d.getTools().isEmpty()
                ? d.getTools().get(0)
                : null;

        // Р•СЃР»Рё РёРЅСЃС‚СЂСѓРјРµРЅС‚ РЅРµ РЅР°Р№РґРµРЅ РІ РєРѕР»Р»РµРєС†РёРё (РЅР°РїСЂРёРјРµСЂ, РїРѕСЃР»Рµ Р·Р°РєСЂС‹С‚РёСЏ РєРѕРЅС‚СЂР°РєС‚Р°),
        // РЅРѕ toolId СЃРѕС…СЂР°РЅРµРЅ РІ РґРѕРєСѓРјРµРЅС‚Рµ, РїС‹С‚Р°РµРјСЃСЏ Р·Р°РіСЂСѓР·РёС‚СЊ РёРЅСЃС‚СЂСѓРјРµРЅС‚ РїРѕ ID
        Long toolId = d.getToolId();
        if (ToolInstance == null && toolId != null) {
            ToolInstance = ToolInstanceRepository.findByIdWithTemplateAndContract(toolId).orElse(null);
        }

        // Р•СЃР»Рё РІСЃРµ РµС‰Рµ РЅРµ РЅР°Р№РґРµРЅ, РїС‹С‚Р°РµРјСЃСЏ РЅР°Р№С‚Рё С‡РµСЂРµР· СЂРµРїРѕР·РёС‚РѕСЂРёР№ РїРѕ contractId
        if (ToolInstance == null) {
            List<ToolInstance> tools = ToolInstanceRepository.findByContractIdWithTemplate(d.getId());
            if (!tools.isEmpty()) {
                ToolInstance = tools.get(0);
                toolId = ToolInstance.getId();
            }
        } else {
            // РСЃРїРѕР»СЊР·СѓРµРј toolId РёР· РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°, РµСЃР»Рё РѕРЅ РЅР°Р№РґРµРЅ
            toolId = ToolInstance.getId();
        }

        String categoryName = null;
        String toolName = null;
        String serialNumber = null;

        if (ToolInstance != null) {
            if (ToolInstance.getTemplate() != null) {
                toolName = ToolInstance.getTemplate().getName();
                serialNumber = ToolInstance.getInventoryNumber();
                if (ToolInstance.getTemplate().getCategory() != null) {
                    categoryName = ToolInstance.getTemplate().getCategory().getName();
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
                .toolId(toolId) // РСЃРїРѕР»СЊР·СѓРµРј СЃРѕС…СЂР°РЅРµРЅРЅС‹Р№ toolId РёР»Рё ID РЅР°Р№РґРµРЅРЅРѕРіРѕ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°
                .returnDate(d.getReturnDate())
                .terminatedAt(d.getTerminatedAt())
                .terminationReason(d.getTerminationReason())
                .status(d.getStatus())
                .build();
    }

    public DocumentDetailDto toDetailDto(RentalDocument d) {
        // Получаем инструмент
        ToolInstance ToolInstance = d.getTools() != null && !d.getTools().isEmpty()
                ? d.getTools().get(0)
                : null;

        // но toolId сохранен в документе, пытаемся загрузить инструмент по ID
        Long toolId = d.getToolId();
        if (ToolInstance == null && toolId != null) {
            ToolInstance = ToolInstanceRepository.findByIdWithTemplateAndContract(toolId).orElse(null);
        }

        if (ToolInstance == null) {
            List<ToolInstance> tools = ToolInstanceRepository.findByContractIdWithTemplate(d.getId());
            if (!tools.isEmpty()) {
                ToolInstance = tools.get(0);
            }
        }

        ToolDto toolDto = ToolInstance != null ? ToolDto.fromEntity(ToolInstance) : null;

        // Р”Р»СЏ РѕР±СЂР°С‚РЅРѕР№ СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚Рё РёР»Рё РµСЃР»Рё РІ ToolInstance РЅРµС‚ РёРЅС„С‹, РЅРѕ РѕРЅР° РµСЃС‚СЊ РІ d
        if (toolDto != null && toolDto.getCategoryName() == null && ToolInstance != null && ToolInstance.getTemplate() != null
                && ToolInstance.getTemplate().getCategory() != null) {
            toolDto.setCategoryName(ToolInstance.getTemplate().getCategory().getName());
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
                .toolId(toolId != null ? toolId : (ToolInstance != null ? ToolInstance.getId() : null))
                .ToolInstance(toolDto)
                .build();
    }

    private ClientDto toDtoForDetail(Client c) {
        // РњР°РїРїРёРј РєР»РёРµРЅС‚Р° Р‘Р•Р— РґРѕРєСѓРјРµРЅС‚РѕРІ, С‡С‚РѕР±С‹ РёР·Р±РµР¶Р°С‚СЊ Р±РµСЃРєРѕРЅРµС‡РЅРѕР№ СЂРµРєСѓСЂСЃРёРё Рё Р»РёС€РЅРёС…
        // РґР°РЅРЅС‹С…
        return ClientDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .whatsappPhone(c.getWhatsappPhone())
                .additionalPhone(c.getAdditionalPhone())
                .registrationAddress(toAddressDto(c.getRegistrationAddress()))
                .livingAddress(toAddressDto(c.getLivingAddress()))
                .objectAddress(c.getObjectAddress())
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

