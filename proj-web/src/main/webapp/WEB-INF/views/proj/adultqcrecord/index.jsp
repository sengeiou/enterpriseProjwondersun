<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="tag" tagdir="/WEB-INF/tags" %>
<html>
<head>
    <tag:header/>
</head>
<body id="main_layout">
<div id="pageGrid"></div>
<!-- dialog -->
<div id="dd"></div>
<script type="text/template" id="pageGridTmpl">
    <form>
        <div class="col">
            <input id="batchCode" data-qfield="batchCode" data-name="BATCHCODE" class="query" data-qoperator="LIKE"
                   data-qtype="STRING"/></div>
        <div class="col">
            <input id="MaterialName" class="query" data-name="MaterialName" data-qfield="material.fullName"
                   data-qoperator="LIKE" data-qtype="STRING"/>
        </div>
        <div class="col">
            <input id="MaterialSku" class="query" data-name="MaterialSku" data-qfield="material.Sku"
                   data-qoperator="LIKE" data-qtype="STRING"/>
        </div>
        <div class="col">
            <select id="FileUploadStatus" style="width: 120px" data-name="FileUploadStatus"
                    data-qfield="record.FileUploadStatus"
                    class="easyui-combobox query"
                    data-options="editable:false">
                <option value="">所有</option>
                <option value="0">待上传</option>
                <option value="1">上传中</option>
                <option value="2">上传成功</option>
                <option value="3">上传失败</option>
            </select>
        </div>
        <div class="col">
            <select id="FileUploadStatus4one" style="width: 120px" data-name="FileUploadStatus4one"
                    data-qfield="record.FileUploadStatus4one"
                    class="easyui-combobox query"
                    data-options="editable:false">
                <option value="">所有</option>
                <option value="0">待上传</option>
                <option value="1">上传中</option>
                <option value="2">上传成功</option>
                <option value="3">上传失败</option>
            </select>
        </div>
        <%--上传状态，0未上传，1上传中，2已上传，3上传失败--%>
    </form>
</script>

