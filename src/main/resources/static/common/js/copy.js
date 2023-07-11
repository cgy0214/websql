/**
 * 返回当前元素的文本内容
 * @parm {DOM} element 当前DOM元素
 */
function selectText(element){
    return element.innerText;
}
/**
 * @param {String} text 需要复制的内容
 * @return {Boolean} 复制成功:true或者复制失败:false  执行完函数后，按ctrl + v试试
 */
function copyText(text){
    var textareaC = document.createElement('textarea');
    textareaC.setAttribute('readonly', 'readonly'); //设置只读属性防止手机上弹出软键盘
    textareaC.value = text;
    document.body.appendChild(textareaC); //将textarea添加为body子元素
    textareaC.select();
    var res = document.execCommand('copy');
    document.body.removeChild(textareaC);//移除DOM元素
    return res;
}