package com.device.management.model;

import com.device.management.common.DeviceState;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private DeviceState state;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Optimistic locking — prevents lost updates under concurrent modification.
    // The client receives the current version in DeviceResponse and must
    // include it in PUT/PATCH requests. If two clients update the same device
    // simultaneously, the second one gets a 409 CONCURRENT_MODIFICATION.
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public DeviceState getState() { return state; }
    public void setState(DeviceState state) { this.state = state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
