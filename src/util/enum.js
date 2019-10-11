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
  CertiStatus,
  GroupReceiverStatus,
  GroupReceiverType,
  OptPermissionRole,
  RegistrationStatus,
  RelationshipStatus,
  GroupAddStatus
};
