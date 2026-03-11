<template>
  <button @click="handlePay">支付</button>
</template>

<script setup lang="ts">
const handlePay = async () => {
  try {
    const res = await fetch('http://127.0.0.1:9002/alipay/pay?id=3', {
      method: 'GET',
      headers: {
        // 'Authorization': 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImV4cCI6MTc3MzE2MDQyMH0.jThYl-mSCZTnDpGuPC8QIXfb7hqFZjAfEOo5fiZef-g',
      }
    })

    // 获取 HTML 文本
    const html = await res.text()

    // 方式1: 新窗口打开
    const newWindow = window.open('', '_blank')
    if (newWindow) {
      newWindow.document.write(html)
      newWindow.document.close()
    }

    // 方式2: 替换当前页面
    // document.open()
    // document.write(html)
    // document.close()

    // 方式3: 如果返回的是支付跳转 URL，直接跳转
    // window.location.href = html

  } catch (err) {
    console.error('请求失败:', err)
  }
}
</script>