package com.astrokiddo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApodResponseDto {
    private String date;
    private String title;
    private String explanation;

    @JsonProperty("media_type")
    private String mediaType;

    private String url;
    private String hdurl;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    private String copyright;
    @JsonProperty("service_version")
    private String serviceVersion;
}
