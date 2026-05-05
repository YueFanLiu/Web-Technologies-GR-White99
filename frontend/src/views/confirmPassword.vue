<template>
  <div class="confirmPassword-page">
    <h1 class="page-title">Accessible Events Platform</h1>

    <div class="confirmPassword">
      <el-form
          ref="confirmPasswordRef"
          :model="confirmPasswordForm"
          :rules="confirmPasswordRules"
          class="confirmPassword-form"
      >
        <h3 class="title">{{ title }}</h3>

        <el-form-item prop="password">
          <el-input
              v-model="confirmPasswordForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="Enter your new password"
              show-password
          >
            <template #prefix>
              <svg-icon icon-class="password" class="el-input__icon input-icon" />
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
              v-model="confirmPasswordForm.confirmPassword"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="Confirm your new password"
              show-password
              @keyup.enter="handleConfirmPassword"
          >
            <template #prefix>
              <svg-icon icon-class="password" class="el-input__icon input-icon" />
            </template>
          </el-input>
        </el-form-item>

        <p class="confirmPassword-tip">
          Please enter your new password to complete the reset.
        </p>

        <el-form-item style="width:100%;">
          <el-button
              :loading="loading"
              size="large"
              type="primary"
              style="width:100%;"
              @click.prevent="handleConfirmPassword"
          >
            <span v-if="!loading">Reset Password</span>
            <span v-else>Resetting...</span>
          </el-button>
        </el-form-item>

        <div class="back-login">
          <span>Remember your password?</span>
          <router-link class="link-type" :to="'/login'"> Sign In</router-link>
        </div>
      </el-form>

      <div class="el-confirmPassword-footer">
        <span>{{ footerContent }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from "element-plus"
import { confirmPassword } from "@/api/login"
import defaultSettings from "@/settings"

const title = "Reset Password"
const footerContent = defaultSettings.footerContent

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loading = ref(false)

const confirmPasswordForm = ref({
  password: "",
  confirmPassword: "",
  token: route.query.token || ""
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error("Please confirm your password"))
  } else if (value !== confirmPasswordForm.value.password) {
    callback(new Error("The two passwords do not match"))
  } else {
    callback()
  }
}

const confirmPasswordRules = {
  password: [
    { required: true, trigger: "blur", message: "Please enter your new password" },
    { min: 6, max: 20, trigger: "blur", message: "Password length should be 6 to 20 characters" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "Please confirm your password" },
    { validator: validateConfirmPassword, trigger: "blur" }
  ]
}

//接API
function handleConfirmPassword() {
  proxy.$refs.confirmPasswordRef.validate(valid => {
    if (valid) {
      loading.value = true

      confirmPassword(confirmPasswordForm.value).then(() => {
        ElMessage.success("Password has been reset successfully.")
        router.push("/login")
      }).finally(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style lang="scss" scoped>
.confirmPassword-page {
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

.confirmPassword-form {
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

.confirmPassword-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.el-confirmPassword-footer {
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

.page-title {
  text-align: center;
  font-size: 28px;
  color: #333;
  margin-bottom: 30px;
  font-weight: 500;
}

.confirmPassword-footer {
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