package com.rcloud.server.sealtalk.param;

import com.rcloud.server.sealtalk.util.JacksonUtil;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 *
 */
@Data
@NoArgsConstructor
public class UserProfileRouteParam {



    private String userId;
    private Integer type;
    private Long time;
    private String userProfile;
    private String userExtProfile;



    @Data
    @NoArgsConstructor
    private static class BaseProfile{

        private String uniqueId;
        private String name;
        private String portraitUri;
        private String email;
        private String birthday;
        private Integer gender;
        private String location;
        private Integer role;
        private Integer level;
    }

    public String userName() {
        try {
            BaseProfile profile = JacksonUtil.fromJson(this.userProfile, BaseProfile.class);
            return profile.getName();
        } catch (Exception ignore) {
        }
        return null;
    }

    public String portraitUri() {
        try {
            BaseProfile profile = JacksonUtil.fromJson(this.userProfile, BaseProfile.class);
            return profile.getPortraitUri();
        } catch (Exception ignore) {
        }
        return null;
    }

}
