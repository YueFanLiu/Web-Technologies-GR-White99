<template>
  <div class="page-container tech-component-container">
    <div class="search-option">
      <el-form :model="queryParams" ref="queryRef" :inline="true" class="search-form">
        <el-form-item label="平台名称" prop="searchName">
            <el-input
               v-model="queryParams.searchName"
               placeholder="请输入平台名称"
               clearable
               @keyup.enter="getList"
            />
         </el-form-item>
         <el-form-item label="选用组件" prop="selectedComponent">
          <el-select v-model="queryParams.selectedComponent" clearable placeholder="请选择技术组件" >
            <el-option v-for = "items in componentOptions" :key="items.value" :label="items.label" :value="items.value"></el-option>
          </el-select>
         </el-form-item>
         <el-form-item>
          <el-button type="primary"  @click="getList">搜索</el-button>
          <el-button  @click="handleReset">重置</el-button>
         </el-form-item>
      </el-form>
      <el-divider style="margin: 6px 0"></el-divider>

    </div>

    <div class="editValue">
      <el-button type="primary" icon="Plus" @click="handleButtonClick('add')">新建平台</el-button>
      <el-button type="danger" icon="Minus" @click="handleButtonClick('batchDelete')">删除平台</el-button>
    </div>
    <!--表格栏目-->
    <div class="table-container">  
        <el-table :data="tableData" class="my-el-table" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" ></el-table-column>
          <el-table-column prop="name" label="平台名称" width="200"></el-table-column> 
          <el-table-column prop="desc" label="平台描述" width="300" show-overflow-tooltip />
          <el-table-column prop="component" label="选用组件" width="180" >
            <template #default="scope">
              <div class="desc-container">{{getComponentName(scope.row.component)}}</div>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="180" />
          <el-table-column prop="status" label="状态" width="180" ></el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180" />
          <el-table-column fixed="right" label="操作" width="110" >
            <template #default="scope">
              <el-button link type="primary" @click="handleButtonClick('edit', scope.row)">编辑</el-button>
              <el-popconfirm title="删除后数据将无法找回，请确认是否删除?" @confirm="handleButtonClick('delete', scope.row)">
                <template #reference>
                  <el-button link type="primary">删除</el-button>
                </template>
              </el-popconfirm>
              
            </template>
          </el-table-column>
        </el-table>
    </div>
    <!-- footer页码功能 -->
    <div class="footer-container">
      <Pagination
        v-if="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="searchTableData"
      />
    </div>
    <addAndEditDialog v-if="dialogVisible" v-model="dialogVisible" :title="dialogTitle" :activeRow="activeRow" @confirm="getList"></addAndEditDialog>
  </div>
  
</template>

<script setup>
import addAndEditDialog from './components/addAndEditDialog.vue';
import Pagination from '@/components/Pagination';
import {ElMessage, ElMessageBox} from 'element-plus';
import { ref, getCurrentInstance} from 'vue';
import { loadPageData, deleteData,searchData } from '../../api/product-platform';


const { proxy } = getCurrentInstance();
const queryParams = ref({
  searchName: '',
  selectedComponent: '',
  pageNum: 1,
  pageSize: 10,
})

const statusReverseMap = {
  "已发布": "published",
  "未发布": "unpublished"
};
const tableData = ref([]);
const queryRef = ref(null);
const selectedRows = ref([]);

//控制footer页码功能
const total = ref(0)//从接口获取总数
const dialogVisible = ref(false)
const dialogTitle = ref('')
const activeRow = ref({})
const loading = ref(false)


// 检索功能下拉框选取组件
const componentOptions = ref([]);

const getComponentOptions = async () => {
  loading.value = true;
  try {
    const requestParams ={
      typeCode:'product_family|application_component',
    }
    const response = await loadPageData(requestParams);

    const res = response.data;
    const {totalRecordCount = 0, records = []} = res;
    
    componentOptions.value = records.map(item => {
      const attrMap = {};
      (item.attributeValues || []).forEach(attr => {
        attrMap[attr.attributeCode] = attr.value;
      });
      const option ={
        value:item.oid,
        label:attrMap.COMPONENT_NAME || '',

      }
      return option;
    });
  }catch (error) {
    console.error('获取技术组件数据失败：', error);
    ElMessage.error('获取数据失败：' + error.message );
  }finally{
    loading.value = false;
  }
};
onMounted(async () => {
  await getComponentOptions();
  getList();
});
  


