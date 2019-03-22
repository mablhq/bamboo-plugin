package com.mabl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class GetLabelsResult implements ApiResult {
    public Set<Label> labels;

    @JsonCreator
    public GetLabelsResult(
            @JsonProperty("labels") final Set<Label> labels
    ) {
        this.labels = labels;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Label {
        public final String name;
        public final String color;

        @JsonCreator
        public Label(
                @JsonProperty("name") final String name,
                @JsonProperty("color") final String color
        ) {
            this.name = name;
            this.color = color;
        }
    }

}
