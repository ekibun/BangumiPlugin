return webview.load(video.url, {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
},"", function(request){
    if(request.getUrl().toString().contains(".m3u8")) return webview.toRequest(request);
    else return null;
})