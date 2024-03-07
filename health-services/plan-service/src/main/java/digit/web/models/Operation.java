package digit.web.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;

/**
 * Operation
 */
@Validated
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Operation {
    @JsonProperty("id")
    @Valid
    @Size(min = 2, max = 64)
    private String id = null;

    @JsonProperty("input")
    @NotNull
    @Size(min = 1, max = 256)
    private String input = null;

    @JsonProperty("operator")
    @NotNull
    @Size(min = 2, max = 64)
    private OperatorEnum operator = null;

    @JsonProperty("assumptionValue")
    @NotNull
    @Size(min = 2, max = 256)
    private String assumptionValue = null;

    @JsonProperty("output")
    @NotNull
    @Size(min = 1, max = 64)
    private String output = null;

    /**
     * The operator used in the operation
     */
    public enum OperatorEnum {
        PLUS("+"),

        MINUS("-"),

        SLASH("/"),

        STAR("*"),

        PERCENT("%"),

        _U("**");

        private String value;

        OperatorEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static OperatorEnum fromValue(String text) {
            for (OperatorEnum b : OperatorEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

}
