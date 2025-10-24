
package com.astrokiddo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateDeckRequestDto {
    @NotBlank
    private String topic;
    private String gradeLevel;
    private String locale;
}
