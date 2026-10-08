package com.psg.model;

public enum ImplementationStatus {
    IMPLEMENTED,     // 100%
    IN_PROGRESS,     // 50%
    PLANNED,         // 25%
    NOT_PLANNED,     // 0%
    NOT_APPLICABLE   // Excluded from scoring, justification mandatory
}
