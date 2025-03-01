package com.mahitotsu.steropes.api.orm;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.NONE)
public abstract class EntityBase {
    
    @GeneratedValue
    @Id
    @Column(name="id", nullable = false, unique = true, insertable = false, updatable = false)
    private UUID id;
}
