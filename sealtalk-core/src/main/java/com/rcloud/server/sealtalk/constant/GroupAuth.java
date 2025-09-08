package com.rcloud.server.sealtalk.constant;

import lombok.Getter;

public class GroupAuth {


    @Getter
    public enum JoinAuth{
        /**
         * 加群需要认证
         */
        REQUIRE_AUTH(0),

        /**
         * 加群不需要认证
         */
        NO_REQUIRE_AUTH(1);

        private final Integer auth;

        JoinAuth(Integer auth) {
            this.auth = auth;
        }
    }
}
