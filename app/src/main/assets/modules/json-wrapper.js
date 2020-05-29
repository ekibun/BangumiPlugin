function handleCircular() {
   var cache = [];
   var keyCache = [];
   return (key, value) => {
       if(value instanceof Packages.java.lang.Object) {
           return JSON.parse(Packages.soko.ekibun.bangumi.plugins.util.JsonUtil.INSTANCE.toJson(value));
       }
       if (typeof value === 'object' && value !== null) {
           var index = cache.indexOf(value);
           if (index !== -1) return '[Circular ' + keyCache[index] + ']';
           cache.push(value);
           keyCache.push(key || 'root');
       }
       return value;
   }
}

var NativeJsonStringify = JSON.stringify;
JSON.stringify = function(value, replacer, space) {  
   replacer = replacer || handleCircular();
   return NativeJsonStringify(value, replacer, space);
}