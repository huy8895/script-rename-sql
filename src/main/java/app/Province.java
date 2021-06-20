package app;

import lombok.*;

@Getter
@Setter
public class Province extends Model{
    @Builder
    public Province(String id, String code, String name) {
        super(id, code, name);
    }
}
