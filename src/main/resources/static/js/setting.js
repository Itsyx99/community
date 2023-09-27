$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    // 发送AJAX请求之前,将CSRF令牌设置到请求的消息头中.
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function(e, xhr, options){
        xhr.setRequestHeader(header, token);
    });

    var formData = new FormData($("#uploadForm")[0]);
    $.ajax({
        url : CONTEXT_PATH + "/user/upload/url",
        method : "post",
        contentType : false,
        processData : false,
        data : formData,
        success : function(data){
            alert(data)
            data = $.parseJSON(data);
            if(data.code == 0) {
                window.location.reload();
            } else {
                alert(data.msg);
            }
        }
    });
    return false;
}