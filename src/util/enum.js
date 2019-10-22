var GroupRole = {
  CREATOR: 0,
  MEMBER: 1,
  MANAGER: 2
};

var OptPermissionRole = [
  GroupRole.CREATOR, GroupRole.MANAGER
];

var CertiStatus = {
  CLOSED: 1,
  OPENED: 0
};

var Error = {
  PERMISSIONS: {
    code: 20001,
    msg: 'No permission'
  },
  LIMIT: {
    code: 20002,
    msg: 'Limit error'
  },
  NOT_FOUND: {
    code: 20003,
    msg: 'Not found'
  }
};

var CopyGroupError = {
  PROTECTED: {
    code: 20004,
    msg: 'Protected'
  },
  COPIED: {
    code: 20005,
    msg: 'Copied'
  },
  NOT_EXIST: {
    code: 20006,
    msg: 'No Group'
  },
  LIMIT: {
    code: 20007,
    msg: 'Member Limit'
  }
}

var GroupReceiverStatus = {
  IGNORE: 0, // 忽略
  AGREED: 1, // 同意
  WAIT: 2, // 等待
  EXPIRED: 3
};

var GroupReceiverType = {
  MANAGER: 2,
  MEMBER: 1
};

var GroupExitedStatus = {
  CREATOR: 0, //群主踢出
  MANAGER: 1, //管理员踢出
  SLEF: 2 //主动退出
}

var RegistrationStatus = {
  REGISTERED: 1,
  UN_REGISTERED: 0
};

var RelationshipStatus = {
  IS_FRIEND: 1,
  NON_FRIEND: 0
};

var GroupAddStatus = {
  ADDED: 1,
  WAIT_MANAGER: 2,
  WAIT_MEMBER: 3
};

module.exports = {
  GroupRole,
  Error,
  CopyGroupError,
  CertiStatus,
  GroupReceiverStatus,
  GroupReceiverType,
  OptPermissionRole,
  RegistrationStatus,
  RelationshipStatus,
  GroupAddStatus,
  GroupExitedStatus
};
