var cookie = App.load("wenku8_cookie")
if(!cookie) {
    var rsp = http.post("https://www.wenku8.net/login.php?do=submit", {}, {
        username: "",
        password: "",
        usecookie: "315360000",
        action: "login",
        submit: " 登 录 "
    })
    print(cookie)
    cookie = rsp.headers("set-cookie").toArray().map((v) => v.split(';')[0]).join(";")
    App.dump("wenku8_cookie", cookie)
}
header = {
    cookie: cookie
}