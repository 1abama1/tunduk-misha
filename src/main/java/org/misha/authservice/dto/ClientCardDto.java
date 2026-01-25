package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientCardDto {
    private Long id;
    private String fullName;
    private String phone;
    private AddressDto registrationAddress;
    private AddressDto livingAddress;
    private String objectAddress;
    private String email;
    private String tag;
    private List<ActiveContractDto> activeContracts;
    private List<ContractHistoryDto> history;
    private List<ClientImageDto> images;
}
