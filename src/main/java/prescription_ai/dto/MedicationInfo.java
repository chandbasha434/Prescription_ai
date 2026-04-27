package prescription_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MedicationInfo {

    private String name;
    private String use;

    public MedicationInfo() {}

    public MedicationInfo(String name, String use) {
        this.name = name;
        this.use  = use;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUse()  { return use; }
    public void setUse(String use) { this.use = use; }
}
