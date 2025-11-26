package com.rcloud.server.sealtalk.param;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@NoArgsConstructor
public class FriendRouteParam {

    private Integer eventType;
    private String userId;
    private String toUserId;
    private Long time;
    private String source;
}
