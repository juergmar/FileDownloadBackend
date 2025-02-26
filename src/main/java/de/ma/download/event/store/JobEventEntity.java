package de.ma.download.event.store;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(name = "job_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class JobEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobEventType eventType;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Lob
    @Column(name = "event_data")
    private byte[] eventData;

    @Column(name = "sequence", nullable = false)
    private Long sequence;
}

