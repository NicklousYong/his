package edu.neu.hoso.service.impl;

import edu.neu.hoso.model.*;
import edu.neu.hoso.service.DoctorTreatmentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @title: DoctorTreatmentServiceImpl
 * @package edu.neu.hoso.service.impl
 * @description: 实现对处置的操作：展示处置模板、添加处置单等
 * @author: 29-y
 * @date: 2019-06-17 20:47
 * @version: V1.0
*/
@Service
public class DoctorTreatmentServiceImpl implements DoctorTreatmentService {
    @Resource
    TreatmentMapper treatmentMapper;
    @Resource
    TreatmentItemsMapper treatmentItemsMapper;
    @Resource
    ExpenseItemsMapper expenseItemsMapper;
    @Resource
    FmedicalItemsMapper fmedicalItemsMapper;
    @Resource
    GroupTreatmentMapper groupTreatmentMapper;
    @Resource
    GroupTreatmentItemsMapper groupTreatmentItemsMapper;

    /**
     * @title: selectTreatmentById
     * @description: 根据处置单ID得到处置单的详细内容
     * @author: 29-y
     * @date: 2019-06-17 20:49
     * @param: [treatmentId]
     * @return: edu.neu.hoso.model.Treatment
     * @throws:
     */
    @Override
    public Treatment selectTreatmentById(Integer treatmentId) {
        return treatmentMapper.selectTreatmentById(treatmentId);
    }

    /**
     * @title: insertTreatment
     * @description: 开立处置单
     * @author: 29-y
     * @date: 2019-06-17 20:50
     * @param: [treatment]
     * @return: java.lang.Integer
     * @throws:
     */
    @Override
    public Integer insertTreatment(Treatment treatment) {
        List<TreatmentItems> treatmentItemsList= treatment.getTreatmentItemsList();
        //逐个插入treatmentItems中 并生成对应的收费条目
        treatmentMapper.insert(treatment);
        Integer medicalRecordId = treatment.getMedicalRecordId();//得到病历ID
        Integer treatmentId = treatment.getTreatmentId();//得到处置单ID
        for(TreatmentItems treatmentItems : treatmentItemsList){//遍历处置单条目列表
            //得到非药品对象
            FmedicalItems fmedicalItems = fmedicalItemsMapper.selectByPrimaryKey(treatmentItems.getFmedicalItemsId());

            //插入expenseItems
            ExpenseItems expenseItems = new ExpenseItems();//新建一个收费条目
            expenseItems.setMedicalRecordId(medicalRecordId);
            expenseItems.setExpenseTypeId(fmedicalItems.getExpenseTypeId());//得到收费类型
            expenseItems.setPayStatus("1");//默认为支付状态为未收费
            expenseItems.setTotalCost(treatmentItems.getQuantity()*fmedicalItems.getFmedicalItemsPrice());//计算总额
            expenseItemsMapper.insert(expenseItems);//插入到expenseItems表中

            //插入treatmentItems
            treatmentItems.setTreatmentId(treatmentId);//设置处置单ID
            treatmentItems.setValidStatus("1");//设置有效状态
            treatmentItems.setActualQuantity(treatmentItems.getQuantity());//设置实际数量为开立数量
            treatmentItems.setExpenseItemsId(expenseItems.getExpenseItemsId());//加入expenseItems的ID
            treatmentItemsMapper.insert(treatmentItems);//插入到treatmentItems中
        }
        return treatmentId;
    }


    /**
     * @title: listGroupTreatment
     * @description: 展示处置组套列表 （scope 1个人、2科室、3全院）
     * @author: 29-y
     * @date: 2019-06-17 20:51
     * @param: [userId, scope]
     * @return: java.util.List<edu.neu.hoso.model.GroupTreatment>
     * @throws:
     */
    @Override
    public List<GroupTreatment> listGroupTreatment(Integer userId, String scope) {
        if(scope.equals("1")){
            return groupTreatmentMapper.listGroupTreatmentFromUser(userId);
        }else if(scope.equals("2")){
            return groupTreatmentMapper.listGroupTreatmentFromDepartment(userId);
        }else if(scope.equals("3")){
            return groupTreatmentMapper.listGroupTreatmentFromHospital();
        }else{
            return null;
        }
    }

    /**
     * @title: selectGroupTreatmentById
     * @description: 根据GroupTreatmentId得到GroupTreatment的信息
     * @author:
     * @date: 2019-06-17 20:52
     * @param: [groupTreatmentId]
     * @return: edu.neu.hoso.model.GroupTreatment
     * @throws:
     */
    @Override
    public GroupTreatment selectGroupTreatmentById(Integer groupTreatmentId) {
        return groupTreatmentMapper.selectGroupTreatmentById(groupTreatmentId);
    }

    /**
     * @title: insertGroupTreatment
     * @description: 插入处置组套
     * @author: 29-y
     * @date: 2019-06-17 23:58
     * @param: [groupTreatment]
     * @return: java.lang.Integer
     * @throws:
     */
    @Override
    public Integer insertGroupTreatment(GroupTreatment groupTreatment) {
        groupTreatmentMapper.insert(groupTreatment);
        Integer groupTreatmentId = groupTreatment.getGroupTreatmentId();//获取处置模板的ID
        //插入到处置条目列表中
        List<GroupTreatmentItems> groupTreatmentItemsList = groupTreatment.getGroupTreatmentItemsList();
        for(GroupTreatmentItems groupTreatmentItems : groupTreatmentItemsList){
            groupTreatmentItems.setGroupTreatmentId(groupTreatmentId);//设置处置模板的ID
            groupTreatmentItemsMapper.insert(groupTreatmentItems);
        }
        return groupTreatmentId;
    }

    /**
     * @title: cancelTreatmentItemsById
     * @description: 作废处置条目
     * @author: 29-y
     * @date: 2019-06-17 20:57
     * @param: [treatmentItemsId]
     * @return: void
     * @throws:
     */
    @Override
    public void cancelTreatmentItemsById(Integer treatmentItemsId) {
        TreatmentItems treatmentItems = treatmentItemsMapper.selectByPrimaryKey(treatmentItemsId);
        Integer expenseItemsId = treatmentItems.getExpenseItemsId();
        ExpenseItems expenseItems = expenseItemsMapper.selectByPrimaryKey(expenseItemsId);
        String payStatus = expenseItems.getPayStatus();//获取支付状态
        if(payStatus.equals("1")) {//如果是未缴费状态
            treatmentItems.setValidStatus("2");//将有效设置为2 即无效
            expenseItems.setPayStatus("4");//收费状态设置为无效
            expenseItemsMapper.updateByPrimaryKey(expenseItems);//更新该状态
        }
    }

    /**
     * @title: listTreatmentByMedicalRecordId
     * @description: 列出病历ID对应的处置单列表
     * @author: 29-y
     * @date: 2019-06-17 20:58
     * @param: [medicalRecordId]
     * @return: java.util.List<edu.neu.hoso.model.Treatment>
     * @throws:
     */
    @Override
    public List<Treatment> listTreatmentByMedicalRecordId(Integer medicalRecordId) {
        return treatmentMapper.listTreatmentByMedicalRecordId(medicalRecordId);
    }

}
