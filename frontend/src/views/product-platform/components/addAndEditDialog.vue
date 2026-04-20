<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="40%"
    append-to-body
    @close="closelDialog"
    :close-on-click-modal="false"
    class="scrollbar"
  >
    <div>
      <el-tabs v-model="activeName" class="el-tabs" @tab-click="handleClick">
        <el-tab-pane label="基本信息" name="first">
          <el-form ref="formRef" :model="formModel" :rules="rules" label-width="90px" label-suffix=":">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formModel.name" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="平台描述" prop="desc">
              <el-input v-model="formModel.desc" type="textarea" class="desc-container" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="平台简称" prop="shortName">
              <el-input v-model="formModel.shortName" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="平台优势" prop="advantage">
              <el-input v-model="formModel.advantage" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="版本" prop="version">
              <el-input v-model="formModel.version" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="负责部门" prop="department">
              <el-input v-model="formModel.department" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="formModel.status" placeholder="请选择" >
                <el-option v-for="items in statusOptions" :key="items.value" :label="items.label" :value="items.value"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="架构信息" name="second">
          <el-form-item label="技术组件" prop="selectedComponent">
            <el-select v-model="formModel.selectedComponent" multiple clearable placeholder="请选择" >
              <el-option v-for="items in componentOptions" :key="items.value" :label="items.label" :value="items.value"></el-option>
            </el-select>
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closelDialog">取消</el-button>
        <el-button type="primary" @click="submitForm" v-loading="loading">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup>
import { ElMessage } from 'element-plus';
import { loadPageData,createPlatformData, updateData} from '@/api/product-platform/index';
import { onMounted } from 'vue';
const { proxy } = getCurrentInstance();
const props = defineProps({
  title: String,
  activeRow: Object
});
const emits = defineEmits(['confirm']);
const dialogVisible = defineModel();
const formRef = ref();
const formModel = ref({
  name: undefined,
  desc: undefined,
  component: undefined,
  version: undefined,
  status: undefined,
  shortName: undefined,
  advantage: undefined,
  department: undefined,
  oid:undefined,
});
const rules = {
  name: [{ required: true, trigger: 'blur', message: '请输入' }],
  version: [{ required: true, trigger: 'blur', message: '请输入' }],
  department: [{ required: true, trigger: 'blur', message: '请输入' }],
 
  status: [{ required: true, trigger: 'blur', message: '请选择' }],
};
const loading = ref(false);
const componentOptions = ref([]);
// 从接口获取组件下拉框的数据
const getComponentOptions = async () => {
  loading.value = true;
  try {
    const requestParams ={
      typeCode:'product_family|application_component',
    }
    const response = await loadPageData(requestParams);

    const res = response.data;
    console.log('=== 业务数据res:',res);
    const {totalRecordCount = 0, records = []} = res;
    
    componentOptions.value = records.map(item => {
      const attrMap = {};
      (item.attributeValues || []).forEach(attr => {
        attrMap[attr.attributeCode] = attr.value;
      });
      return{
        value:item.oid,
        label:attrMap.COMPONENT_NAME || '',
      }
    });
  }catch (error) {
    console.error('获取技术组件数据失败：', error);
    ElMessage.error('获取数据失败：' + error.message );
  }finally{
    loading.value = false;
  }
};


  

const statusOptions = [
  {
    value: 'published',
    label: '已发布'
  },
  {
    value: 'unpublished',
    label: '未发布'
  },
]

const activeName = ref('first');
const handleClick = (tab, event) => {
  console.log(tab, event);
}

// 调用新建和编辑接口
const submitForm =  async () => {
  formRef.value.validate(async(validate) => {
    if (validate) {
      try{
        loading.value = true
        const isEdit = !!formModel.value.oid;
        let response;
        if (isEdit) {
          const editParams = {
            oid:formModel.value.oid,
            name:"产品平台",
            attributeValues: [
              {attributeCode: 'PLATFORM_NAME', value: formModel.value.name || ''},
              {attributeCode: 'PLATFORM_DESCRIPTION', value: formModel.value.desc || ''},
              {attributeCode: 'PLATFORM_SHORTNAME', value: formModel.value.shortName || ''},
              {attributeCode: 'PLATFORM_ADVANTAGES', value: formModel.value.advantage || ''},
              {attributeCode: 'PLATFORM_VERSION', value: formModel.value.version || ''},
              {attributeCode: 'RESPONSIBLE_DEPARTMENT', value: formModel.value.department || ''},
              {attributeCode: 'PLATFORM_STATUS', value: formModel.value.status},
              {attributeCode: 'SELECTED_COMPONENTS', value: formModel.value.selectedComponent 
              ?formModel.value.selectedComponent.join(',')
               :'' },
            ]
          }
          response = await updateData(editParams)
          ElMessage.success('编辑成功');
        } else{
          response = await createPlatformData({
            name:"产品平台",
            code:"product_family|product_platform",
            objAttrMapping: {
              PLATFORM_NAME: formModel.value.name || '',
              PLATFORM_DESCRIPTION: formModel.value.desc || '',
              PLATFORM_SHORTNAME: formModel.value.shortName || '',
              PLATFORM_ADVANTAGES: formModel.value.advantage || '',
              PLATFORM_VERSION: formModel.value.version || '',
              RESPONSIBLE_DEPARTMENT: formModel.value.department || '',
              
              PLATFORM_STATUS: formModel.value.status || 'unpublished',
              SELECTED_COMPONENTS: formModel.value.selectedComponent
              ? formModel.value.selectedComponent.join(',') 
              : '',
            }
          })
        
          ElMessage.success('新增成功');
        }

        closelDialog()
        emits('confirm')
      }catch (error) {
        console.error('接口请求失败：',error);
        ElMessage.error('网络错误，请稍后重试');
      } finally{
        loading.value = false;
      }

    }
  });
};

const closelDialog = () => {
  formRef.value.resetFields();
  dialogVisible.value = false;
};
const initData = () => {
  if (props.activeRow?.oid) {
    const componentOidStr = props.activeRow.component || '';
    const selectedOids = componentOidStr
      .split(',')
      .map(oid => oid.trim()) 
      .filter(oid => oid); 
    formModel.value = {
      ...props.activeRow,
      selectedComponent: selectedOids 
      
    }
    
  }
  
}
onMounted(() => {
  initData()
  getComponentOptions();
})

</script>
<style scoped lang="scss"></style>
