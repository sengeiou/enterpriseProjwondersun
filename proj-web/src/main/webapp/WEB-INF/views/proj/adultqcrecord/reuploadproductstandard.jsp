<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<div class="winContainer">
    <form id="upfileForm" action="${pageContext.request.contextPath}/api/ebc/projproductqcrecord/reuploadproductstandard" method="POST"
          enctype="multipart/form-data">
        <div class="btnBar">
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" iconcls="icon-save"
               onclick="submitForm()">保存</a>
        </div>
        <table class="table1">
            <tr>
                <th>产品标准：<span class="needStar">*</span></th>
                <td>
                    <input id="productStandard" name="productStandard" type="text" class="widget easyui-validatebox" validtype="digits" value=""/>
                </td>
            </tr>
            <tr>
                <th>产品标准(pdf)：<span class="needStar">*</span></th>
                <td>
                    <input name="productStandardPdf" type="file" class="easyui-validatebox"/>
                </td>
            </tr>
            <tr style="display: none;">
                <th></th>
                <td>
                    <input id="id" name="id" type="text" value="${id}" />
                </td>
            </tr>
        </table>
    </form>
</div>


<script type="text/javascript">
    // var id;
    // jQuery onReady START
    jQuery(function(){
        // 获取导入行的参数
        // id = $('body').data('re_id');
        // $('#id').val(id);
        // jQuery onReady END
    });

    function submitForm() {

    	var $upfileForm = $('#upfileForm');
    	
    	$upfileForm.form({  
    	    url: $upfileForm.attr("action"),
    	    onSubmit: function(){  
    	    	$.messager.progress({text: ''});
    	    },
            success: function (data) {

            	$.messager.progress('close');
            	var obj = eval('(' + data + ')');
            	if (0 == obj.code) {
                    $.messager.alert('提示', '重新上传产品标准 成功', '提示', function (c) {
                        $('#dd').dialog('close');
                        var pageNumber = $(".pagination-num").val();
                        var pageSize = $(".pagination-page-list").val();
                        pagegrid.search(pageNumber, pageSize);
                    });
            	} else {
                    $.messager.alert('提示', obj.errMsg, '提示');
            	}

            }
    	});
    	
    	$upfileForm.submit();
    }
</script>
