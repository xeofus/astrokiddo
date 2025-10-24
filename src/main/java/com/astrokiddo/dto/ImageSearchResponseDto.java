package com.astrokiddo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ImageSearchResponseDto {
    public Collection collection;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Collection {
        private String version;
        private String href;
        private List<Item> items;
        private Metadata metadata;
        private List<Link> links;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String href;
        private List<Data> data;
        private List<Link> links;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String center;
        private String title;
        private String description;

        @JsonProperty("nasa_id")
        private String nasaId;

        @JsonProperty("media_type")
        private String mediaType;

        @JsonProperty("date_created")
        private Instant dateCreated;

        private String photographer;

        @JsonProperty("secondary_creator")
        private String secondaryCreator;

        private List<String> keywords;
        private List<String> album;
        private String location;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
        private String rel;
        private String render;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("total_hits")
        private int totalHits;
    }
}
