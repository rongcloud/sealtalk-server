package com.rcloud.server.sealtalk.constant;


public enum BotTypeEnum {


  PUBLIC(0),
  PRIVATE(1),
  ;
  private final int type;

  BotTypeEnum(int type) {
    this.type = type;
  }

  public int getType() {
    return type;
  }

  /**
   * 根据type查询枚举值
   * @param type 机器人类型
   * @return 对应的枚举值，如果找不到则返回null
   */
  public static BotTypeEnum getByType(Integer type) {
    if (type == null) {
      return null;
    }
    for (BotTypeEnum botType : values()) {
      if (botType.getType() == type) {
        return botType;
      }
    }
    return null;
  }
}
