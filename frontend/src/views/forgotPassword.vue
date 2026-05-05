<template>
  <div class="forgotPassword-page">
    <h1 class="page-title">Accessible Events Platform</h1>

    <div class="forgotPassword">
      <el-form
          ref="forgotPasswordRef"
          :model="forgotPasswordForm"
          :rules="forgotPasswordRules"
          class="forgotPassword-form"
      >
        <h3 class="title">{{ title }}</h3>

        <el-form-item prop="email">
          <el-input
              v-model="forgotPasswordForm.email"
              type="text"
              size="large"
              auto-complete="off"
              placeholder="Enter your email"
              @keyup.enter="handleforgotPassword"
          >
            <template #prefix>
              <svg-icon icon-class="user" class="el-input__icon input-icon" />
            </template>
          </el-input>

          <!-- 验证码按钮，当前版本暂时不需要 -->
          <!-- <el-button>Send verification code</el-button> -->
        </el-form-item>

        <p class="forgotPassword-tip">
          We'll send a password reset link to your email.
        </p>

        <el-form-item style="width:100%;">
          <el-button
              :loading="loading"
              size="large"
              type="primary"
              style="width:100%;"
              @click.prevent="handleforgotPassword"
          >
            <span v-if="!loading">Send Reset Link</span>
            <span v-else>Sending...</span>
          </el-button>
        </el-form-item>

        <div class="back-login">
          <span>Already have an account?</span>
          <router-link class="link-type" :to="'/login'"> Sign In</router-link>
        </div>
      </el-form>

<!--      forgotPassword-footer 和 el-forgotPassword-footer 是两个不同类-->
      <div class="el-forgotPassword-footer">
              <span>{{ footerContent }}</span>
            </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from "element-plus"
import { forgotPassword } from "@/api/login"
import defaultSettings from "@/settings"

const title = "Forgot Password"
const footerContent = defaultSettings.footerContent
const { proxy } = getCurrentInstance()

const forgotPasswordForm = ref({
  email: ""
})

const forgotPasswordRules = {
  email: [
    { required: true, trigger: "blur", message: "Please enter your email" },
    { type: "email", trigger: "blur", message: "Please enter a valid email address" }
  ]
}

const loading = ref(false)

// 接 API —— forgotPassword
function handleforgotPassword() {
  proxy.$refs.forgotPasswordRef.validate(valid => {
    if (valid) {
      loading.value = true

      forgotPassword(forgotPasswordForm.value).then(() => {
        ElMessage.success("Password reset email has been sent. Please check your inbox.")
      }).finally(() => {
        loading.value = false
      })
    }
  })
}
</script>


<style lang='scss' scoped>
.forgotPassword-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login_background.png");
  background-size: cover;
  background-position: center;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.forgotPassword-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.forgotPassword-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.forgotPassword-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-forgotPassword-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.forgotPassword-code-img {
  height: 40px;
  padding-left: 12px;
}
.page-title{
  text-align: center;
  font-size: 28px;
  color: #333;
  margin-bottom: 30px;
  font-weight: 500;
}
.forgotPassword-footer{
  margin-top: 24px;
  text-align: center;
  font-size: 16px;
  line-height: 1.8; 
  color: #333;
}
.back-login {
  margin-top: 10px;
  text-align: center;
  font-size: 14px;
  color: #333;
}
</style>
