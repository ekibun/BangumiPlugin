var sp = __TEST__ || __pkg__.App.app.getPlugin().getSharedPreferences("plugin", 0);

module.exports = {
    dump(key, data) {
       if(!__TEST__) sp.edit().putString(key, JSON.stringify(data)).apply();
   },
   load(key) {
       if(__TEST__) return;
       data = sp.getString(key, "");
       return data && JSON.parse(data);
   }
}