const getList = () => {
  searchTableData();
};



// 检索接口函数
const searchTableData = async () => {
  loading.value = true;
  try {
    const { searchName, pageNum, pageSize, selectedComponent } = queryParams.value;
    const requestParams = {
      code:'product_family|product_platform',
      pageNum,
      pageSize,
      attributeValues: [
        { attributeCode: 'PLATFORM_NAME', value: searchName.trim() || ''},
        { attributeCode: 'SELECTED_COMPONENTS', value: selectedComponent || '' }
      ]
    };

    const response = await searchData(requestParams);;
    const res = response.data;
    const {totalRecordCount = 0, records = []} = res;
    total.value = totalRecordCount;

    

    tableData.value = records.map(item => {
      const attrMap = {};
      (item.attributeValues || []).forEach(attr => {
        attrMap[attr.attributeCode] = attr.value;
      });
      const oid = `ROOT:${item.id}`;
      return{
        id: item?.id || '', 
        oid: oid,
        name: attrMap.PLATFORM_NAME || '',
        desc: attrMap.PLATFORM_DESCRIPTION || '',
        updateTime: item?.gmtLastModified || '无',
        version: attrMap.PLATFORM_VERSION || '',
        status: attrMap.PLATFORM_STATUS === 'published' ? '已发布' : '未发布',
        component: attrMap.SELECTED_COMPONENTS || '',
        shortName: attrMap.PLATFORM_SHORTNAME || '',
        advantage: attrMap.PLATFORM_ADVANTAGES || '',
        department: attrMap.RESPONSIBLE_DEPARTMENT || '', 
      };
    });
  } catch (error) {
    console.error('请求发生错误：', error);
    ElMessage.error('获取数据失败,请稍后重试' );
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};




const handleReset = () => {
  if(queryRef.value){
    queryRef.value.resetFields();
  }
  queryParams.value.pageNum = 1;
  queryParams.value.pageSize = 10;
  getList();
};

// 删除函数
const deleteBatchData = async (oids) => {
  try {
    loading.value = true;
   
    const response = await deleteData(oids);
    ElMessage.success('删除成功');
    getList();
    selectedRows.value = [];
  } catch (error) {
    console.error('删除失败：', error);
    ElMessage.error('删除失败,请稍后重试');
  } finally {
    loading.value = false;
  }
};

//按钮点击功能,新建组件，删除组件
const handleButtonClick = (flag, row) => {
  switch (flag) {
    case 'add':
      dialogTitle.value = '新建平台'
      activeRow.value = {}
      dialogVisible.value = true
      break;
    case 'batchDelete':
      if (selectedRows.value.length == 0) {
        proxy.$ElMessage(
          {
            message: '请勾选要删除的数据',
            type: 'warning',
          }
        );
        return
      }
      proxy.$ElMessageBox.confirm('删除后数据将无法找回，请确认是否删除?', '提示')
        .then(() => {
          const oids = selectedRows.value.map(item => item.oid);
          deleteBatchData(oids);
        })
        .catch(() => {})
      break;
      
    case 'delete':
        ElMessageBox.confirm('删除后数据无法找回，请确认是否删除？', '提示')
          .then(() => {
            deleteBatchData([row.oid]);
          })
          .catch(() => {});
      break
    case 'edit':
      dialogTitle.value = '编辑平台'
      activeRow.value = {
        ...row,
        status: statusReverseMap[row.status]
      }
      dialogVisible.value = true
      break;
    
  };
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
};

const getComponentName = (componentStr) => {
  let result = [];
  componentStr?.split(',')
    .map(id => id.trim())
    .filter(id => id)
    .forEach(id => {
      const match = componentOptions.value.find(item => item.value === id);
      result.push(match ? match.label : '');
    });
  return result.join('，') || '';
};


</script>




<style scoped lang="scss">

.desc-container {
  width: 1000px; 
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
}

.search-option{
  padding: 8px;
}

.table-container{
  margin:10px;
  padding:10px;
}



</style>
