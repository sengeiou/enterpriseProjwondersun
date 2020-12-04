<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="tag" tagdir="/WEB-INF/tags" %>
<html>
<head>
    <title></title>
    <tag:header/>
    <style>
        .needStarAfter:after {
            content: "*";
            color: #f00;
            font-weight: bold;
        }
        #batchCode{
            width:150px;
        }
    </style>
</head>
<body id="main_layout">
<div class="winContainer">
    <form id="upfileForm" action="${pageContext.request.contextPath}/api/ebc/productqcrecord/reuploadqcandcc" method="POST" enctype="multipart/form-data">
        <div class="btnBar">
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" iconCls="icon-save"  onclick="submitUpfileForm()">保存</a>
        </div>
        <table class="table1">
            <tr>
                <th style="width:15%">产品名称：</th>
                <td>
                    <input id="prodName"  name="prodName" readonly  style="border: none"  type="text"/></td>
                    <input id="prodId"  name="prodId"  type="text" style="display: none"/></td>
                <th style="width:15%">批号：</th>
                <td style="width:35%">
                    <input  id="batchCode" name="batchCode" readonly style="border: none" type="text" value=""/>
                </td>
            </tr>
            <tr>
                <th style="width:15%" nowrap="nowrap">产品检验报告(pdf、jpg、jpeg)：</th>
                <td><span class="needStarAfter"></span>
                    <input name="inspectFile" type="file" class="easyui-validatebox" /></td>
                <th style="width:15%" nowrap="nowrap">主要原料合格证明(pdf、jpg、jpeg)：</th>
                <td><span class="needStarAfter"></span>
                    <input name="certificateFile" type="file" class="easyui-validatebox" /></td>
            </tr>
        </table>
        <input id="id" name="id" type="hidden" value="${id}"/>
        <input type="hidden" name="from" value="web" />
    </form>
    <span id="resultMsg" style="display: none;color:red;"></span>
    <div id="errMsg" style="display: none;height: 30px;width: 100%;overflow-y: scroll;color:red;"></div>
</div>
    </body>
</html>
<script type="text/javascript">
    $(function(){
        $.ajax({
            url : "${pageContext.request.contextPath}/api/ebc/productqcrecord?id=${id}",
            type : "get",
        }).success(function(result){
            if(result.code == 0){
                var data = result.data;
                $("#prodId").val(data.id);
                $("#prodName").val(data.materialName);
                $("#batchCode").val(data.batchCode);
            }
        });
    })

    //保存
    function submitUpfileForm() {
        $('#resultMsg').empty();
        $('#errMsg').empty();
        $('#resultMsg').hide();
        $('#errMsg').hide();
        var pv=$('#prodId').val();
        if (pv === '') {
            $.messager.alert('提示', '产品为空！', '提示');
            return;
        }
        var $form = $('#upfileForm');
        if (!$form.form('validate')) {
            return;
        }
        if ($("input[name='inspectFile']")[0].value === '') {
            $.messager.alert('提示', '请选择产品检验报告！', '提示', function (c) {
                $("input[name='inspectFile']")[0].focus();
            });
            return;
        }
        if ($("input[name='certificateFile']")[0].value === '') {
            $.messager.alert('提示', '请选择主要原料合格证明！', '提示', function (c) {
                $("input[name='certificateFile']")[0].focus();
            });
            return;
        }
        $.messager.progress();
        $('#upfileForm').form('submit', {
            success: function (data) {
                $.messager.progress('close');
                data = $.parseJSON(data);
                var errFlag=false;
                if (data.code === 0) {
                    $.messager.alert('提示', '处理成功！', '提示');
                    $(window.parent.closeTab('close'));
                }else{
                    errFlag=true;
                    $('#resultMsg').html('处理失败！');
                    $('#errMsg').html(data.errMsg);
                }
                if(errFlag){
                    $('#resultMsg').show();
                    $('#errMsg').show();
                }
            }
        });
    }
</script>