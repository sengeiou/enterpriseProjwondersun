<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<div class="winContainer">
    <form id="upfileForm" action="${pageContext.request.contextPath}/api/${modulePath}/upload" method="POST"
          enctype="multipart/form-data">
        <div class="btnBar">
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" iconcls="icon-save"
               onclick="submitUpfileForm()">导入</a>
        </div>
        <table class="table1">
            <tr>
                <th style="width:15%" nowrap="nowrap">主要产品合格证明(PDF OR JPG )：</th>
                <td><span class="needStarAfter"></span>
                    <input name="file" type="file" class="easyui-validatebox"/></td>
            </tr>
        </table>
        <input type="hidden" name="id" value="${id}"/>
        <input type="hidden" name="from" value="web"/>
        <input type="hidden" name="type" value="1"/>
    </form>
</div>


<script type="text/javascript">
    function submitUpfileForm() {
        if ($("input[name='file']")[0].value === '') {
            $.messager.alert('提示', '请选择主要产品合格证明！', '提示', function (c) {
                $("input[name='file']")[0].focus();
            });
            return;
        }
        $.messager.progress();
        $('#upfileForm').form('submit', {
            success: function (data) {
                $.messager.progress('close');
                data = $.parseJSON(data);
                console.log(data);
                if (data.code === 0) {
                    $.messager.alert('提示', '处理成功！', '提示', function (c) {
                    });
                } else {
                    $.messager.alert('提示', data.errMsg, '提示', function (c) {
                    });
                }
            }
        });
    }
</script>
