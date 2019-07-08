// Notice that values templated like this ${xxx} will be replaced

// First we add a button, that browser.js will replace with a QR Code
var button = document.createElement('button');
button.setAttribute('data-yoti-application-id', '${appId}');
button.setAttribute('data-yoti-type', 'inline');
button.innerText='Login with Yoti';
var div = document.createElement('div');
div.setAttribute('class', 'text-center');
div.appendChild(button);
var loginButton = document.getElementById('loginButton_0');
loginButton.parentNode.replaceChild(div, loginButton);

// Now we load and execute the browser.js script from yoti.com
var yotiScript = document.createElement('script');
yotiScript.type='text/javascript';
yotiScript.src='${yotiScriptSource}';
yotiScript.addEventListener('load', function() {
    var initScript = document.createElement('script');
    initScript.type='text/javascript';
    initScript.src=_ybg.init();
    document.body.appendChild(initScript);

    // And we add a script to support reloading, in case the link times out
    _ybg.getPreloadedQrUrlCalled = false
    _ybg.getPreloadedQrUrl = function() {
        if(!_ybg.getPreloadedQrUrlCalled){
            _ybg.getPreloadedQrUrlCalled = true;
            return '${url}';
        }
        window.location.reload();
    }
});
document.head.appendChild(yotiScript);
