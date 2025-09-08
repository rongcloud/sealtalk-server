package com.rcloud.server.sealtalk.dto;

import lombok.Data;

@Data
public class ScreenCaptureParam {

    private Integer conversationType; //会话类型：1 单聊、3 群聊
    private String targetId;        //接收者 Id
    private Integer noticeStatus;   //设置状态： 0 关闭 1 打开


}
