package com.rcloud.server.sealtalk.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class MessageVerifyListDTO {
    private int count;
    private List<MessageVerifyItemDTO> users;
}
