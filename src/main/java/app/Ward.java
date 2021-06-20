package app;

import lombok.*;

@Getter
@Setter
public class Ward extends Model{
    private String districtCode;
    private String districtId;

    @Builder
    public Ward(String id, String code, String name, String districtCode, String districtId) {
        super(id, code, name);
        this.districtCode = districtCode;
        this.districtId = districtId;
    }
}