<script type="text/javascript">
    var pagegrid = null;
    (function () {
        //参数配置
        var ownOptions = {
            containerId: 'pageGrid',
            pageGridTmplId: 'pageGridTmpl',
            moduleName: '产品质检报表',
            useCommonApi: true,
            contextUrl: '${pageContext.request.contextPath}',
            module: '${module}',
            submodule: '${submodule}',
            instantSearch: true
        };

        /** 设置自定义按钮事件处理方法 **/
        ownOptions.btnHandlers = {
            "onUpload": function (pg) {
                var row = pg.getSingleSelected();
                if (!row) {
                    return;
                }
                var id = row.id || row.ID || row.Id;
                pg.openWin('upload', "上传" + pg.moduleName, "id=" + id, 'dialog');
            },
            "onDelete": function (pg) {
                var rows = pg.getMultiSelected();
                if (!rows || rows.length === 0) {
                    $.messager.alert('警告', "请选择数据！", "warning");
                    return;
                }
                $.messager.confirm('确认操作', '确认要删除吗？', function (r) {
                    if (r) {
                        var url = "${pageContext.request.contextPath}/api/ebc/projproductqcrecord/delete";
                        deleteRows(rows, pg, url);
                    }
                });
            },
            "onUnqc": function (pg) {
                var _this = pg;
                var rows = pg.getMultiSelected();
                if (!rows || rows.length === 0) {
                    $.messager.alert('警告', "请选择数据！", "warning");
                    return;
                } else {
                    $.messager.confirm('确认操作', '确认撤销吗？', function (r) {
                        if (r) {
                            setUploadStatus(pg, rows);
                        }
                    });
                }
            },
            "onAddAndUploadQCAndCC": function (pg) {   //新增和上传质检数据和主要产品合格证明
                pg.openWin('addanduploadqcandcc', "新增" + pg.moduleName, "");
            },
            "onReuploadQCAndCC": function (pg) {  //重新上传质检数据和主要产品合格证明
                var row = pg.getSingleSelected();
                if (!row) {
                    return;
                }
                var id = row.id || row.ID || row.Id;
                pg.openWin('reuploadqcandcc', "重新上传", "id=" + id);
            },
            "onUploadCC": function (pg) {  //上传主要产品合格证明
                var row = pg.getSingleSelected();
                if (!row) {
                    return;
                }
                var id = row.id || row.ID || row.Id;
                pg.openWin('uploadconformitycertificate', "上传主要产品合格证明", "id=" + id, 'dialog');
            },//软促 重传
            "onReupload": function (pg) {
                var _this = pg;
                var rows = pg.getMultiSelected();
                if (!rows || rows.length === 0) {
                    $.messager.alert('警告', "请选择数据！", "warning");
                    return;
                } else {
                    $.messager.confirm('确认操作', '确认重新上传吗？', function (r) {
                        if (r) {
                            setUploadStatus4Wondersun(pg,rows,0);
                        }
                    });
                }
            },//一所 重传
            "onReupload4one": function (pg) {
                var _this = pg;
                var rows = pg.getMultiSelected();
                if (!rows || rows.length === 0) {
                    $.messager.alert('警告', "请选择数据！", "warning");
                    return;
                } else {
                    $.messager.confirm('确认操作', '确认重新上传吗？', function (r) {
                        if (r) {
                            setUploadStatus4Wondersun(pg,rows,1);
                        }
                    });
                }
            },//重新上传产品标准
            "onReuploadproductstandard": function (pg) {
                var row = pg.getSingleSelected();
                if (!row) {
                    return;
                }
                var id = row.id || row.ID || row.Id;
                // 赋值本行 row
                $('body').data('re_row',row);
                $('body').data('re_id',id);
                // pg.openWin('reuploadproductstandard', "重新上传产品标准" + pg.moduleName, "id=" + id, 'dialog');

                var url = '${pageContext.request.contextPath}/view/ebc/productqcrecord/reuploadproductstandard?id=' + id;
                var winTitle = '重新上传产品标准';
                $('#dd').dialog({
                    title: winTitle,
                    width: 600,
                    height: 400,
                    closed: false,
                    cache: false,
                    href: url,
                    modal: true,
                    resizable: true
                });

            }
        };
        ownOptions.colFormatters = {//下载文件
            "PRODINSPECTIONREPORT": function (value, row, index) {
                return ('<a onclick="dlFile(\'' + row.ID + '\''+',\''+'0\')">查看</a>');
            },
            "CONFORMITYCERTIFICATE": function (value, row, index) {
                return ('<a onclick="dlFile(\'' + row.ID + '\''+',\''+'1\')">查看</a>');
            },
            "PRODUCTSTANDARDPDF": function (value, row, index) {
                return ('<a onclick="dlFile(\'' + row.ID + '\''+',\''+'2\')">查看</a>');
            }
        };
        //初始化
        pagegrid = new arlen.PageGrid(ownOptions).build();
    })();


    function setUploadStatus4Wondersun(obj,rows,type){
        var ss = [];
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            ss.push({name: "ids", value: "" + (row.id || row.ID ) + ""});
        }
        ss.push({name: "type", value: "" + type + ""});
        var _this = obj;
        $.messager.progress({text: ''});
        $.ajax({
            type: 'post',
            url: obj.getApiUrl('reupload'),
            data: ss
        }).done(function (data) {
            if (data.code === 0) {
                $.messager.alert('提示', '操作成功');
                _this.search();
            } else {
                $.messager.alert('操作失败', data.errMsg, 'warning');
            }
        }).always(function () {
            $.messager.progress('close');
        });
    }

    function setUploadStatus(obj, rows) {
        var ss = [];
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            ss.push({name: "ids", value: "" + (row.id || row.ID ) + ""});
        }
        var _this = obj;
        $.messager.progress({text: ''});
        $.ajax({
            type: 'post',
            url: obj.getApiUrl('unqc'),
            data: ss
        }).done(function (data) {
            if (data.code === 0) {
                $.messager.alert('提示', '操作成功');
                _this.search();
            } else {
                $.messager.alert('操作失败', data.errMsg, 'warning');
            }
        }).always(function () {
            $.messager.progress('close');
        });
    }

    function dlFile(Id, type) {
        var url = pagegrid.contextUrl + "/api/ebc/projproductqcrecord/download";
        var $form = $('<form method="post" action="" target="_blank"></form>');
        $form.attr('action', url);
        var $inputPageReqId = $('<input id="id" name="id" type="hidden"/>');
        $inputPageReqId.val(Id);
        $form.append($inputPageReqId);
        var $inputPageReqType = $('<input id="type" name="type" type="hidden"/>');
        $inputPageReqType.val(type);
        $form.append($inputPageReqType);
        $('body').append($form);
        $form.submit();
        $form.remove();
    }

    function deleteRows(rows,pagegrid,url){
        var ids  = [];
        for(var i = 0 ;i<rows.length;i++){
            ids.push({name: "ids", value: "" + rows[i].ID + ""});
        }
        $.messager.confirm('确认操作', '确定要删除吗？', function (r) {
            if (r) {
                $.messager.progress({text: ''});
                $.ajax({
                    type: 'POST',
                    url: url,
                    data: ids
                }).done(function (data) {
                    if (data.code==0) {
                        $.messager.alert('提示', '删除成功', "info");
                        pagegrid.search();
                    } else {
                        $.messager.alert('提示', data.errMsg, "info");
                    }
                }).fail(function (jqXHR) {
                    $.messager.alert('Info', "Request failed: " + jqXHR.status);
                }).always(function () {
                    $.messager.progress('close');
                });
            }
        });
    }
</script>
</body>
</html>