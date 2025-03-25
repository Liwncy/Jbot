package x.ovo.jbot.plugin.xuanxue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CesuanDto {
    //
    private String chatId;
    private String serviceName;
    private String name;
    private String sex;
    private String type = "gongli";
    private String year;
    private String month;
    private String day;
    private String hours;
    private String minute = "00";
    private String sect = "1";
}
