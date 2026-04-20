import request from '@/utils/request'

// 新增数据
export function createPlatformData(formData){
    return request({
        url: '/cap-plm/productfamily/import',
        method: 'post',
        data: formData,
    })
}
// 获取加载页面数据
export function loadPageData(params = {}){
    return request({
        url: '/cap-plm/productfamily/list',
        method: 'get',
        params: params
    })
}
// 编辑更改数据
export function updateData(formData = {}){
    return request({
        url: '/cap-plm/productfamily/updateData',
        method: 'post',
        data: formData,
    })
}

// 删除数据
export function deleteData(oids){
    return request({
        url: '/cap-plm/productfamily/deleteBatch',
        method: 'delete',
        data: oids
    })
}

// 检索数据
export function searchData(data){
    return request({
        url: '/cap-plm/productfamily/searchData',
        method: 'post',
        data
    })
}
