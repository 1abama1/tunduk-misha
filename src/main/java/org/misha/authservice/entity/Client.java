package org.misha.authservice.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "clients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "documents", "passport" })
@ToString(exclude = { "documents", "passport" })
public class Client {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String fullName;

        @Column(unique = true)
        private String phone;

        @Column(unique = true)
        private String email;

        private String whatsappPhone;

        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "region", column = @Column(name = "reg_region")),
                        @AttributeOverride(name = "street", column = @Column(name = "reg_street"))
        })
        private Address registrationAddress; // Адрес из паспорта

        @Embedded
        @AttributeOverrides({
                        @AttributeOverride(name = "region", column = @Column(name = "live_region")),
                        @AttributeOverride(name = "street", column = @Column(name = "live_street"))
        })
        private Address livingAddress; // Фактический адрес проживания

        private String objectAddress; // Адрес объекта

        private LocalDate birthDate;
        private Integer birthYear;
        private String passportNumber;
        private LocalDate passportIssuedAt;
        private String pin;

        @Column(columnDefinition = "TEXT")
        private String comment;

        @Enumerated(EnumType.STRING)
        private Tag tag;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "client_tags", joinColumns = @JoinColumn(name = "client_id"))
        @Enumerated(EnumType.STRING)
        @Builder.Default
        private Set<ClientTag> tags = new HashSet<>();

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "last_branch_id")
        private Branch lastBranch;

        @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
        private ClientPassport passport;

        @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonManagedReference
        @Builder.Default
        private List<RentalDocument> documents = new ArrayList<>();

}
