package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "client")
@ToString(exclude = "client")
public class ClientImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
}

