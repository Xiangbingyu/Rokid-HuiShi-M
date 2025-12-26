#!/bin/bash

# API 测试脚本
OUTPUT="test_result.md"

echo "# API 测试结果" >$OUTPUT
echo "" >>$OUTPUT
echo '**时间**: '$(date "+%Y-%m-%d %H:%M:%S") >>$OUTPUT
echo "" >>$OUTPUT

API="http://localhost:8000"

# 1. 登录
echo '## 1. 登录' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X POST "$API/auth/login" \
	-H "Content-Type: application/json" \
	-d '{"user_id":"24320313","password":"13567756447aA.","device_id":"AR001"}' | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT
echo "" >>$OUTPUT

# 提取 token
TOKEN=$(curl -s -X POST "$API/auth/login" \
	-H "Content-Type: application/json" \
	-d '{"user_id":"24320313","password":"13567756447aA.","device_id":"AR001"}' | jq -r '.access_token')

# 2. 设备注册
echo '## 2. 设备注册 (需admin)' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X POST "$API/system/device-register" \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d '{"device_id":"TEST003","device_name":"测试设备003","device_type":"AR-Glasses","owner":"测试负责人"}' | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT
echo "" >>$OUTPUT

# 3. 设备上线
echo '## 3. 设备上线' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X POST "$API/system/device-online" \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d '{"device_id":"TEST003","wearer_user_id":"24320313","location":"测试位置"}' | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT
echo "" >>$OUTPUT

# 4. 查询在线设备 (需admin)
echo '## 4. 查询在线设备 (需admin)' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X POST "$API/system/online-devices" \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d '{"page":1,"limit":10}' | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT
echo "" >>$OUTPUT

# 5. 设备下线
echo '## 5. 设备下线' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X POST "$API/system/device-offline" \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d '{"device_id":"TEST003"}' | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT
echo "" >>$OUTPUT

# 6. 健康检查
echo '## 6. 健康检查' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X GET "$API/system/health" \
	-H "Authorization: Bearer $TOKEN" | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT
echo "" >>$OUTPUT

# 7. 无 token 测试
echo '## 7. 无 token 访问 (应失败)' >>$OUTPUT
echo '```' >>$OUTPUT
curl -s -X GET "$API/system/health" | tee -a $OUTPUT
echo -e '\n```' >>$OUTPUT

echo "✓ 测试完成，结果已保存到 $OUTPUT"
