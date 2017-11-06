### 群组相关接口

| 接口地址 | 说明 |
|---------|-----|
| [/group/create](#post-groupcreate) | 创建群组 |
| [/group/add](#post-groupadd) | 添加群成员 |
| [/group/join](#post-groupjoin) | 加入群组 |
| [/group/kick](#post-groupkick) | 踢人 |
| [/group/quit](#post-groupquit) | 退出群组 |
| [/group/dismiss](#post-groupdismiss) | 解散群组 |
| [/group/transfer](#post-grouptransfer) | 转让群主角色 |
| [/group/rename](#post-grouprename) | 群组重命名 |
| [/group/set_bulletin](#post-groupset_bulletin) | 发布群公告 |
| [/group/set_portrait_uri](#post-groupset_portrait_uri) | 设置群头像 |
| [/group/set_display_name](#post-groupset_display_name) | 设置群名片 |
| [/group/:id](#get-groupid) | 获取群信息 |
| [/group/:id/members](#get-groupidmembers) | 获取群成员 |


## API 说明

### POST /group/create

创建群组

#### 请求参数

```
{
     "name": "RongCloud",
     "memberIds": ["AUj8X32w1", "ODbpJIgrL"]
}
```

* name: 群名称
* memberIds: 群成员 Id 列表, 包含 `创建者 Id`

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code":200,
	"result": {
		"id": "RfqHbcjes"
	}
}
```

* id: 群组 Id

返回码说明：

* 200: 请求成功
* 400: 错误的请求
* 1000: 群组个数超限

### POST /group/add

添加群成员

#### 请求参数

```
{
	"groupId": "KC6kot3ID",
	"memberIds": ["52dzNbLBZ"]
}
```

* groupId: 群组 Id
* memberIds: userId 列表

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 错误的请求

### POST /group/join

用户加入群组

#### 请求参数

```
{
	groupId: "KC6kot3ID"
}
```

* 

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 错误的请求

### POST /group/kick

群主或群管理将群成员移出群组

#### 请求参数

```
{
	"groupId": "KC6kot3ID",
	"memberIds": ["52dzNbLBZ"]
}
```

* groupId: 群组 Id
* memberIds: userId 列表

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 错误的请求
* 404: 未知群组
* 403: 当前用户不创建者
* 500: 服务器内部错误，无法同步数据至 RongCloud IM Server

### POST /group/quit

用户退出群组

#### 请求参数

```
{
	groupId: "KC6kot3ID"
}
```

* groupId: 群组 Id

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 错误的请求
* 404: 未知群组
* 403: 当前用户不在群组中
* 500: 服务器内部错误，无法同步数据至 RongCloud IM Server

### POST /group/dismiss

解散群组

#### 请求参数

```
{
	groupId: "KC6kot3ID"
}
```

* groupId: 群组 Id

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 当前用户不是创建者
* 500: 服务器内部错误，无法同步数据至 RongCloud IM Server

### POST /group/transfer

转让群主

#### 请求参数

```
{
	groupId: "KC6kot3ID",
	userId: "52dzNbLBZ"
}
```

* groupId: 群组 Id
* userId: 用户 Id

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 当前用户不是创建者
* 403: 不能把群主转让给自己

### POST /group/rename

群组重命名

#### 请求参数

```
{
	groupId: "KC6kot3ID",
	name: "RongCloud"
}
```

* groupId: 群组 Id
* name: 群名称, 长度不超过 32 个字符

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 群名长度超限

### POST /group/set_bulletin

发布群公告

#### 请求参数

```
{
	groupId: "KC6kot3ID",
	bulletin: "@All 明天 4 点下班"
}
```

* groupId: 群组 Id
* bulletin: 群公告内容, 长度不超过 1024 个字符

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 非法请求，未知群组或当前不用不是群组

### POST /group/set_portrait_uri

设置群头像

#### 请求参数

```
{
	groupId: "KC6kot3ID",
	portraitUri: "http://7xogjk.com1.z0.glb.clouddn.com/u0LUuhzHm1466557920584458984"
}
```

* groupId: 群组 Id
* portraitUri: 群头像地址, 长度不能超过 256 个字符

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 400: 非法请求

### POST /group/set_display_name

设置自己的群名片

#### 请求参数

```
{
	groupId: "KC6kot3ID",
	displayName: "Martin"
}
```

* groupId: 群组 Id
* displayName: 群名片

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200
}
```

返回码说明：

* 200: 请求成功
* 404: 未知群组

### GET /group/:id

获取群信息

#### 请求参数

```
{
	groupId: "KC6kot3ID"
}
```

* groupId: 群组 Id

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
	"code": 200,
	"result": {
		"id": "KC6kot3ID",
		"name": "RongCloud",
		"portraitUri": "",
		"memberCount": 13,
		"maxMemberCount": 500,
		"creatorId": "I8cpNlo7t",
		"type": 1,
		"bulletin": null,
		"deletedAt": null
	}
}
```

* id: 群组 Id
* name: 群名称
* portraitUri: 群头像
* memberCount: 群人数
* maxMemberCount: 群人数上限
* creatorId: 群主 Id
* type: 类型 1 普通群 2 企业群
* bulletin: 群公告
* deletedAt: 删除日期

返回码说明：

* 200: 请求成功
* 404: 未知群组

### GET /group/:id/members

获取群成员列表

#### 请求参数

```
{
	groupId: "KC6kot3ID"
}
```

* groupId: 群组 Id

#### 返回结果

正常返回，返回的 HTTP Status Code 为 200，返回的内容如下：

```
{
"code": 200,
"result": [
	{
		"displayName": "",
		"role": 1,
		"createdAt": "2016-11-22T03:06:13.000Z",
		"updatedAt": "2016-11-22T03:06:13.000Z",
		"user": {
		"id": "xNlpDTUmw",
		"nickname": "zl01",
		"portraitUri": ""
		}
	},{
		"displayName": "",
		"role": 1,
		"createdAt": "2016-11-22T03:14:09.000Z",
		"updatedAt": "2016-11-22T03:14:09.000Z",
		"user": {
		"id": "h6nEgcPC7",
		"nickname": "zl02",
		"portraitUri": ""
	}]
}
```

* displayName: 群名片
* role: 群角色
* createdAt: 创建时间
* updatedAt: 更改时间
* id: userId
* nickname: 用户名称
* portraitUri: 用户头像

返回码说明：

* 200: 请求成功
* 404: 未知群组
