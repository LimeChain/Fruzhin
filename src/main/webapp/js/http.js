function httpRequestSync(method, url, body) {
    var xhr = new XMLHttpRequest();
    xhr.open(method, url, false); // false for synchronous request
    xhr.setRequestHeader('Content-Type', 'application/json');
    if (method === 'POST' && body) {
        xhr.send(JSON.stringify(body));
    } else {
        xhr.send();
    }
    if (xhr.status === 200) {
        return xhr.responseText;
    } else {
        throw new Error('Request failed with status ' + xhr.status);
    }
}