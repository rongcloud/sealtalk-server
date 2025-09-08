package com.rcloud.server.sealtalk.constant;

import lombok.Getter;

/**
 * 用户相关授权
 */
public class UserAuth {



    @Getter
    public enum FriendAuth {
        /**
         * 加好友不需要认证
         */
        NO_REQUIRE_AUTH(0),
        /**
         * 加好友需要认证
         */
        REQUIRE_AUTH(1);

        private final Integer auth;

        FriendAuth(Integer auth) {
            this.auth = auth;
        }
    }


    @Getter
    public  enum JoinGroupAuth{


        /**
         * 加群需要认证
         */
        REQUIRE_AUTH(0),

        /**
         * 加群不需要认证
         */
        NO_REQUIRE_AUTH(1);

        private final Integer auth;

        JoinGroupAuth(Integer auth) {
            this.auth = auth;
        }
    }

}
