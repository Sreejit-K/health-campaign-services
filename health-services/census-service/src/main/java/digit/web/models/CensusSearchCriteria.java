package digit.web.models;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;

/**
 * CensusSearchCriteria
 */
@Validated
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CensusSearchCriteria {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("tenantId")
    @Size(min = 1, max = 100)
    private String tenantId = null;

    @JsonProperty("areaCodes")
    private List<String> areaCodes = null;


    public CensusSearchCriteria addAreaCodesItem(String areaCodesItem) {
        if (this.areaCodes == null) {
            this.areaCodes = new ArrayList<>();
        }
        this.areaCodes.add(areaCodesItem);
        return this;
    }

}
