var ep_sort = episode.sort + Number(line.extra||0)
var format = /\{\{(.*)\}\}/g.exec(line.id) || ["{{ep}}", "ep"]
if(format[0] == "{{ep}}") format[1] = "#.##"
var url = line.id.replace(format[0], java.text.DecimalFormat(format[1]).format(ep_sort))
return {
    site: "webpage",
    id: line.id,
    url: url
}