layui.config({
    base: "js/"
}).use(['form', 'layer'], function () {
    var form = layui.form,
        layer = parent.layer === undefined ? layui.layer : parent.layer,
        $ = layui.jquery;
});

function refreshCode() {
    var captcha = document.getElementById("captcha");
    captcha.src = "/login/captcha?t=" + new Date().getTime();
}
