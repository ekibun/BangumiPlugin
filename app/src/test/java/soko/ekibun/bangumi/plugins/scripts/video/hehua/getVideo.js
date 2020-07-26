var req = webview(video.url, {}, "", function(request){
    if(request.getUrl().toString().contains(".m3u8")) return http.makeRequest(request);
    else return null;
});
if(req.url.includes("?url="))
    req.url = req.url.substring(req.url.indexOf("?url=") + 5)
return req