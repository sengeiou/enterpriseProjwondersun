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

        #batchCode {
            width: 150px;
        }
    </style>
</head>
<body id="main_layout">
<div class="winContainer">
    <form id="upfileForm" action="${pageContext.request.contextPath}/api/ebc/projproductqcrecord/uploadnorecord"
          method="POST" enctype="multipart/form-data">
        <div class="btnBar">
            <a href="javascript:void(0)" class="easyui-linkbutton" plain="true" iconCls="icon-save"
               onclick="submitUpfileForm()">保存</a>
        </div>
        <table class="table1">
            <tr>
                <th style="width:15%">产品名称：</th>
                <td><span class="needStarAfter"></span>
                    <input id="prodId" class="widget jothuheim-bringback" data-bringback-group="prodId" name="prodId"
                           type="text"
                           data-options="pageUrl: '${pageContext.request.contextPath}/view/ebd/material/bringback?ext13=1',
                           dataUrl: '${pageContext.request.contextPath}/api/ebd/material',
                           idField: 'ID',
                           textField: 'SHORTNAME',required:true"/></td>
                <th style="width:15%">批号：</th>
                <td style="width:35%"><span class="needStarAfter"></span>
                    <input id="batchCode" name="batchCode" type="text" class="widget easyui-validatebox"
                           validtype="digits" value=""/>
                    <a href="javascript:void(0)" id="chckHrefId" class="l-btn l-btn-small l-btn-plain" plain="true"
                       iconCls="" onclick="check()" group="" id="">
                        <span class="l-btn-left l-btn-icon-left">
                            <span class="l-btn-text">检测</span>
                            <span class="l-btn-icon">&nbsp;</span>
                        </span>
                    </a>
                </td>
            </tr>
            <tr>
                <th style="width:15%">主要原料来源地：</th>
                <td style="width:35%"><span class="needStarAfter"></span>
                    <select id="materialSource" style="width: 350px" name="materialSource"
                            class="easyui-combobox  widget"
                            data-options="textField:'text',valueField:'value',editable:false">
                    </select>
                </td>
                <th style="width:15%">产品标准：</th>
                <td style="width:35%"><span class="needStarAfter"></span>
                    <input id="productStandard" name="productStandard" type="text" class="widget easyui-validatebox"
                           validtype="digits" value=""/>
                </td>
            </tr>
            <tr>
                <th style="width:15%" nowrap="nowrap">产品检验报告(pdf)：</th>
                <td><span class="needStarAfter"></span>
                    <input name="inspectFile" type="file" class="easyui-validatebox"/></td>
                <th style="width:15%" nowrap="nowrap">主要原料合格证明(pdf)：</th>
                <td><span class="needStarAfter"></span>
                    <input name="certificateFile" type="file" class="easyui-validatebox"/></td>
            </tr>
            <tr>
                <th style="width:15%" nowrap="nowrap">产品标准(pdf)：</th>
                <td><span class="needStarAfter"></span>
                    <input name="productStandardPdf" type="file" class="easyui-validatebox"/></td>
                <th style="width:15%"></th>
                <td style="width:35%"></td>
            </tr>
        </table>
        <input type="hidden" name="from" value="web"/>
    </form>
    <span id="resultMsg" style="display: none;color:red;"></span>
    <div id="errMsg" style="display: none;height: 30px;width: 100%;overflow-y: scroll;color:red;"></div>
</div>
</body>
</html>
<script type="text/javascript">
    //保存
    function submitUpfileForm() {
        $('#resultMsg').empty();
        $('#errMsg').empty();
        $('#resultMsg').hide();
        $('#errMsg').hide();
        var pv = $('#prodId').val();
        if (pv === '') {
            $.messager.alert('提示', '请选择产品名称！', '提示', function (c) {
                $("input[name='prodId']")[0].focus();
            });
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
                var errFlag = false;
                if (data.code === 0) {
                    $.messager.alert('提示', '处理成功！', '提示');
                    $(window.parent.closeTab('close'));
                } else {
                    errFlag = true;
                    $('#resultMsg').html('处理失败！');
                    $('#errMsg').html(data.errMsg);
                }
                if (errFlag) {
                    $('#resultMsg').show();
                    $('#errMsg').show();
                }
            }
        });
    }
</script>
<script type="text/javascript">
    var pagegrid;
    (function () {
        arlen.jothuheim.BringBack.parse($('.winContainer'));
        var widgets = $('.table1').find('.widget');
        _.each(widgets, function (widget) {
            $(widget).prop('required', true);
        });
        $.parser.parse($('.winContainer'));
    })();

    function check() {
        var pv = $('#prodId').val();
        var bv = $('#batchCode').val();
        if (pv === '') {
            $.messager.alert('提示', '请选择产品名称！', '提示', function (c) {
                $("input[name='prodId']")[0].focus();
            });
            return;
        }
        if (bv === '') {
            $.messager.alert('提示', '请输入批号！', '提示', function (c) {
                $("input[name='batchCode']")[0].focus();
            });
            return;
        }
        var getResult = function () {
            return $.ajax({
                url: '${pageContext.request.contextPath}/api/ebc/projproductqcrecord/check',
                type: "post",
                data: {
                    prodId: pv,
                    batchCode: bv
                }
            });
        };
        getResult().done(function (data) {
            data = eval(data);
            var $ck = $('span.l-btn-icon', '#chckHrefId');
            if (data.code === 0) {
                $('#chckHrefId').attr('iconCls', 'icon-tick');
                $('#materialSource').combobox({
                    url: '${pageContext.request.contextPath}/api/ebc/projproductqcrecord/getComboBox',
                    valueField: 'value',
                    textField: 'text',
                    onLoadSuccess: function () {
                        $('#materialSource').combobox('select', data.data);
                    }
                });
                $ck.removeClass('icon-cross');
                $ck.addClass('icon-tick');
            } else {
                $('#chckHrefId').attr('iconCls', 'icon-cross');
                $ck.removeClass('icon-tick');
                $ck.addClass('icon-cross');
                $.messager.alert('提示', '输入批号【' + $("input[name='batchCode']").val() + '】不存在！', '提示', function (c) {
                });
            }
            $.parser.parse($('.winContainer'));
        }).always(function () {

        });
    }
</script>