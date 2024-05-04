package org.egov.transformer.models.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectStaffIndexV1 {
    @JsonProperty("id")
    private String id;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("projectId")
    private String projectId;
    @JsonProperty("userName")
    private String userName;
    @JsonProperty("nameOfUser")
    private String nameOfUser;
    @JsonProperty("userAddress")
    private String userAddress;
    @JsonProperty("role")
    private String role;
    @JsonProperty("taskDates")
    private List<String> taskDates;
    @JsonProperty("boundaryHierarchy")
    private Map<String, String> boundaryHierarchy;
    @JsonProperty("tenantId")
    private String tenantId;
    @JsonProperty("projectType")
    private String projectType;
    @JsonProperty("projectTypeId")
    private String projectTypeId;
    @JsonProperty("createdBy")
    private String createdBy;
    @JsonProperty("createdTime")
    private Long createdTime;
    @JsonProperty("additionalDetails")
    private JsonNode additionalDetails;
    @JsonProperty("isDeleted")
    private Boolean isDeleted;
    @JsonProperty("localityCode")
    private String localityCode;

}
