<template>
  <div class="register-page">
    <div class="register">
      <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
        <h3 class="title">{{ title }}</h3>

        <el-form-item prop="fullName">
          <el-input 
            v-model="registerForm.fullName" 
            type="text" 
            size="large" 
            placeholder="Full Name"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="email">
          <el-input 
            v-model="registerForm.email" 
            type="text" 
            size="large" 
            auto-complete="off" 
            placeholder="Email"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            size="large" 
            auto-complete="off"
            placeholder="Password"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="role">
          <el-select 
            v-model="registerForm.role" 
            size="large" 
            placeholder="Select Role"
            style="width:100%"
          >
            <el-option label="User" value="user" />
            <el-option label="Admin" value="admin" />
          </el-select>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            size="large" 
            auto-complete="off"
            placeholder="Confirm Password"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item style="width:100%;">
          <el-button
            :loading="loading"
            size="large" 
            type="primary"
            style="width:100%;"
            @click.prevent="handleRegister"
          >
            <span v-if="!loading">Register</span>
            <span v-else>Registering</span>
          </el-button>
          <div style="float: right;">
            <router-link class="link-type" :to="'/login'">Log in with an existing account</router-link>
          </div>
        </el-form-item>
      </el-form>
      <div class="el-register-footer">
        <span>{{ footerContent }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { register } from "@/api/login"
import defaultSettings from '@/settings'

const title = "Register Account"
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()

const registerForm = ref({
  fullName:"",
  email: "",
  password: "",
  role: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("The two passwords did not match"))
  } else {
    callback()
  }
}

const registerRules = {
  fullName: [
    { required: true, trigger: "blur", message: "Please enter your full name" }
  ],
  email: [
    { required: true, trigger: "blur", message: "Please enter your email" },
    { type:'email', message: "User account length must be between 2 and 20", trigger: "blur" }
  ],
  password: [
    { required: true, trigger: "blur", message: "Please enter your password" },
    { min: 5, max: 20, message: "User passwords must be between 5 and 20 characters long", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "Cannot contain illegal characters：< > \" ' \\\ |", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "Please enter your password again" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  role: [
    { required: true, trigger: "change", message: "Please select a role" }
  ]
}

const loading = ref(false)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        ElMessageBox.alert("Registration successful!", "Success", {
          type: "success"
        }).then(() => {
          router.push("/login")
        })
      }).catch(() => {
        loading.value = false
        })
    }
  })
}


</script>

<style lang='scss' scoped>
.register-page {
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

.register-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
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
.register-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.register-code {
  width: 33%;
  height: 40px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-register-footer {
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
.register-code-img {
  height: 40px;
  padding-left: 12px;
}
</style>
