package org.egov.common.models.core;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import digit.models.coremodels.AuditDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.egov.common.models.facility.AdditionalFields;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EgovModel {
    @JsonProperty("id")
    @Size(min = 2, max = 64)
    protected String id;

    @JsonProperty("tenantId")
    @NotNull
    @Size(min = 2, max = 1000)
    protected String tenantId;

    @JsonProperty("status")
    protected String status;

    @JsonProperty("source")
    protected String source;

    @JsonProperty("rowVersion")
    protected Integer rowVersion;

    @JsonProperty("applicationId")
    protected String applicationId;

    @JsonProperty("hasErrors")
    protected Boolean hasErrors = Boolean.FALSE;

    @JsonProperty("additionalFields")
    @Valid
    protected AdditionalFields additionalFields;

    @JsonProperty("auditDetails")
    @Valid
    protected AuditDetails auditDetails;

}
