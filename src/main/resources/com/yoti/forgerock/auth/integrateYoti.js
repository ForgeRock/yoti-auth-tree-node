// Yoti Login Button
var container = document.createElement('div');
container.setAttribute('id', 'yoti-share-button');
container.setAttribute('class', 'center-block');
container.setAttribute('style', 'width: 130px; height: 50px;');

// Parent container for Yoti Login Button
var parent = document.createElement('div');
parent.setAttribute('class', 'text-center');
parent.appendChild(container);

// Replace default login button with Yoti Login Button
var loginButton = document.getElementById('loginButton_0');
loginButton.parentNode.replaceChild(parent, loginButton);

var shareType = '${yotiShareType}'

var yotiScript = document.createElement('script');
yotiScript.setAttribute('id', 'yoti-modal-script');
yotiScript.type='text/javascript';
yotiScript.src='${yotiScriptSource}';
yotiScript.addEventListener('load', function() {
    if (shareType === 'DYNAMIC') {
        window.Yoti.Share.init({
            "elements": [{
                "domId": "yoti-share-button",
                "shareUrl": "${shareUrl}",
                "clientSdkId": "${clientSdkId}",
                "button": {
                    "label": "Use Yoti"
                }
            }]
        });
    } else {
        window.Yoti.Share.init({
            "elements": [{
                "domId": "yoti-share-button",
                "scenarioId": "${scenarioId}",
                "clientSdkId": "${clientSdkId}",
                "button": {
                    "label": "Use Yoti"
                }
            }]
        });
    }
});
document.head.appendChild(yotiScript);
