"use strict";

const log = (context, object) => print(context + ": " + String(object));
const newFloats = capacity => java.lang.reflect.Array.newInstance(java.lang.Float.TYPE, capacity);

// These wrappers cover Java functional interfaces that Rhino cannot always infer.
const run = method => new java.lang.Runnable(){run: method};
const boolf = method => new Boolf(){get: method};
const boolp = method => new Boolp(){get: method};
const floatf = method => new Floatf(){get: method};
const floatp = method => new Floatp(){get: method};
const cons = method => new Cons(){get: method};
const prov = method => new Prov(){get: method};
const func = method => new Func(){get: method};

// JavaScript 'extend(Base, ..., {})' is Java's 'new Base(...) {}'.
function extend(/* Base, ..., definition */){
    const Base = arguments[0];
    const definition = arguments[arguments.length - 1];
    const args = [Base, definition].concat(Array.from(arguments).splice(1, arguments.length - 2));
    const instance = JavaAdapter.apply(null, args);
    for(let field in definition){
        if(typeof definition[field] !== "function") instance[field] = definition[field];
    }
    return instance;
}
