package com.redhat.agogos.interceptors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class TriggerContext {

    @Getter
    @Setter
    @JsonProperty("event_url")
    private String eventURL;

    @Getter
    @Setter
    @JsonProperty("event_id")
    private String eventID;

    @Getter
    @Setter
    @JsonProperty("trigger_id")
    private String triggerID;
}
