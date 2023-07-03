package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.redhat.agogos.ResultableResourceStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class ResultableStatus extends AgogosResourceStatus {

    public ResultableStatus() {
        status = String.valueOf(ResultableResourceStatus.New);
    }

    @Getter
    @Setter
    String startTime;

    @Getter
    @Setter
    String completionTime;

    @Getter
    @Setter
    // explicit parameters are needed to avoid crashing, see https://github.com/sundrio/sundrio/issues/398
    private Map<Object, Object> result;

    @JsonIgnore
    public ZonedDateTime startTime() {
        if (this.startTime == null) {
            return null;
        }

        return LocalDateTime.parse(this.startTime,
                DateTimeFormatter.ISO_ZONED_DATE_TIME).atZone(ZoneId.of("UTC"));
    }

    @JsonIgnore
    public ZonedDateTime completionTime() {
        if (this.completionTime == null) {
            return null;
        }

        return LocalDateTime.parse(this.completionTime,
                DateTimeFormatter.ISO_ZONED_DATE_TIME).atZone(ZoneId.of("UTC"));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResultableStatus)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false;
        }

        ResultableStatus status = (ResultableStatus) obj;

        if (Objects.equals(status.getStatus(), getStatus()) //
                && Objects.equals(status.getStartTime(), getStartTime())//
                && Objects.equals(status.getCompletionTime(), getCompletionTime()) //
                && Objects.equals(status.getResult(), getResult())) {

            return true;
        }

        return false;
    }
}
