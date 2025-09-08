package com.rcloud.server.sealtalk.model.dto;

import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.ValidateUtils;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BotDto {

    private String botId;
    private String name;
    private String portraitUri;
    private String openingMessage;
    private Integer botType;
    private List<BotIntegrateDTO> integrations;
    private List<PhoneDTO> phones;


    public void check() throws ParamException {
        ValidateUtils.notBlank(name,"name");
        ValidateUtils.notBlank(portraitUri, "portraitUri");
        ValidateUtils.checkLength(integrations, 1, 1,"integrations");
        for (BotIntegrateDTO integration : integrations) {
            ValidateUtils.notBlank(integration.getIntegrateType(), "integrateType");
            ValidateUtils.notBlank(integration.getCallbackUrl(), "callbackUrl");
            ValidateUtils.notNull(integration.getStream(),"stream");
            ValidateUtils.checkLength(integration.getObjectNames(), 1, 100,"objectNames");
            ValidateUtils.notBlank(integration.getApiKey(), "apiKey");
        }
        ValidateUtils.checkLength(phones, 0, 20,"phones");
        if (phones != null) {
            for (PhoneDTO phone : phones) {
                ValidateUtils.notBlank(phone.getRegion(), "region");
                ValidateUtils.notBlank(phone.getPhone(), "phone");
            }
        }
    }






}