package app;

import lombok.*;

@Getter
@Setter
public class District extends Model{
    private String provinceCode;
    private String provinceId;

    @Builder
    public District(String id, String code, String name, String provinceCode, String provinceId) {
        super(id, code, name);
        this.provinceCode = provinceCode;
        this.provinceId = provinceId;
    }
}
