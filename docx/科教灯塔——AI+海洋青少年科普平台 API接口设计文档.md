### 1. 文档说明

1. 项目名称：科教灯塔——AI+海洋青少年科普教育模块化平台

2. 接口风格：RESTful API

3) 开发框架：SpringBoot

4) 数据格式：JSON

5. 基础路径：http://{server-ip}:8080/api

6. 适用环境：Android前端对接

### 2. 统一响应规范

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

* code：200成功；400参数异常；404资源不存在；500服务器异常

* msg：提示信息

* data：返回业务数据

### 3. 课程模块接口

#### 3.1 获取课程列表

* 接口地址：GET /course/list

* 接口描述：获取平台全部课程信息

* 请求参数：无

* 响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "courseName": "海洋小卫士——AI生物识别",
      "intro": "使用AI视觉技术识别海洋生物，学习海洋生物知识",
      "coverUrl": "",
      "courseType": 1,
      "createTime": "2026-04-02 00:00:00"
    },
    {
      "id": 2,
      "courseName": "洋流探险家——交互式模拟",
      "intro": "通过交互式模拟理解洋流形成原理与运动规律",
      "coverUrl": "",
      "courseType": 2,
      "createTime": "2026-04-02 00:00:00"
    },
    {
      "id": 3,
      "courseName": "海洋工程师——智能浮标DIY",
      "intro": "DIY智能浮标硬件，完成水质数据采集与监测",
      "coverUrl": "",
      "courseType": 3,
      "createTime": "2026-04-02 00:00:00"
    }
  ]
}
```



#### 3.2 获取课程详情

* 接口地址：GET /course/detail/{id}

* 接口描述：根据课程ID获取单门课程详细信息

* 请求参数：id（路径参数，课程ID）

* 响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "courseName": "海洋小卫士——AI生物识别",
    "intro": "详细课程介绍",
    "coverUrl": "",
    "courseType": 1,
    "createTime": "2026-04-02 00:00:00"
  }
}
```



### 4. 海洋生物识别模块接口

#### 4.1 获取海洋生物列表

* 接口地址：GET /biology/list

* 接口描述：获取AI可识别的全部海洋生物信息

* 请求参数：无

* 响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "bioName": "小丑鱼",
      "enName": "Clownfish",
      "intro": "热带珊瑚礁鱼类，与海葵共生",
      "imgUrl": ""
    },
    {
      "id": 2,
      "bioName": "海龟",
      "enName": "Sea Turtle",
      "intro": "古老海洋爬行动物，具有长距离洄游特性",
      "imgUrl": ""
    }
  ]
}
```



#### 4.2 根据生物名称查询详情

* 接口地址：GET /biology/name/{bioName}

* 接口描述：AI识别出生物名称后，查询详细科普信息

* 请求参数：bioName（路径参数）

* 响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "bioName": "小丑鱼",
    "enName": "Clownfish",
    "intro": "小丑鱼生活在热带浅海海域，与海葵形成共生关系",
    "imgUrl": ""
  }
}
```

### 5. AI模型信息接口

#### 5.1 获取端侧AI模型信息

* 接口地址：GET /ai/info

* 接口描述：获取端侧TFLite模型版本、类别信息

* 请求参数：无

* 响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "modelName": "marine_bio_v2.tflite",
    "version": "2.0",
    "classCount": 20,
    "updateTime": "2026-04-01"
  }
}
```



### 6. 用户模块接口

#### 6.1 用户简易登录

* 接口地址：POST /user/login

* 接口描述：演示版简易登录，无需密码

* 请求参数：

```json
{
  "username": "student01",
  "role": "student"
}
```

* 响应示例：

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "userId": 1,
    "username": "student01",
    "nickname": "学生01",
    "role": "student"
  }
}
```



#### 6.2 获取用户信息

* 接口地址：GET /user/info/{userId}

* 接口描述：根据用户ID获取基本信息

* 请求参数：userId（路径参数）

* 响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "userId": 1,
    "username": "student01",
    "nickname": "学生01",
    "role": "student"
  }
}
```



### 7. 文件上传接口

#### 7.1 图片上传

* 接口地址：POST /file/upload

* 接口描述：上传图片文件

* 请求参数：file（multipart/form-data）

* 响应示例：

```json
{
  "code": 200,
  "msg": "上传成功",
  "data": {
    "url": "/upload/xxx.jpg"
  }
}
```



### 9. 接口清单总表

| 模块   | 接口地址                 | 请求方式 | 功能说明     |
| ---- | -------------------- | ---- | -------- |
| 课程   | /course/list         | GET  | 获取课程列表   |
| 课程   | /course/detail/{id}  | GET  | 获取课程详情   |
| 生物识别 | /biology/list        | GET  | 获取生物列表   |
| 生物识别 | /biology/name/{name} | GET  | 按名称查生物详情 |
| AI模型 | /ai/info             | GET  | 获取端侧模型信息 |
| 用户   | /user/login          | POST | 用户简易登录   |
| 用户   | /user/info/{userId}  | GET  | 获取用户信息   |
| 文件   | /file/upload         | POST | 图片上传     |



### 12. 对接说明

1. 后端部署在本地笔记本，使用局域网IP访问

2. AI推理在Android端本地完成，后端不参与计算

3) 全部接口支持跨域访问，便于前端调试

4) 暂时无鉴权机制
