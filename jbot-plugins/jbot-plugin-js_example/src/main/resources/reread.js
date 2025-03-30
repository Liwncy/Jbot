// noinspection JSUnresolvedReference

import {Listener} from "listener.js";

(() => {
    const Plugin = Java.extend(PluginClass, {
        getEventListener: () => {
            return new Listener(this);
        },
        getCommandExecutor:() => null
    })
    return new Plugin();
})()