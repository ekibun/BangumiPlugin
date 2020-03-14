var url = "http://bullet.video.qq.com/fcgi-bin/target/regist?vid="+video.id;
var doc = Jsoup.parse(http.get(url).body().string());
return doc.selectFirst("targetid").text();