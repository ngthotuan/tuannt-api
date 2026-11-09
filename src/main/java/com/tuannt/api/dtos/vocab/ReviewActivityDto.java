package com.tuannt.api.dtos.vocab;

/** Answers given on one UTC day (ISO date, e.g. 2026-11-09). Days without reviews are omitted. */
public record ReviewActivityDto(String day, long count) {
}
