package org.mskcc.limsrest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Setter
public class ExemplarConfig {
    protected String visiumImagePath;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getVisiumImagePath() {
        return visiumImagePath;
    }
}