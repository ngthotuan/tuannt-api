package com.tuannt.api.dtos.vocab;

/** A word topic and how many words carry it. */
public record TopicCountDto(String topic, long count) {
}
