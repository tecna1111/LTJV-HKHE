package com.cosre.cosre_backend.modules.checkpoint.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CheckpointAssignmentId implements Serializable {
    private Long checkpointId;
    private Long studentId;
}